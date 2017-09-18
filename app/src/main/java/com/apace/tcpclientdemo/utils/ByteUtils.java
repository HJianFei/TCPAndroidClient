package com.apace.tcpclientdemo.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteUtils {

    /**
     * 命令编号
     *
     * @param data
     * @return
     */
    public static byte[] getTerminalBytes(int data) {
        byte[] bytes = new byte[3];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        bytes[2] = (byte) ((data & 0xff0000) >> 16);
        return bytes;
    }

    /**
     * 发送时间
     *
     * @param data
     * @return
     */
    public static byte[] getTimeBytes(long data) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data >> 8) & 0xff);
        bytes[2] = (byte) ((data >> 16) & 0xff);
        bytes[3] = (byte) ((data >> 24) & 0xff);
        bytes[4] = (byte) ((data >> 32) & 0xff);
        bytes[5] = (byte) ((data >> 40) & 0xff);
        bytes[6] = (byte) ((data >> 48) & 0xff);
        bytes[7] = (byte) ((data >> 56) & 0xff);
        return bytes;
    }

    /**
     * 流水号
     *
     * @param data
     * @return
     */
    public static byte[] getSerialBytes(int data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }

    /**
     * 后台服务器用户标识
     *
     * @param data
     * @return
     */
    public static byte[] getServerBytes(int data) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (data & 0xff);
        return bytes;
    }

    /**
     * 发送用户Id
     *
     * @param data
     * @return
     */
    public static byte[] getUserIdBytes(int data) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        bytes[2] = (byte) ((data & 0xff0000) >> 16);
        bytes[3] = (byte) ((data & 0xff000000) >> 24);
        return bytes;
    }

    /**
     * 总包数
     *
     * @param data
     * @return
     */
    public static byte[] getAllPacketBytes(int data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }

    /**
     * 当前包
     *
     * @param data
     * @return
     */
    public static byte[] getCurrPacketBytes(int data) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        return bytes;
    }

    /**
     * 包内容属性
     *
     * @param subpackage 是否分包
     * @param encrypt    是否加密
     * @param length     数据长度
     * @return
     */
    public static byte[] getPacketBytes(boolean subpackage, boolean encrypt, int length) {
        byte[] data = new byte[2];
        String str = "";
        String binaryLength = toBinaryString(length);
        if (subpackage && encrypt) {// 分包且加密
            str = "001001" + binaryLength;
        } else if (subpackage && !encrypt) {// 分包但不加密
            str = "001000" + binaryLength;

        } else if (!subpackage && encrypt) {// 不分包但加密
            str = "000001" + binaryLength;

        } else if (!subpackage && !encrypt) {// 不分包不加密
            str = "000000" + binaryLength;
        }
        data = BinaryToByteArray(str);
        return data;
    }

    /**
     * 用户验证标志
     *
     * @param verity
     * @return
     */
    public static byte[] getVerifyBytes(String verity) {

        byte[] data = null;
        try {
            data = verity.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return addLen(data, 32);
    }

    /**
     * 字节数组自定义长度
     *
     * @param b   原数组
     * @param len 数组长度
     * @return
     */
    public static byte[] addLen(byte[] b, int len) {

        if (b != null && b.length < len) {
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
     * 将 int 类型数据转成二进制的字符串，不足 int 类型位数时在前面添“0”以凑足位数
     *
     * @param num
     * @return
     */
    public static String toBinaryString(int num) {
        char[] chs = new char[10];
        for (int i = 0; i < 10; i++) {
            chs[9 - i] = (char) (((num >> i) & 1) + '0');
        }
        return new String(chs);
    }

    /**
     * 二进制字符串转字节数组
     *
     * @param binaryString
     * @return
     */
    public static byte[] BinaryToByteArray(String binaryString) {

        binaryString = binaryString.trim();
        System.out.println(binaryString);
        if ((binaryString.length() % 8) != 0) {
            System.out.println("二进制字符串长度不对");
            return null;
        }
        byte[] buffer = new byte[binaryString.length() / 8];
        for (int i = 0; i < buffer.length; i++) {
            String tmp = binaryString.substring(i * 8, (i + 1) * 8).trim();
            byte bytes = (byte) Integer.parseInt(tmp, 2);
            buffer[i] = bytes;
        }
        return buffer;
    }

    /**
     * 字节数组转字符串
     *
     * @param data 两个字节数组
     * @return
     */
    public static String ByteToString(byte[] data) {

        String tmpStr = new BigInteger(1, data).toString(2);
        if (tmpStr.length() < 16) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 16 - tmpStr.length(); i++) {
                builder.append("0");
            }
            tmpStr = builder.append(tmpStr).toString();
        }
        return tmpStr;
    }

    /**
     * 获取发送的数据内容
     *
     * @param cmd     命令编号
     * @param encrypt 是否需要压缩
     * @param content 发送内容
     * @return
     */
    public static List<byte[]> getSendData(int cmd, boolean encrypt, String content) {

        try {

            int len = 724;
            String pwd = "12345678";
            byte[] start = new byte[]{40};// 开始符
            byte[] terminalBytes = getTerminalBytes(cmd);// 命令编号
            System.out.println(Arrays.toString(terminalBytes));
            long l = System.currentTimeMillis();
            System.out.println("time:" + l);
            byte[] timeBytes = getTimeBytes(l);// 发送时间
            byte[] serialBytes = getSerialBytes(12345);// 流水号
            byte[] serverBytes = getServerBytes(123);// 后台服务器用户标识
            byte[] verifyBytes = getVerifyBytes("123456");// 用户验证标识
            System.out.println("用户验证标识："+Arrays.toString(verifyBytes));
            byte[] userIdBytes = getUserIdBytes(123456);// 发送用户Id
            byte[] end = new byte[]{41};// 结束符

            List<byte[]> byteList = new ArrayList<>();

            byte[] tmp = content.getBytes("UTF-8");// 包内容字节数组

            byte[] contentBytes = null;
            if (encrypt) {
                contentBytes = DESUtils.encrypt(tmp, pwd);// 数据加密
            } else {
                contentBytes = tmp;// 不需要加密
            }
            System.out.println("全部内容：" + Arrays.toString(contentBytes));
            int packetCount = contentBytes.length / (len - 4);// 数据内容分包数，为什么-4，是因为包内容里面包含2个字节的总包数，2个字节的当前包
            int endPacketLength = (contentBytes.length) % (len - 4);// 最后一个数据包的长度

            byte[] allPacketBytes = null;
            if (endPacketLength > 0) {
                allPacketBytes = getAllPacketBytes(packetCount + 1);
                endPacketLength = endPacketLength + 4;
                System.out.println("总包数：" + (packetCount + 1));
            } else {
                allPacketBytes = getAllPacketBytes(packetCount);
                System.out.println("总包数：" + packetCount);
            }

            int i = 0;
            if (packetCount > 1 || (packetCount == 1 && endPacketLength > 0)) {// 需要分包处理
                System.out.println("分包处理");
                for (i = 0; i < packetCount; i++) {
                    // 临时协议头
                    byte[] tmp_head = new byte[53];
                    // 每个数据包长度
                    byte[] retAryBytes = new byte[len];
                    // 需要校验的数据内容
                    byte[] data = new byte[tmp_head.length + len];
                    // 最后发送的数据包
                    byte[] allData = new byte[tmp_head.length + len + 3];
                    // 当前包
                    byte[] currPacketBytes = getCurrPacketBytes(i + 1);
                    // 包内容
                    System.arraycopy(allPacketBytes, 0, retAryBytes, 0, allPacketBytes.length);
                    System.arraycopy(currPacketBytes, 0, retAryBytes, allPacketBytes.length, currPacketBytes.length);
                    System.arraycopy(contentBytes, i * (len - 4), retAryBytes,
                            allPacketBytes.length + currPacketBytes.length, (len - 4));
                    System.out.println("当前包内容：" + Arrays.toString(retAryBytes));
                    // 包内容属性
                    byte[] packetBytes = getPacketBytes(true, encrypt, len);
                    System.out.println(Arrays.toString(packetBytes));
                    // 数组合并：开始符
                    System.arraycopy(start, 0, tmp_head, 0, start.length);
                    // 数组合并：开始符+命令编号
                    System.arraycopy(terminalBytes, 0, tmp_head, start.length, terminalBytes.length);
                    // 数组合并：开始符+命令编号+发送时间
                    System.arraycopy(timeBytes, 0, tmp_head, start.length + terminalBytes.length, timeBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性
                    System.arraycopy(packetBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length,
                            packetBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号
                    System.arraycopy(serialBytes, 0, tmp_head,
                            start.length + terminalBytes.length + timeBytes.length + packetBytes.length,
                            serialBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识
                    System.arraycopy(serverBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                            + packetBytes.length + serialBytes.length, serverBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识
                    System.arraycopy(verifyBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                            + packetBytes.length + serialBytes.length + serverBytes.length, verifyBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id
                    System.arraycopy(
                            userIdBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                                    + packetBytes.length + serialBytes.length + serverBytes.length + verifyBytes.length,
                            userIdBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容
                    System.arraycopy(tmp_head, 0, data, 0, tmp_head.length);
                    System.arraycopy(retAryBytes, 0, data, tmp_head.length, retAryBytes.length);
                    byte[] crcBytes = getCrc(data);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码
                    System.arraycopy(data, 0, allData, 0, data.length);
                    System.arraycopy(crcBytes, 0, allData, data.length, crcBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码+结束符
                    System.arraycopy(end, 0, allData, data.length + crcBytes.length, end.length);
                    byteList.add(allData);

                }
                if (endPacketLength > 4) {// 最后一个数据包
                    // 临时协议头
                    byte[] tmp_head = new byte[53];
                    // 需要校验的数据内容
                    byte[] data = new byte[tmp_head.length + endPacketLength];
                    // 最后发送的数据包
                    byte[] allData = new byte[tmp_head.length + endPacketLength + 3];
                    // 最后一个数据包的长度
                    byte[] endPacket = new byte[endPacketLength];
                    // 当前包
                    byte[] currPacketBytes = getCurrPacketBytes(i + 1);

                    // 包内容
                    System.arraycopy(allPacketBytes, 0, endPacket, 0, allPacketBytes.length);
                    System.arraycopy(currPacketBytes, 0, endPacket, allPacketBytes.length, currPacketBytes.length);
                    System.arraycopy(contentBytes, i * (len - 4), endPacket,
                            allPacketBytes.length + currPacketBytes.length, (endPacketLength - 4));
                    System.out.println("当前包内容：" + Arrays.toString(endPacket));
                    // 包内容属性
                    byte[] packetBytes = getPacketBytes(true, encrypt, endPacketLength);
                    // 数组合并：开始符
                    System.arraycopy(start, 0, tmp_head, 0, start.length);
                    // 数组合并：开始符+命令编号
                    System.arraycopy(terminalBytes, 0, tmp_head, start.length, terminalBytes.length);
                    // 数组合并：开始符+命令编号+发送时间
                    System.arraycopy(timeBytes, 0, tmp_head, start.length + terminalBytes.length, timeBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性
                    System.arraycopy(packetBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length,
                            packetBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号
                    System.arraycopy(serialBytes, 0, tmp_head,
                            start.length + terminalBytes.length + timeBytes.length + packetBytes.length,
                            serialBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识
                    System.arraycopy(serverBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                            + packetBytes.length + serialBytes.length, serverBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识
                    System.arraycopy(verifyBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                            + packetBytes.length + serialBytes.length + serverBytes.length, verifyBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id
                    System.arraycopy(
                            userIdBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                                    + packetBytes.length + serialBytes.length + serverBytes.length + verifyBytes.length,
                            userIdBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容
                    System.arraycopy(tmp_head, 0, data, 0, tmp_head.length);
                    System.arraycopy(endPacket, 0, data, tmp_head.length, endPacket.length);
                    byte[] crcBytes = getCrc(data);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码
                    System.arraycopy(data, 0, allData, 0, data.length);
                    System.arraycopy(crcBytes, 0, allData, data.length, crcBytes.length);
                    // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码+结束符
                    System.arraycopy(end, 0, allData, data.length + crcBytes.length, end.length);
                    byteList.add(allData);
                }

            } else {// 不需要分包处理
                System.out.println("不分包处理");
                // 临时协议头
                byte[] tmp_head = new byte[53];
                // 需要验证的数据内容
                byte[] data = new byte[tmp_head.length + contentBytes.length];
                // 最后发送的数据包
                byte[] allData = new byte[tmp_head.length + contentBytes.length + 3];
                // 包内容属性
                byte[] packetBytes = getPacketBytes(false, encrypt, contentBytes.length);
                System.out.println("包内容属性："+Arrays.toString(packetBytes));
                // 数组合并：开始符
                System.arraycopy(start, 0, tmp_head, 0, start.length);
                // 数组合并：开始符+命令编号
                System.arraycopy(terminalBytes, 0, tmp_head, start.length, terminalBytes.length);
                // 数组合并：开始符+命令编号+发送时间
                System.arraycopy(timeBytes, 0, tmp_head, start.length + terminalBytes.length, timeBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性
                System.arraycopy(packetBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length,
                        packetBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号
                System.arraycopy(serialBytes, 0, tmp_head,
                        start.length + terminalBytes.length + timeBytes.length + packetBytes.length,
                        serialBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识
                System.arraycopy(serverBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                        + packetBytes.length + serialBytes.length, serverBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识
                System.arraycopy(verifyBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                        + packetBytes.length + serialBytes.length + serverBytes.length, verifyBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id
                System.arraycopy(
                        userIdBytes, 0, tmp_head, start.length + terminalBytes.length + timeBytes.length
                                + packetBytes.length + serialBytes.length + serverBytes.length + verifyBytes.length,
                        userIdBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容
                System.arraycopy(tmp_head, 0, data, 0, tmp_head.length);
                System.arraycopy(contentBytes, 0, data, tmp_head.length, contentBytes.length);
                System.out.println("当前包内容：" + Arrays.toString(contentBytes));
                byte[] crcBytes = getCrc(data);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码
                System.arraycopy(data, 0, allData, 0, data.length);
                System.arraycopy(crcBytes, 0, allData, data.length, crcBytes.length);
                // 数组合并：开始符+命令编号+发送时间+包内容属性+流水号+后台服务器用户标识+用户验证标识+发送用户Id+包内容+校验码+结束符
                System.arraycopy(end, 0, allData, data.length + crcBytes.length, end.length);
                byteList.add(allData);

            }
            return byteList;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 获取CRC16校验码
     *
     * @param data
     * @return
     */
    public static byte[] getCrc(byte[] data) {
        int high;
        int flag;

        // 16位寄存器，所有数位均为1
        int wcrc = 0xffff;
        for (int i = 0; i < data.length; i++) {
            // 16 位寄存器的高位字节
            high = wcrc >> 8;
            // 取被校验串的一个字节与 16 位寄存器的高位字节进行“异或”运算
            wcrc = high ^ data[i];

            for (int j = 0; j < 8; j++) {
                flag = wcrc & 0x0001;
                // 把这个 16 寄存器向右移一位
                wcrc = wcrc >> 1;
                // 若向右(标记位)移出的数位是 1,则生成多项式 1010 0000 0000 0001 和这个寄存器进行“异或”运算
                if (flag == 1)
                    wcrc ^= 0xa001;
            }
        }
        String hexString = Integer.toHexString(wcrc);
        if (hexString.length() > 4) {
            return HexString2Bytes(hexString.substring(4));
        } else {
            return HexString2Bytes(hexString);
        }

    }

    // 从十六进制字符串到字节数组转换
    public static byte[] HexString2Bytes(String hexstr) {
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        return b;
    }

    private static int parse(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }
}
