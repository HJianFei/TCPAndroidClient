package com.apace.tcpclientdemo.bean;

/**
 * Created by Administrator at 2017/8/3
 * Description: 发送的源的IP,端口
 */
public class TargetInfo {
    private String ip;
    private int port;

    public TargetInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "TargetInfo{" +
                "ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                '}';
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
