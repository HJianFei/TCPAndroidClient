package com.apace.tcpclientdemo.config;


import com.apace.tcpclientdemo.utils.CharsetUtil;

public class TcpConnConfig {
    private String charsetName = CharsetUtil.UTF_8;//默认编码
    private long connTimeout = 5000;//连接超时时间
    private long receiveTimeout = 0;//接受消息的超时时间,0为无限大
    private boolean isReconnect = false;//是否重连
    private int localPort = -1;

    private TcpConnConfig() {
    }

    public String getCharsetName() {
        return charsetName;
    }

    public long getConnTimeout() {
        return connTimeout;
    }

    public boolean isReconnect() {
        return isReconnect;
    }


    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public int getLocalPort() {
        return localPort;
    }

    public static class Builder {
        private TcpConnConfig mTcpConnConfig;

        public Builder() {
            mTcpConnConfig = new TcpConnConfig();
        }

        public TcpConnConfig create() {
            return mTcpConnConfig;
        }

        public Builder setCharsetName(String charsetName) {
            mTcpConnConfig.charsetName = charsetName;
            return this;
        }


        public Builder setConnTimeout(long timeout) {
            mTcpConnConfig.connTimeout = timeout;
            return this;
        }

        public Builder setIsReconnect(boolean b) {
            mTcpConnConfig.isReconnect = b;
            return this;
        }
    }
}
