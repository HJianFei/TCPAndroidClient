package com.apace.tcpclientdemo.bean;

import java.io.File;
import java.util.Arrays;


public class TcpMsg {
    public enum MsgType {
        Send, Receive
    }

    private byte[] sourceDataBytes;//数据源
    private String sourceDataString;//数据源
    private TargetInfo target;
    private long time;//发送、接受消息的时间戳
    private MsgType mMsgType = MsgType.Send;
    private File file;


    public TcpMsg(byte[] data, TargetInfo target, MsgType type) {
        this.sourceDataBytes = data;
        this.target = target;
        this.mMsgType = type;
    }

    public TcpMsg(String data, TargetInfo target, MsgType type) {
        this.target = target;
        this.sourceDataString = data;
        this.mMsgType = type;
    }

    public TcpMsg(File file, TargetInfo target, MsgType mMsgType) {
        this.target = target;
        this.mMsgType = mMsgType;
        this.file = file;
    }

    public void setTime() {
        time = System.currentTimeMillis();
    }


    public long getTime() {
        return time;
    }


    public MsgType getMsgType() {
        return mMsgType;
    }

    public void setMsgType(MsgType msgType) {
        mMsgType = msgType;
    }

    @Override
    public String toString() {

        return "TcpMsg{" +
                "sourceDataBytes=" + Arrays.toString(sourceDataBytes) +
                ", sourceDataString='" + sourceDataString + '\'' +
                ", target=" + target +
                ", time=" + time +
                ", msgtyoe=" + mMsgType +
                '}';
    }


    public byte[] getSourceDataBytes() {
        return sourceDataBytes;
    }

    public void setSourceDataBytes(byte[] sourceDataBytes) {
        this.sourceDataBytes = sourceDataBytes;
    }

    public String getSourceDataString() {
        return sourceDataString;
    }

    public void setSourceDataString(String sourceDataString) {
        this.sourceDataString = sourceDataString;
    }

    public TargetInfo getTarget() {
        return target;
    }

    public void setTarget(TargetInfo target) {
        this.target = target;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
