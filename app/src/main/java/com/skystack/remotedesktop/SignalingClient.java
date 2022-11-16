package com.skystack.remotedesktop;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Time: 2022/11/16
 * Author: zou
 * Description:
 */
public class SignalingClient {
    private static final String TAG = "zypSignalingClient";
    private static SignalingClient instance;
    private Socket socket;
    private String room = "RemoteDesktop";
    private Callback callback;

    private SignalingClient(){
        init();
    }

    //单例模式
    public static SignalingClient getInstance(){
        if(instance == null){
            synchronized (SignalingClient.class){
                if(instance == null){
                    instance = new SignalingClient();
                }
            }
        }

        return instance;
    }


    private final TrustManager[] trustAll = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };

    private void init(){
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, null);

            IO.setDefaultHostnameVerifier(((hostname, session) -> true));
            IO.setDefaultSSLContext(sslContext);

            socket = IO.socket("https://119.91.25.164:9000");
            socket.connect();

            socket.emit("create or join", room);

            socket.on("created", args -> {
                Log.i(TAG,"you have created the room: " + room);
                callback.onCreateRoom();
            });

            socket.on("full", args -> {
               Log.i(TAG, "the room: " + room + " is full");
            });

            socket.on("join", args -> {
                Log.i(TAG,"peer has joined the room: " + room);
                callback.onPeerJoined();
            });

            socket.on("joined", args -> {
                Log.i(TAG,"you have joined the room: " + room);
                callback.onSelfJoined();
            });

            socket.on("bye", args -> {
                Log.i(TAG,"peer have leave the room: " + room);
                callback.onPeerLeave();
            });

            socket.on("message", args -> {
                Log.i(TAG, "message: " + Arrays.toString(args));
                Object arg = args[0];
                if(arg instanceof String){

                }else if(arg instanceof JSONObject){
                    JSONObject data = (JSONObject) arg;
                    String type = data.optString("type");

                    if("offer".equals(type)){
                        callback.onOfferReceived(data);
                    }else if("answer".equals(type)){
                        callback.onAnswerReceived(data);
                    }else if("candidate".equals(type)){
                        callback.onIceCandidateReceived(data);
                    }
                }
            });

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    void SendSessionDescription(SessionDescription sessionDescription){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", sessionDescription.type.canonicalForm());
            jsonObject.put("sdp", sessionDescription.description);
            socket.emit("message", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    void SendIceCandidate(IceCandidate iceCandidate) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", "candidate");
            jsonObject.put("label", iceCandidate.sdpMLineIndex);
            jsonObject.put("id", iceCandidate.sdpMid);
            jsonObject.put("candidate", iceCandidate.sdp);

            socket.emit("message", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void SetCallback(Callback callback){
        this.callback = callback;
    }

    public interface Callback {
        void onCreateRoom();
        void onPeerJoined();
        void onSelfJoined();
        void onPeerLeave();

        void onOfferReceived(JSONObject data);
        void onAnswerReceived(JSONObject data);
        void onIceCandidateReceived(JSONObject data);
    }
}
