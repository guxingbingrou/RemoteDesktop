package com.skystack.remotedesktop;

/**
 * Time: 2022/11/16
 * Author: zou
 * Description:
 */
public class DataChannelProtocol {
    public static final int TypePos = 0;
    //Touch event protocol
    public static final byte TypeTouchEvent = 0x01;

    public static final byte TouchPointerCountPos = 1;
    public static final byte TouchPointerMaskPos = 2;

    public static final byte TouchPointerXPos = 3;
    public static final byte TouchPointerYPos = 7;
    public static final byte TouchPointerIdPos = 11;

    public static final int TouchMessageLen = 12;


    //param change protocol
    public static final byte TypeParamChange = 0x02;

    public static final byte ParamOrientationPos = 1;
    public static final byte ParamRealWidthPos = 2;
    public static final byte ParamRealHeightPos = 6;

    public static final int ParamMessageLen = 10;


    /**
     * int到byte[] 由低位到高位
     *
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    public static void intToBytes(int i, byte[] dest, int pos) {
        dest[pos + 0] = (byte) (i & 0xFF);
        dest[pos + 1] = (byte) ((i >> 8) & 0xFF);
        dest[pos + 2] = (byte) ((i >> 16) & 0xFF);
        dest[pos + 3] = (byte) ((i >> 24) & 0xFF);
    }

    public static int BytesToInt(byte[] src, int pos){
        int ret = 0;
        ret = src[pos+3] & 0xff;
        ret = (ret << 8) | (src[pos+2] & 0xff);
        ret = (ret << 8) | (src[pos+1] & 0xff);
        ret = (ret << 8) | (src[pos+0] & 0xff);

        return ret;
    }
}
