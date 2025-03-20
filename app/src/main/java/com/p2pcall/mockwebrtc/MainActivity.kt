package com.p2pcall.mockwebrtc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.gson.Gson
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer

class MainActivity : AppCompatActivity() {
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var localPeer: PeerConnection
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer

    private lateinit var btnMute: Button
    private lateinit var btnSwitchCamera: Button
    private lateinit var btnEndCall: Button
    private lateinit var btnGenerateOffer: Button

    private var isMuted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        localView = findViewById(R.id.localView)
        remoteView = findViewById(R.id.remoteView)
        btnMute = findViewById(R.id.btnMute)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnGenerateOffer = findViewById(R.id.btnGenerateOffer)

        initializeWebRTC()

        btnMute.setOnClickListener {
            isMuted = !isMuted
            localAudioTrack.setEnabled(!isMuted)
            btnMute.text = if (isMuted) "Unmute" else "Mute"
        }

        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnGenerateOffer.setOnClickListener { createOffer() }
        btnEndCall.setOnClickListener { endCall() }
        // Initialize Views

    }

    private fun initializeWebRTC() {
        Log.d("afdasasf"," initializeWebRTC ")

        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()

        // Setup Video Capture
        videoCapturer = getVideoCapturer()
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)

        // Initialize Views
        localView.init(EglBase.create().eglBaseContext, null)
        remoteView.init(EglBase.create().eglBaseContext, null)

        // Create Peer Connection
        val iceServers = listOf(IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        Log.d("afdasasf"," iceServers $iceServers")

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Log.d("afdasasf"," onSignalingChange ${Gson().toJson(p0)} ")

            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.d("afdasasf"," onIceConnectionChange ")

            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.d("afdasasf"," onIceConnectionReceivingChange ")

            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Log.d("afdasasf"," onIceGatheringChange ")

            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d("afdasasf"," onIceCandidate ")

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "New ICE Candidate", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {
                Log.d("afdasasf"," onIceCandidatesRemoved ")

            }

            override fun onAddStream(stream: MediaStream?)
            {
                Log.d("afdasasf"," onAddStream ")

                stream?.videoTracks?.get(0)?.addSink(remoteView)
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Log.d("afdasasf"," onRemoveStream ")

            }

            override fun onDataChannel(p0: DataChannel?) {
                Log.d("afdasasf"," onDataChannel ")

            }

            override fun onRenegotiationNeeded() {
                Log.d("afdasasf"," onRenegotiationNeeded ")

            }
        })!!

        // Add Media Stream
        val mediaStream = peerConnectionFactory.createLocalMediaStream("stream")
        mediaStream.addTrack(localVideoTrack)
        mediaStream.addTrack(localAudioTrack)
        localPeer.addStream(mediaStream)
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        localPeer.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                localPeer.setLocalDescription(this, sessionDescription)
                Log.d("afdasasf","oncreate success ${Gson().toJson(sessionDescription)}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "SDP Offer Created", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun setRemoteAnswer(sdp: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        localPeer.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, sessionDescription)
    }

    private fun getVideoCapturer(): VideoCapturer {
        return Camera2Enumerator(this).run {
            deviceNames.firstOrNull { isFrontFacing(it) }?.let { createCapturer(it, null) }
        } ?: throw IllegalStateException("No front camera found")
    }

    private fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    private fun endCall() {
        localPeer.close()
        finish()
    }
}


