package com.apace.tcpclientdemo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.apace.tcpclientdemo.adapter.SimpleAdapter;
import com.apace.tcpclientdemo.bean.TargetInfo;
import com.apace.tcpclientdemo.bean.TcpMsg;
import com.apace.tcpclientdemo.client.TcpClient;
import com.apace.tcpclientdemo.config.TcpConnConfig;
import com.apace.tcpclientdemo.listener.TcpClientListener;
import com.apace.tcpclientdemo.utils.ByteUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TcpClientListener {


    private Button tcpclientBuConnect;
    private EditText tcpclientEdit;
    private EditText tcpclientEditIp;
    private Button tcpclientBuSend;
    private Button tcpclientBuSendFile;
    private Button btn_clean;
    private SwitchCompat tcpclientSwitchReconnect;
    private TcpClient mTcpClient;
    private RecyclerView rv_content;
    private List<String> list = new ArrayList<>();
    private SimpleAdapter mAdapter;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TcpMsg message = (TcpMsg) msg.obj;
            list.add(message.getSourceDataString());
            mAdapter.notifyDataSetChanged();
        }
    };

    private static final int FILE_SELECT_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tcpclientBuConnect = (Button) findViewById(R.id.tcpclient_bu_connect);
        tcpclientEdit = (EditText) findViewById(R.id.tcpclient_edit);
        tcpclientBuSend = (Button) findViewById(R.id.tcpclient_bu_send);
        tcpclientBuSendFile = (Button) findViewById(R.id.tcpclient_bu_send_file);
        btn_clean = (Button) findViewById(R.id.clean);
        tcpclientEditIp = (EditText) findViewById(R.id.tcpclient_edit_ip);
        tcpclientSwitchReconnect = (SwitchCompat) findViewById(R.id.tcpclient_switch_reconnect);
        rv_content = (RecyclerView) findViewById(R.id.rv_content);
        tcpclientBuConnect.setOnClickListener(this);
        tcpclientBuSendFile.setOnClickListener(this);
        tcpclientBuSend.setOnClickListener(this);
        btn_clean.setOnClickListener(this);
        mAdapter = new SimpleAdapter(list, this);
        rv_content.setLayoutManager(new LinearLayoutManager(this));
        rv_content.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tcpclient_bu_connect) {
            if (mTcpClient != null && mTcpClient.isConnected()) {
                mTcpClient.disconnect();
            } else {
                String temp = tcpclientEditIp.getText().toString().trim();
                String[] temp2 = temp.split(":");
                TargetInfo targetInfo = new TargetInfo(temp2[0], Integer.parseInt(temp2[1]));
                mTcpClient = TcpClient.getTcpClient(targetInfo);
                mTcpClient.addTcpClientListener(this);
                mTcpClient.config(new TcpConnConfig.Builder()
                        .setIsReconnect(tcpclientSwitchReconnect.isChecked())
                        .create());
                if (mTcpClient.isDisconnected()) {
                    mTcpClient.connect();
                } else {
                    Toast.makeText(this, "已经存在该连接", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (v.getId() == R.id.tcpclient_bu_send) {//send msg
            String text = tcpclientEdit.getText().toString().trim();
            if (mTcpClient != null) {
                List<byte[]> sendData = ByteUtils.getSendData(1249537, true, text);
                for (byte[] b : sendData) {
                    System.out.println("发送数据："+ Arrays.toString(b));
                    mTcpClient.sendMsg(b);
                }

            } else {
                Toast.makeText(this, "还没有连接到服务器", Toast.LENGTH_SHORT).show();
            }
        } else if (v.getId() == R.id.tcpclient_bu_send_file) {
            showFileChooser();
        } else if (v.getId() == R.id.clean) {
            list.clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("onResponse", "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    try {
                        path = getPath(this, uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    Log.d("onResponse", "File Path: " + path);
                    File file = new File(path);
                    if (mTcpClient != null) {
                        mTcpClient.sendMsg(file);
                    } else {
                        Toast.makeText(this, "还没有连接到服务器", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it  Or Log it.
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    @Override
    public void onConnected(TcpClient client) {
        TcpMsg tcpMsg = new TcpMsg("连接成功", client.getTargetInfo(), TcpMsg.MsgType.Send);
        Message message = Message.obtain();
        message.obj = tcpMsg;
        handler.sendMessage(message);

    }

    @Override
    public void onSended(TcpClient client, TcpMsg tcpMsg) {
        Message message = Message.obtain();
        message.obj = tcpMsg;
        handler.sendMessage(message);
    }

    @Override
    public void onDisconnected(TcpClient client, String msg, Exception e) {
        TcpMsg tcpMsg = new TcpMsg("连接断开啦", client.getTargetInfo(), TcpMsg.MsgType.Send);
        Message message = Message.obtain();
        message.obj = tcpMsg;
        handler.sendMessage(message);
    }

    @Override
    public void onReceive(TcpClient client, TcpMsg tcpMsg) {
        Message message = Message.obtain();
        message.obj = tcpMsg;
        handler.sendMessage(message);

    }
}
