package com.skystack.remotedesktop;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import org.webrtc.DataChannel;

import java.util.ArrayList;

/**
 * Time: 2022/11/17
 * Author: zou
 * Description:
 */
public class TouchEventProcessor implements RemoteProcessor{
    private static final String TAG = "zypTouchProcessor";
    private static final int singleLength = DataChannelProtocol.TouchMessageLen - DataChannelProtocol.TouchPointerMaskPos - 1;
    private int input_source = InputDevice.SOURCE_TOUCHSCREEN;
    private Instrumentation inst = new Instrumentation();
    private ArrayList<SinkEvent> events_list = new ArrayList<>();

    private class SinkEvent{
        public MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        public MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
        SinkEvent(MotionEvent.PointerCoords coords, MotionEvent.PointerProperties properties){
            pointerCoords = coords;
            pointerProperties = properties;
        }
    }

    @Override
    public void ProcessData(byte[] data) {
        if((data[DataChannelProtocol.TypePos] & 0xff)!= DataChannelProtocol.TypeTouchEvent)
            return;

        int pointerCount = data[DataChannelProtocol.TouchPointerCountPos] & 0xff;

        int pointerMask = data[DataChannelProtocol.TouchPointerMaskPos] & 0xff;
        int pointerId = -1;

        int pos = DataChannelProtocol.TouchPointerXPos;
        while(pos < data.length){
            int x =0, y = 0;
            x = DataChannelProtocol.BytesToInt(data, pos);
            y = DataChannelProtocol.BytesToInt(data, pos + 4);
            pointerId = data[pos + 8] & 0xff;
            Log.i(TAG, "mask: " + pointerMask + "  x: " + x + "  y: " + y + "  id: " + pointerId);
            MotionEvent.PointerProperties newPointerProperties = new MotionEvent.PointerProperties();
            newPointerProperties.id = pointerId;
            newPointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords newPointerCoords = new MotionEvent.PointerCoords();
            newPointerCoords.x = x;
            newPointerCoords.y = y;

            boolean isNewPointer = true;
            for(SinkEvent event : events_list){
                if(event.pointerProperties.id == newPointerProperties.id){
                    isNewPointer = false;
                    event.pointerProperties = newPointerProperties;
                    event.pointerCoords = newPointerCoords;
                    break;
                }
            }
            if(isNewPointer){
                events_list.add(new SinkEvent(newPointerCoords, newPointerProperties));
            }

            pos += singleLength;
        }

        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[events_list.size()];
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[events_list.size()];

        int index = 0;
        int lastIndex = -1;
        SinkEvent lastEvent = null;
        for(SinkEvent event : events_list){
            pointerProperties[index] = event.pointerProperties;
            pointerCoords[index] = event.pointerCoords;

            if(event.pointerProperties.id == pointerId){
                lastEvent = event;
                lastIndex = index;
            }
            index++;
        }

        int actionPoint = (pointerMask != MotionEvent.ACTION_MOVE) ?
                (pointerMask + (lastIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT)) : MotionEvent.ACTION_MOVE;

        MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), actionPoint,
                events_list.size(), pointerProperties, pointerCoords,
                0, 0, 0, 0, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);

        if(pointerMask == MotionEvent.ACTION_POINTER_UP){
            events_list.remove(lastEvent);
        }else if(pointerMask == MotionEvent.ACTION_UP){
            events_list.clear();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                inst.sendPointerSync(event);
            }
        }).start();
    }
}
