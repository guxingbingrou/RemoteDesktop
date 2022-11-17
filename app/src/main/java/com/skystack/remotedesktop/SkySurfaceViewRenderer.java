package com.skystack.remotedesktop;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.webrtc.SurfaceViewRenderer;


/**
 * Time: 2022/11/16
 * Author: zou
 * Description:
 */
public class SkySurfaceViewRenderer extends SurfaceViewRenderer {
    private static final String TAG = "zypSkySurfaceView";
    private int serverWidth;
    private int serverHeight;
    private float wRadio;
    private float hRadio;
    private int orientation;
    private Callback callback;


    public SkySurfaceViewRenderer(Context context) {
        super(context);
    }

    public SkySurfaceViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                SendTouchEvent(event, 1);
                break;
            case MotionEvent.ACTION_MOVE:
                SendTouchEvent(event, event.getPointerCount());
                break;
        }

        return true;
    }

    private void SendTouchEvent(MotionEvent event, int pointCount) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            wRadio = getWidth() / (float)serverWidth;
            hRadio = getHeight() / (float)serverHeight;
        } else {
            wRadio = getWidth() / (float)serverHeight;
            hRadio = getHeight() / (float)serverWidth;
        }

        int singleLength = DataChannelProtocol.TouchMessageLen - DataChannelProtocol.TouchPointerMaskPos - 1;
        int messageLength = DataChannelProtocol.TouchMessageLen + (pointCount - 1) * singleLength;

        byte[] messageBytes = new byte[messageLength];

        messageBytes[DataChannelProtocol.TypePos] = DataChannelProtocol.TypeTouchEvent;
        messageBytes[DataChannelProtocol.TouchPointerCountPos] = (byte) pointCount;
        messageBytes[DataChannelProtocol.TouchPointerMaskPos] = (byte) event.getActionMasked();

        for (int i = 0; i < pointCount; ++i) {
            DataChannelProtocol.intToBytes((int) (event.getX(i) / wRadio), messageBytes, DataChannelProtocol.TouchPointerXPos + singleLength * i);
            DataChannelProtocol.intToBytes((int) (event.getY(i) / hRadio), messageBytes, DataChannelProtocol.TouchPointerYPos + singleLength * i);

            messageBytes[DataChannelProtocol.TouchPointerIdPos + singleLength * i] = (byte) event.getPointerId(i);

//            Log.i(TAG, "wRadio: " + wRadio + "  hRadio: " + hRadio);
//            Log.i(TAG, "mask: " + event.getActionMasked() + "  x: " + event.getX(i) + "  y: " + event.getY(i) + "  id: " + event.getPointerId(i));
        }

        callback.OnTouchEventDone(messageBytes);

    }

    public void OnParamChanged(int w, int h, int rotation) {
        Log.i(TAG, "OnParamChanged: " + w + " x " + h + " x " + orientation);
        serverWidth = w;
        serverHeight = h;
        switch (rotation){
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                orientation = Configuration.ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                 orientation = Configuration.ORIENTATION_PORTRAIT;
                 break;
        }
    }

    public void SetCallback(Callback callback){
        this.callback = callback;
    }

    public interface Callback{
        void OnTouchEventDone(byte[] messageBytes);
    }
}
