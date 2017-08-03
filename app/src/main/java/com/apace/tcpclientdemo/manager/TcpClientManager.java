package com.apace.tcpclientdemo.manager;

import com.apace.tcpclientdemo.bean.TargetInfo;
import com.apace.tcpclientdemo.client.TcpClient;

import java.util.HashSet;
import java.util.Set;

public class TcpClientManager {

    private static Set<TcpClient> sMXTcpClients = new HashSet<>();

    public static void putTcpClient(TcpClient mTcpClient) {
        sMXTcpClients.add(mTcpClient);
    }

    public static TcpClient getTcpClient(TargetInfo targetInfo) {
        for (TcpClient tc : sMXTcpClients) {
            if (tc.getTargetInfo().equals(targetInfo)) {
                return tc;
            }
        }
        return null;
    }
}
