package com.skystack.remotedesktop;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

/**
 * Time: 2022/11/15
 * Author: zou
 * Description:
 */
public class PeerConnectionAdapter implements PeerConnection.Observer {
    private static final String TAG = "zypPeerConnection";
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.i(TAG, "onIceConnectionChange");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.i(TAG, "onIceConnectionReceivingChange");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.i(TAG, "onSignalingChange");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.i(TAG, "onIceCandidate");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.i(TAG, "onIceCandidatesRemoved");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.i(TAG, "onAddStream");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.i(TAG, "onRemoveStream");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.i(TAG, "onAddTrack");
    }
}
