package com.skystack.remotedesktop;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Time: 2022/11/16
 * Author: zou
 * Description:
 */
public class SdpAdapter implements SdpObserver {
    private static final String TAG = "zypSdpAdapter";
    private final String sdpName;
    public SdpAdapter(String sdpName){
        this.sdpName = sdpName;
    }
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.i(TAG, sdpName + ": onCreateSuccess");
    }

    @Override
    public void onSetSuccess() {
        Log.i(TAG, sdpName + ": onSetSuccess");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.i(TAG, sdpName + ": onCreateFailure");
    }

    @Override
    public void onSetFailure(String s) {
        Log.i(TAG, sdpName + ": onSetFailure");
    }
}
