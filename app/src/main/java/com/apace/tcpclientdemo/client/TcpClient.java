package com.apace.tcpclientdemo.client;

import android.util.Log;

import com.apace.tcpclientdemo.bean.TargetInfo;
import com.apace.tcpclientdemo.bean.TcpMsg;
import com.apace.tcpclientdemo.config.TcpConnConfig;
import com.apace.tcpclientdemo.listener.TcpClientListener;
import com.apace.tcpclientdemo.manager.TcpClientManager;
import com.apace.tcpclientdemo.state.ClientState;
import com.apace.tcpclientdemo.utils.CharsetUtil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Administrator on 2017/8/2.
 */

public class TcpClient {

    private static final String TAG = "onResponse";
    protected TargetInfo mTargetInfo;//目标ip和端口号
    protected Socket mSocket;
    protected ClientState mClientState;
    protected TcpConnConfig mTcpConnConfig;
    protected ConnectionThread mConnectionThread;
    protected SendThread mSendThread;
    protected ReceiveThread mReceiveThread;
    protected List<TcpClientListener> mTcpClientListeners;
    private LinkedBlockingQueue<TcpMsg> msgQueue;

    private TcpClient() {
        super();
    }

    /**
     * 创建tcp连接，需要提供服务器信息
     *
     * @param targetInfo
     * @return
     */
    public static TcpClient getTcpClient(TargetInfo targetInfo) {
        return getTcpClient(targetInfo, null);
    }

    public static TcpClient getTcpClient(TargetInfo targetInfo, TcpConnConfig tcpConnConfig) {
        TcpClient mTcpClient = TcpClientManager.getTcpClient(targetInfo);
        if (mTcpClient == null) {
            mTcpClient = new TcpClient();
            mTcpClient.init(targetInfo, tcpConnConfig);
            TcpClientManager.putTcpClient(mTcpClient);
        }
        return mTcpClient;
    }

    /**
     * 根据socket创建client端，目前仅用在socketServer接受client之后
     *
     * @param socket
     * @return
     */
    public static TcpClient getTcpClient(Socket socket, TargetInfo targetInfo) {
        return getTcpClient(socket, targetInfo, null);
    }


    public static TcpClient getTcpClient(Socket socket, TargetInfo targetInfo, TcpConnConfig connConfig) {
        if (!socket.isConnected()) {
            Log.d("onResponse", "socket is closeed");
        }
        TcpClient mTcpClient = new TcpClient();
        mTcpClient.init(targetInfo, connConfig);
        mTcpClient.mSocket = socket;
        mTcpClient.mClientState = ClientState.Connected;
        mTcpClient.onConnectSuccess();
        return mTcpClient;
    }

    private void init(TargetInfo targetInfo, TcpConnConfig connConfig) {
        this.mTargetInfo = targetInfo;
        mClientState = ClientState.Disconnected;
        mTcpClientListeners = new ArrayList<>();
        if (mTcpConnConfig == null && connConfig == null) {
            mTcpConnConfig = new TcpConnConfig.Builder().create();
        } else if (connConfig != null) {
            mTcpConnConfig = connConfig;
        }
    }

    public synchronized TcpMsg sendMsg(String message) {
        TcpMsg msg = new TcpMsg(message, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    public synchronized TcpMsg sendMsg(byte[] message) {
        TcpMsg msg = new TcpMsg(message, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    public synchronized TcpMsg sendMsg(File file) {
        TcpMsg msg = new TcpMsg(file, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    public synchronized TcpMsg sendMsg(TcpMsg msg) {
        if (isDisconnected()) {
            Log.d(TAG, "发送消息 " + msg + "，当前没有tcp连接，先进行连接");
            connect();
        }
        boolean re = enqueueTcpMsg(msg);
        if (re) {
            return msg;
        }
        return null;
    }

    public synchronized void connect() {
        if (!isDisconnected()) {
            Log.d(TAG, "已经连接了或正在连接");
            return;
        }
        Log.d(TAG, "tcp connecting");
        setClientState(ClientState.Connecting);//正在连接
        getConnectionThread().start();
    }

    public synchronized Socket getSocket() {
        if (mSocket == null || isDisconnected() || !mSocket.isConnected()) {
            mSocket = new Socket();
            try {
                mSocket.setSoTimeout((int) mTcpConnConfig.getReceiveTimeout());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return mSocket;
    }

    public synchronized void disconnect() {
        disconnect("手动关闭tcpclient", null);
    }


    protected synchronized void onErrorDisConnect(String msg, Exception e) {
        if (isDisconnected()) {
            return;
        }
        disconnect(msg, e);
        if (mTcpConnConfig.isReconnect()) {//重连
            connect();
        }
    }

    protected synchronized void disconnect(String msg, Exception e) {
        if (isDisconnected()) {
            return;
        }
        closeSocket();
        getConnectionThread().interrupt();
        getSendThread().interrupt();
        getReceiveThread().interrupt();
        setClientState(ClientState.Disconnected);
        notifyDisconnected(msg, e);
    }

    private synchronized boolean closeSocket() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //连接已经连接，接下来的流程，创建发送和接受消息的线程
    private void onConnectSuccess() {

        setClientState(ClientState.Connected);//标记为已连接
        getSendThread().start();
        getReceiveThread().start();
    }

    /**
     * tcp连接线程
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            try {
                int localPort = mTcpConnConfig.getLocalPort();
                if (localPort > 0) {
                    if (!getSocket().isBound()) {
                        getSocket().bind(new InetSocketAddress(localPort));
                    }
                }
                getSocket().connect(new InetSocketAddress(mTargetInfo.getIp(), mTargetInfo.getPort()),
                        (int) mTcpConnConfig.getConnTimeout());
                Log.d(TAG, "创建连接成功,target=" + mTargetInfo + ",localport=" + localPort);
            } catch (Exception e) {
                Log.d(TAG, "创建连接失败,target=" + mTargetInfo + "," + e);
                onErrorDisConnect("创建连接失败", e);
                return;
            }
            notifyConnected();
            onConnectSuccess();
        }
    }

    public boolean enqueueTcpMsg(final TcpMsg tcpMsg) {
        if (tcpMsg == null || getMsgQueue().contains(tcpMsg)) {
            return false;
        }
        try {
            getMsgQueue().put(tcpMsg);
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected LinkedBlockingQueue<TcpMsg> getMsgQueue() {
        if (msgQueue == null) {
            msgQueue = new LinkedBlockingQueue<>();
        }
        return msgQueue;
    }


    private class SendThread extends Thread {
        private TcpMsg sendingTcpMsg;
        private FileInputStream fis;
        private DataOutputStream dos;

        protected SendThread setSendingTcpMsg(TcpMsg sendingTcpMsg) {
            this.sendingTcpMsg = sendingTcpMsg;
            return this;
        }

        public TcpMsg getSendingTcpMsg() {
            return this.sendingTcpMsg;
        }


        @Override
        public void run() {
            TcpMsg msg;
            try {
                while (isConnected() && !Thread.interrupted() && (msg = getMsgQueue().take()) != null) {
                    setSendingTcpMsg(msg);//设置正在发送的
                    if (msg.getFile() == null) {//发送文本信息
                        byte[] data = msg.getSourceDataBytes();
                        if (data == null) {//根据编码转换消息
                            data = CharsetUtil.stringToData(msg.getSourceDataString(), mTcpConnConfig.getCharsetName());
                        }
                        if (data != null && data.length > 0) {
                            try {
                                getSocket().getOutputStream().write(data);
                                getSocket().getOutputStream().flush();
                                msg.setTime();
                                notifySended(msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                                onErrorDisConnect("发送消息失败", e);
                                return;
                            }
                        }
                    } else {//发送文件
                        try {
                            Log.d("onResponse", "文件名称：" + msg.getFile().getName());
                            Log.d("onResponse", "文件长度：" + msg.getFile().length());
                            fis = new FileInputStream(msg.getFile());
                            dos = new DataOutputStream(getSocket().getOutputStream());
                            //文件名和长度
                            dos.writeUTF(msg.getFile().getName());
                            dos.flush();
                            dos.writeLong(msg.getFile().length());
                            dos.flush();
                            //传输文件
                            byte[] sendBytes = new byte[1024];
                            int length = 0;
                            while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                                dos.write(sendBytes, 0, length);
                                dos.flush();
                            }
                            msg.setSourceDataString(msg.getFile().getName());
                            notifySended(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (fis != null) {
                                    fis.close();
                                }
                                if (dos != null) {
                                    dos.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            try {
                InputStream is = getSocket().getInputStream();
                while (isConnected() && !Thread.interrupted()) {
                    byte[] bytes = new byte[1024];
                    byte[] result = new byte[1024];
                    int len;
                    if ((len = is.read(bytes)) != -1) {
                        result = Arrays.copyOf(bytes, len);
                    }
                    TcpMsg tcpMsg = new TcpMsg(result, mTargetInfo, TcpMsg.MsgType.Receive);
                    tcpMsg.setTime();
                    String msgstr = CharsetUtil.dataToString(result, mTcpConnConfig.getCharsetName());
                    tcpMsg.setSourceDataString(msgstr);
                    notifyReceive(tcpMsg);//notify listener
                }
            } catch (Exception e) {
                Log.d(TAG, "tcp Receive  error  " + e);
                onErrorDisConnect("接受消息错误", e);
            }
        }
    }

    protected ReceiveThread getReceiveThread() {
        if (mReceiveThread == null || !mReceiveThread.isAlive()) {
            mReceiveThread = new ReceiveThread();
        }
        return mReceiveThread;
    }

    protected SendThread getSendThread() {
        if (mSendThread == null || !mSendThread.isAlive()) {
            mSendThread = new SendThread();
        }
        return mSendThread;
    }

    protected ConnectionThread getConnectionThread() {
        if (mConnectionThread == null || !mConnectionThread.isAlive() || mConnectionThread.isInterrupted()) {
            mConnectionThread = new ConnectionThread();
        }
        return mConnectionThread;
    }

    public ClientState getClientState() {
        return mClientState;
    }

    protected void setClientState(ClientState state) {
        if (mClientState != state) {
            mClientState = state;
        }
    }

    public boolean isDisconnected() {
        return getClientState() == ClientState.Disconnected;
    }

    public boolean isConnected() {
        return getClientState() == ClientState.Connected;
    }

    private void notifyConnected() {
        for (TcpClientListener l : mTcpClientListeners) {
            l.onConnected(TcpClient.this);

        }


    }

    private void notifyDisconnected(final String msg, final Exception e) {
        for (TcpClientListener l : mTcpClientListeners) {
            l.onDisconnected(TcpClient.this, msg, e);

        }

    }


    private void notifyReceive(final TcpMsg tcpMsg) {
        for (TcpClientListener l : mTcpClientListeners) {
            tcpMsg.setSourceDataString("服务器：" + tcpMsg.getSourceDataString());
            l.onReceive(TcpClient.this, tcpMsg);
        }
    }


    private void notifySended(final TcpMsg tcpMsg) {
        for (TcpClientListener l : mTcpClientListeners) {
            tcpMsg.setSourceDataString("我：" + tcpMsg.getSourceDataString());
            l.onSended(TcpClient.this, tcpMsg);
        }

    }

    public TargetInfo getTargetInfo() {
        return mTargetInfo;
    }

    public void addTcpClientListener(TcpClientListener listener) {
        if (mTcpClientListeners.contains(listener)) {
            return;
        }
        mTcpClientListeners.add(listener);
    }

    public void removeTcpClientListener(TcpClientListener listener) {
        mTcpClientListeners.remove(listener);
    }

    public void config(TcpConnConfig tcpConnConfig) {
        mTcpConnConfig = tcpConnConfig;
    }

    @Override
    public String toString() {
        return "XTcpClient{" +
                "mTargetInfo=" + mTargetInfo + ",state=" + mClientState + ",isconnect=" + isConnected() +
                '}';
    }
}
