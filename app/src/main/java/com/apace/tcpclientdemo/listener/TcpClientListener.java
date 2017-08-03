package com.apace.tcpclientdemo.listener;


import com.apace.tcpclientdemo.bean.TcpMsg;
import com.apace.tcpclientdemo.client.TcpClient;


public interface TcpClientListener {

    void onConnected(TcpClient client);

    void onSended(TcpClient client, TcpMsg tcpMsg);

    void onDisconnected(TcpClient client, String msg, Exception e);

    void onReceive(TcpClient client, TcpMsg tcpMsg);

    void onValidationFail(TcpClient client, TcpMsg tcpMsg);

}
