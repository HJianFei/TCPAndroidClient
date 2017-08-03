package com.apace.tcpclientdemo.listener;


import com.apace.tcpclientdemo.bean.TcpMsg;
import com.apace.tcpclientdemo.client.TcpClient;


/**
 * Created by Administrator at 2017/8/3
 * Description: TCP连接监听接口
 */
public interface TcpClientListener {

    void onConnected(TcpClient client);

    void onSended(TcpClient client, TcpMsg tcpMsg);

    void onDisconnected(TcpClient client, String msg, Exception e);

    void onReceive(TcpClient client, TcpMsg tcpMsg);

}
