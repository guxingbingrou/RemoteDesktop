package com.skystack.remotedesktop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DesktopServerActivity extends AppCompatActivity implements SignalingClient.Callback {
    private static final String TAG = "zypDesktopServer";
    private static Intent mMediaProjectionPermissionResultData;
    private static int mMediaProjectionPermissionResultCode;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private MediaStream localStream;
    private EglBase.Context eglBaseContext;
    private SurfaceTextureHelper surfaceTextureHelper;
    private DataChannel dataChannel;
    private boolean isDataChannelOpen = false;
    private TouchEventProcessor touchEventProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_r_t_c);
        startService(new Intent(this, DesktopServerService.class));


        StartScreenCapture();
    }

    void StartScreenCapture(){
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(MEDIA_PROJECTION_SERVICE);

        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != 1){
            Log.e(TAG, "know request");
            return;
        }

        mMediaProjectionPermissionResultCode = resultCode;
        mMediaProjectionPermissionResultData = data;

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

        //采集系统扬声器声音   需要system权限
        JavaAudioDeviceModule.Builder admBuilder = JavaAudioDeviceModule.builder(this);
        admBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);

        //创建PeerConnectionFactory
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .setAudioDeviceModule(admBuilder.createAudioDeviceModule())
                .createPeerConnectionFactory();

        InitMediaStreams();

    }

    private void InitMediaStreams(){
        surfaceTextureHelper = SurfaceTextureHelper.create("DesktopCaptureThread", eglBaseContext);

        //创建VideoCapture
        VideoCapturer videoCapturer = CreateScreenCapture();

        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());

        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(720, 1280, 30);

        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        localStream = peerConnectionFactory.createLocalMediaStream("LocalStream");
        localStream.addTrack(videoTrack);

        CreatePeerConnection();
    }

    private VideoCapturer CreateScreenCapture(){
        if(mMediaProjectionPermissionResultCode != RESULT_OK){
            Log.e(TAG, "User didn't give permission to capture the screen");
            return null;
        }

        return new ScreenCapturerAndroid(mMediaProjectionPermissionResultData,
                new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.e(TAG,"User revoked permission to capture the screen.");
                    }
                });
    }

    private void CreatePeerConnection(){

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

        });

        peerConnection.addStream(localStream);

        CreateDataChannel();

        SignalingClient.getInstance().SetCallback(this);

    }

    private void CreateDataChannel(){
        DataChannel.Init init = new DataChannel.Init();
        init.ordered = true;
        init.negotiated = false;

        dataChannel = peerConnection.createDataChannel("zyp", init);
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {
                switch (dataChannel.state()){
                    case OPEN:
                        isDataChannelOpen = true;
                        touchEventProcessor = new TouchEventProcessor();
                        SendInitData();
                        break;
                    case CLOSED:
                        isDataChannelOpen = false;
                        break;
                }
                Log.i(TAG, "data channel state is change: " + dataChannel.state());
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.i(TAG, "onDataChannelMessage, size: " + buffer.data.capacity());
                byte[] data = new byte[buffer.data.capacity()];
                buffer.data.get(data);

                switch (data[0]){
                    case DataChannelProtocol.TypeTouchEvent:
                        touchEventProcessor.ProcessData(data);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void SendInitData(){
        if(isDataChannelOpen && dataChannel != null){
            byte[] msg = new byte[DataChannelProtocol.ParamMessageLen];

            msg[DataChannelProtocol.TypePos] = DataChannelProtocol.TypeParamChange;

            Point point = new Point();
            Display defaultDisplay = getWindowManager().getDefaultDisplay();
            defaultDisplay.getRealSize(point);

            msg[DataChannelProtocol.ParamOrientationPos] = (byte) defaultDisplay.getRotation();

            DataChannelProtocol.intToBytes(point.x, msg, DataChannelProtocol.ParamRealWidthPos);
            DataChannelProtocol.intToBytes(point.y, msg, DataChannelProtocol.ParamRealHeightPos);

            DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(msg), true);
            dataChannel.send(buffer);
        }
    }

    private void CreateOfferAndSend(){
        peerConnection.createOffer(new SdpAdapter("create local offer"){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new SdpAdapter(" set local offer"),
                        sessionDescription);

                SignalingClient.getInstance().SendSessionDescription(sessionDescription);
            }

        }, new MediaConstraints());
    }

    @Override
    public void onCreateRoom() {

    }

    @Override
    public void onPeerJoined() {
        CreateOfferAndSend();
    }

    @Override
    public void onSelfJoined() {
        CreateOfferAndSend();
    }

    @Override
    public void onPeerLeave() {

    }

    @Override
    public void onOfferReceived(JSONObject data) {

    }

    @Override
    public void onAnswerReceived(JSONObject data) {
        peerConnection.setRemoteDescription(new SdpAdapter("set remote answer"),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    @Override
    public void onIceCandidateReceived(JSONObject data) {
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }
}