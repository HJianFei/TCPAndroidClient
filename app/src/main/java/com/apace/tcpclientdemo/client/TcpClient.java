package com.apace.tcpclientdemo.client;

import android.util.Log;

import com.apace.tcpclientdemo.bean.TargetInfo;
import com.apace.tcpclientdemo.bean.TcpMsg;
import com.apace.tcpclientdemo.config.TcpConnConfig;
import com.apace.tcpclientdemo.listener.TcpClientListener;
import com.apace.tcpclientdemo.manager.TcpClientManager;
import com.apace.tcpclientdemo.state.ClientState;
import com.apace.tcpclientdemo.utils.ByteUtil;
import com.apace.tcpclientdemo.utils.CharsetUtil;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Administrator at 2017/8/3
 * Description: TCP客户端
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
     * 创建TCP客户端
     *
     * @param targetInfo
     * @return
     */
    public static TcpClient getTcpClient(TargetInfo targetInfo) {
        return getTcpClient(targetInfo, null);
    }

    /**
     * 获取TCP客户端
     *
     * @param targetInfo
     * @param tcpConnConfig
     * @return
     */
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
     * 创建TCP客户端
     *
     * @param socket
     * @param targetInfo
     * @param connConfig
     * @return
     */
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

    /**
     * TCP客户端初始化
     *
     * @param targetInfo
     * @param connConfig
     */
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

    /**
     * 发送数据
     *
     * @param message（字符串）
     * @return
     */
    public synchronized TcpMsg sendMsg(String message) {
        TcpMsg msg = new TcpMsg(message, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    /**
     * 发送数据（字节）
     *
     * @param message
     * @return
     */
    public synchronized TcpMsg sendMsg(byte[] message) {
        TcpMsg msg = new TcpMsg(message, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    /**
     * 发送数据（文件）
     *
     * @param file
     * @return
     */
    public synchronized TcpMsg sendMsg(File file) {
        TcpMsg msg = new TcpMsg(file, mTargetInfo, TcpMsg.MsgType.Send);
        return sendMsg(msg);
    }

    /**
     * 发送数据
     *
     * @param msg
     * @return
     */
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

    /**
     * TCP连接服务器
     */
    public synchronized void connect() {
        if (!isDisconnected()) {
            Log.d(TAG, "已经连接了或正在连接");
            return;
        }
        Log.d(TAG, "tcp connecting");
        setClientState(ClientState.Connecting);//正在连接
        getConnectionThread().start();
    }

    /**
     * 获取一个Socket实例
     *
     * @return
     */
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

    /**
     * 断开连接
     */
    public synchronized void disconnect() {
        disconnect("手动关闭tcpclient", null);
    }


    /**
     * 系统出错，断开连接
     *
     * @param msg
     * @param e
     */
    protected synchronized void onErrorDisConnect(String msg, Exception e) {
        if (isDisconnected()) {
            return;
        }
        disconnect(msg, e);
        if (mTcpConnConfig.isReconnect()) {//重连
            connect();
        }
    }

    /**
     * 断开连接
     *
     * @param msg
     * @param e
     */
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
        Log.d(TAG, "tcp closed");
    }

    /**
     * 关闭Socket
     *
     * @return
     */
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

    /**
     * 连接已经连接，接下来的流程，创建发送和接受消息的线程
     */
    private void onConnectSuccess() {

        setClientState(ClientState.Connected);//标记为已连接
        getSendThread().start();//发送线程
//        getReceiveThread().start();//接收线程
    }

    /**
     * TCP连接线程
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

    /**
     * 把需要发送的消息put进消息发送队列
     *
     * @param tcpMsg
     * @return
     */
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


    /**
     * 消息发送线程
     */
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
                                byte[] buf = new byte[100];
                                InputStream is = getSocket().getInputStream();
                                OutputStream os = getSocket().getOutputStream();
                                byte info_type = 0;
                                byte[] user_id = ByteUtil.getBytes(123);
                                String file_name_ = "";
                                byte[] file_name = ByteUtil.getBytes(file_name_);
                                file_name = addLen(file_name, 20);
                                byte[] requestary = new byte[user_id.length + file_name.length + 1 + data.length];
                                requestary[0] = info_type;
                                System.arraycopy(user_id, 0, requestary, 1, user_id.length);
                                System.arraycopy(file_name, 0, requestary, 1 + user_id.length, file_name.length);
                                System.arraycopy(data, 0, requestary, 1 + user_id.length + file_name.length, data.length);
                                os.write(requestary);
                                msg.setTime();
                                notifySended(msg);
                                int len = -1;
                                String result = "";
                                while ((len = is.read(buf)) != -1) {
                                    result = new String(buf, 0, len);
                                    if (result != null && result.length() > 0) {
                                        break;
                                    }
                                }
                                msg.setTime();
                                msg.setSourceDataString(result);
                                notifyReceive(msg);
                                is.close();
                                os.close();
                                getSocket().close();

                            } catch (IOException e) {
                                e.printStackTrace();
                                onErrorDisConnect("发送消息失败", e);
                                return;
                            }
                        }
                    } else {   //发送文件

                        InputStream is = getSocket().getInputStream();
                        OutputStream os = getSocket().getOutputStream();
                        // 用来接受传输过来的字符
                        byte[] buf = new byte[100];
                        byte info_type = 1;
                        byte[] user_id = ByteUtil.getBytes(123);
                        String file_name_ = msg.getFile().getName();
                        byte[] file_name = ByteUtil.getBytes(file_name_);
                        file_name = addLen(file_name, 20);
                        byte[] requestary = new byte[user_id.length + file_name.length + 1];
                        requestary[0] = info_type;
                        System.arraycopy(user_id, 0, requestary, 1, user_id.length);
                        System.arraycopy(file_name, 0, requestary, 1 + user_id.length, file_name.length);
                        os.write(requestary);
                        int len = -1;
                        String stutus = "";
                        while ((len = is.read(buf)) != -1) {
                            stutus = new String(buf, 0, len);
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if ("ok".equals(stutus)) {
                                break;
                            }
                        }
                        if ("ok".equals(stutus)) {
                            FileInputStream fIn = new FileInputStream(msg.getFile());
                            int data = -1;
                            while (-1 != (data = fIn.read())) {
                                os.write(data);
                            }
                            msg.setSourceDataString(msg.getFile().getName());
                            notifySended(msg);
//                            while ((len = is.read(buf)) != -1) {
//                                stutus = new String(buf, 0, len);
//                                try {
//                                    Thread.sleep(10);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                if ("file_ok".equals(stutus)) {
//                                    break;
//                                }
//                            }
                            msg.setSourceDataString(stutus);
                            notifyReceive(msg);
                            if (fIn != null) {
                                fIn.close();
                            }
                            if (os != null) {
                                os.close();
                            }
                            if (os != null) {
                                is.close();
                            }
                        }
                        getSocket().close();


                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("onResponse",e.toString());
            }
        }
    }

    public static byte[] addLen(byte[] b, int len) {
        if (b.length < len) {
            byte[] retAry = new byte[len];
            for (int i = 0; i < len - b.length; i++) {
                retAry[i] = 0;
            }
            System.arraycopy(b, 0, retAry, len - b.length, b.length);
            return retAry;
        }
        return b;
    }

    /**
     * 消息接收线程
     */
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

    /**
     * 获取消息接收线程
     *
     * @return
     */
    protected ReceiveThread getReceiveThread() {
        if (mReceiveThread == null || !mReceiveThread.isAlive()) {
            mReceiveThread = new ReceiveThread();
        }
        return mReceiveThread;
    }

    /**
     * 获取消息发送线程
     *
     * @return
     */
    protected SendThread getSendThread() {
        if (mSendThread == null || !mSendThread.isAlive()) {
            mSendThread = new SendThread();
        }
        return mSendThread;
    }

    /**
     * 获取连接线程
     *
     * @return
     */
    protected ConnectionThread getConnectionThread() {
        if (mConnectionThread == null || !mConnectionThread.isAlive() || mConnectionThread.isInterrupted()) {
            mConnectionThread = new ConnectionThread();
        }
        return mConnectionThread;
    }

    /**
     * 判断Socket的连接状态
     *
     * @return
     */
    public ClientState getClientState() {
        return mClientState;
    }

    /**
     * 设置Socket的连接状态
     *
     * @param state
     */
    protected void setClientState(ClientState state) {
        if (mClientState != state) {
            mClientState = state;
        }
    }

    /**
     * 判断是否断开连接
     *
     * @return
     */
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

    }

    int i = 1;

    private void notifyReceive(final TcpMsg tcpMsg) {
        Log.d("onResponse", ">>>:" + i);
        i++;
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
}
