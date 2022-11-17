package com.skystack.remotedesktop;

/**
 * Time: 2022/11/17
 * Author: zou
 * Description:
 */
public class ParamProcessor implements RemoteProcessor{
    private static final String TAG = "zypParamProcessor";

    private int width;
    private int height;
    private int orientation;
    private Callback callback;

    @Override
    public void ProcessData(byte[] data) {
        if((data[DataChannelProtocol.TypePos] & 0xff ) != DataChannelProtocol.TypeParamChange)
            return;

        orientation = data[DataChannelProtocol.ParamOrientationPos] & 0xff;
        width = DataChannelProtocol.BytesToInt(data, DataChannelProtocol.ParamRealWidthPos);
        height = DataChannelProtocol.BytesToInt(data, DataChannelProtocol.ParamRealHeightPos);

        callback.onParamChanged(width, height, orientation);
    }

    public void SetCallBack(Callback callback){
        this.callback = callback;
    }

    public interface Callback{
        void onParamChanged(int w, int h, int orientation);
    }
}
