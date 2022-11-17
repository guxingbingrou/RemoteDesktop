package com.skystack.remotedesktop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DesktopClientActivity extends AppCompatActivity implements SignalingClient.Callback,
        SkySurfaceViewRenderer.Callback, ParamProcessor.Callback{
    SkySurfaceViewRenderer remoteView;
    private static final String TAG = "zypDesktopClient";
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel;
    private boolean isDataChannelOpen = false;
    private ParamProcessor paramProcessor;

    EglBase.Context eglBaseContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_desktop_client);

        InitPeerConnection();
    }

    private void InitPeerConnection(){
        //初始化PeerConnectionFactory
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                .builder(this).createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //创建编解码器
        eglBaseContext = EglBase.create().getEglBaseContext();

        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(eglBaseContext);

        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                eglBaseContext, true, true
        );


        //创建PeerConnectionFactory
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        remoteView = findViewById(R.id.remoteView);
        remoteView.setMirror(false);
        remoteView.init(eglBaseContext, null);
        remoteView.SetCallback(this);

        CreatePeerConnection();
    }

    private void CreatePeerConnection(){
        SignalingClient.getInstance().SetCallback(this);

        List<PeerConnection.IceServer> iceServerList = new ArrayList<>();
        iceServerList.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServerList.add(PeerConnection.IceServer.builder("turn:119.91.25.164:3478")
                .setUsername("zyp").setPassword("zyp12").createIceServer());

        peerConnection = peerConnectionFactory.createPeerConnection(iceServerList, new PeerConnectionAdapter(){
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                SignalingClient.getInstance().SendIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);

                runOnUiThread(() -> {
                    remoteVideoTrack.addSink(remoteView);
                });

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                super.onDataChannel(dataChannel);
                SetDataChannel(dataChannel);
            }
        });

    }

    public void SetDataChannel(DataChannel dataChannel){
        this.dataChannel = dataChannel;
        this.dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {
                switch (dataChannel.state()){
                    case OPEN:
                        isDataChannelOpen = true;
                        paramProcessor = new ParamProcessor();
                        paramProcessor.SetCallBack(GetThis());
                        break;
                    case CLOSED:
                        isDataChannelOpen = false;
                        paramProcessor = null;
                        break;
                }
                Log.i(TAG, "data channel state is change: " + dataChannel.state());
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                byte[] data = new byte[buffer.data.capacity()];
                buffer.data.get(data);

                switch (data[0]){
                    case DataChannelProtocol.TypeParamChange:
                        paramProcessor.ProcessData(data);
                        break;
                    default:
                        break;
                }

            }
        });

    }

    @Override
    public void onCreateRoom() {

    }

    @Override
    public void onPeerJoined() {

    }

    @Override
    public void onSelfJoined() {

    }

    @Override
    public void onPeerLeave() {

    }

    @Override
    public void onOfferReceived(JSONObject data) {
        runOnUiThread(()->{
            peerConnection.setRemoteDescription(new SdpAdapter("set remote offer"),
                    new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));

            peerConnection.createAnswer(new SdpAdapter("create answer"){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);

                    peerConnection.setLocalDescription(new SdpAdapter("set local answer"),
                            sessionDescription);

                    SignalingClient.getInstance().SendSessionDescription(sessionDescription);
                }
            }, new MediaConstraints());
        });
    }

    @Override
    public void onAnswerReceived(JSONObject data) {

    }

    @Override
    public void onIceCandidateReceived(JSONObject data) {
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    @Override
    public void OnTouchEventDone(byte[] messageBytes) {
        if(isDataChannelOpen && dataChannel != null){
            DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(messageBytes), true);
            dataChannel.send(buffer);
        }
    }

    @Override
    public void onParamChanged(int w, int h, int orientation) {
        remoteView.OnParamChanged(w, h, orientation);
    }

    public DesktopClientActivity GetThis(){
        return this;
    }

    @Override
    public void onBackPressed() {
        
    }
}