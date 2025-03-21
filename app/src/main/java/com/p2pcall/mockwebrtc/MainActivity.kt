package com.p2pcall.mockwebrtc

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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
    private lateinit var btnAnswerOffer: Button

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
        btnAnswerOffer = findViewById(R.id.btnAnswerOffer)

        initializeWebRTC()

        btnMute.setOnClickListener {
            isMuted = !isMuted
            localAudioTrack.setEnabled(!isMuted)
            btnMute.text = if (isMuted) "Unmute" else "Mute"
        }

        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnGenerateOffer.setOnClickListener {
            Log.d("afdasasf","createOffer click")

            createOffer()
           //setRemoteAnswer("h")

        }
        btnAnswerOffer.setOnClickListener {
           // createOffer()
            Log.d("afdasasf","btnAnswerOffer click")

            fetchAndSetRemoteSdp(true)
            //setRemoteAnswer("h")

        }
        btnEndCall.setOnClickListener { endCall() }
        // Initialize Views

    }

    private fun initializeWebRTC() {
        Log.d("afdasasf"," initializeWebRTC ")

        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val eglBaseContext = EglBase.create().eglBaseContext
        val peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .createPeerConnectionFactory()
        // Setup Video Capture
        videoCapturer = getVideoCapturer()
        Log.d("afdasasf"," onSignalingChange $videoCapturer ")
        val videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)

// 3️⃣ Create local video track
        val localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)

// 4️⃣ Start capturing video
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, this@MainActivity, videoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", audioSource)
     //   val localVideoView = findViewById<SurfaceViewRenderer>(R.id.localView)

        localView.init(eglBaseContext, null)
        localView.setMirror(true)
        localView.setEnableHardwareScaler(true)
        localView.setZOrderMediaOverlay(true)
        val mediaStream = peerConnectionFactory.createLocalMediaStream("localStream")
        mediaStream.addTrack(localVideoTrack)
        mediaStream.addTrack(localAudioTrack)

// Attach media stream to peer connection
        localVideoTrack.setEnabled(true) // Ensure video track is enabled

// 2️⃣ Attach video track to renderer
        val testSink = VideoSink { frame ->
            Log.d("WebRTC", "Received video frame: ${frame.buffer.width}x${frame.buffer.height}")
        }
        localVideoTrack.removeSink(localView) // Remove existing sink
        localVideoTrack.addSink(localView)



        // localView.init(EglBase.create().eglBaseContext, null)
       // remoteView.init(EglBase.create().eglBaseContext, null)

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
        localPeer.addStream(mediaStream)
        // Add Media Stream


    }

    private fun createOffer() {
        val constraints = MediaConstraints()

        localPeer.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
              //  val modifiedSdp = SessionDescription(sessionDescription!!.type, modifySdp(sessionDescription.description))
                localPeer.setLocalDescription(this, sessionDescription)
                Log.d("afdasasf","oncreate success ${Gson().toJson(sessionDescription)}")
                storeSessionDescription(sessionDescription!!, true)

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "SDP Offer Created", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.d("afdasasf","onCreateFailure localdescription $error")

            }
            override fun onSetSuccess() {
                Log.d("afdasasf","onSetSuccess localdescription ")

            }
            override fun onSetFailure(error: String?) {
                Log.d("afdasasf","onSetFailure localdescription $error")

            }
        }, constraints)
    }
    val database = Firebase.database.reference.child("calls").child("callId")

    fun storeSessionDescription(sdp: SessionDescription, isOffer: Boolean) {
        val sdpMap = mapOf(
            "type" to sdp.type.canonicalForm(), // "offer" or "answer"
            "description" to sdp.description
        )

        val sdpType = if (isOffer) "offer" else "answer"
        database.child(sdpType).setValue(sdpMap)
            .addOnSuccessListener { Log.d("WebRTC", "SDP stored in Firebase") }
            .addOnFailureListener { Log.e("WebRTC", "Failed to store SDP: ${it.message}") }
    }
    private fun getSessionDescription(): SessionDescription? {
        val sharedPreferences = this@MainActivity.getSharedPreferences("WebRTC_Prefs", Context.MODE_PRIVATE)
        val sdpType = sharedPreferences.getString("sdp_type", null)
        val sdpDescription = sharedPreferences.getString("sdp_description", null)

        return if (sdpType != null && sdpDescription != null) {
            val type = SessionDescription.Type.fromCanonicalForm(sdpType)
            SessionDescription(type, sdpDescription)
        } else {
            null
        }
    }
    private fun saveSessionDescription(sdp: SessionDescription, context: Context) {
        val sharedPreferences = context.getSharedPreferences("WebRTC_Prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("sdp_type", sdp.type.canonicalForm())
        editor.putString("sdp_description", sdp.description)
        editor.apply()

        Log.d("WebRTC", "SessionDescription saved successfully!")
    }
    fun fetchAndSetRemoteSdp(isOffer: Boolean) {
        val sdpType = if (isOffer) "offer" else "answer"

        database.child(sdpType).get().addOnSuccessListener { snapshot ->
            val data = snapshot.value as? Map<String, String> ?: return@addOnSuccessListener
            Log.d("afdasasf","fetch database ${Gson().toJson(data)}")

            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(data["type"] ?: "offer"),
                data["description"] ?: ""
            )
            localPeer.setRemoteDescription(object : SdpObserver {

                override fun onCreateFailure(error: String?) {
                    Log.d("afdasasf","oncreate onCreateFailure ${Gson().toJson(error)}")

                }

                override fun onCreateSuccess(p0: SessionDescription?) {
                    Log.d("afdasasf","oncreate onCreateSuccess ${Gson().toJson(p0)}")

                            }

                override fun onSetSuccess() {
                    Log.d("afdasasf","oncreate onSetSuccess ${Gson().toJson(sdp)}")

                }
                override fun onSetFailure(error: String?) {

                    Log.d("afdasasf","oncreate onSetFailure ${Gson().toJson(error)}")

                }
            }, sdp)
    /*        localPeer.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() { Log.d("WebRTC", "Remote SDP set successfully") }
                override fun onSetFailure(error: String?) { Log.e("WebRTC", "Failed to set remote SDP: $error") }
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, sdp)*/
        }.addOnFailureListener {
            Log.e("afdasasf", "Failed to fetch SDP: ${it.message}")
        }
    }
    private fun setRemoteAnswer(sdp: String) {
        Log.d("WebRTCAA", "A")


        val sessionDescriptionString = getSessionDescription()
        Log.d("WebRTCAA", "A Session Description: ${Gson().toJson(sessionDescriptionString)}")

        if (localPeer == null) {
            Log.e("WebRTCAA", "PeerConnection is NULL. Cannot set remote answer.")
            return
        }

        if (localPeer.connectionState() == PeerConnection.PeerConnectionState.CLOSED) {
            Log.e("WebRTCAA", "PeerConnection is CLOSED. Cannot set remote answer.")
            return
        }
       // val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sessionDescriptionString)
        Log.d("WebRTCAA", "A Session Description: ${Gson().toJson(sessionDescriptionString)}")

         //   Log.d("WebRTCAA", "B Session Description: ${Gson().toJson(sessionDescription)}")

            localPeer.setRemoteDescription(object : SdpObserver {

                override fun onCreateFailure(error: String?) {
                    Log.d("afdasasf","oncreate onCreateFailure ${Gson().toJson(error)}")

                }

                override fun onCreateSuccess(p0: SessionDescription?) {
                    TODO("Not yet implemented")
                }

                override fun onSetSuccess() {
                    Log.d("afdasasf","oncreate onSetSuccess ${Gson().toJson(sessionDescriptionString)}")

                }
                override fun onSetFailure(error: String?) {

                    Log.d("afdasasf","oncreate onSetFailure ${Gson().toJson(error)}")

                }
            }, getSessionDescription())
    }



    private fun getVideoCapturer(): VideoCapturer {
        val cameraEnumerator = Camera2Enumerator(this@MainActivity)
        for (deviceName in cameraEnumerator.deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) { // Use front camera
                val videoCapturer = cameraEnumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) return videoCapturer
            }
        }
        throw RuntimeException("No suitable camera found!")
    }

    private fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    private fun endCall() {
        localPeer.close()
        finish()
    }
    fun modifySdp(sdp: String): String {
        val lines = sdp.split("\n").toMutableList()
        val modifiedSdp = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("a=recvonly") -> modifiedSdp.append("a=sendrecv\n") // Change recvonly to sendrecv
                line.startsWith("a=sendonly") -> modifiedSdp.append("a=sendrecv\n") // Change sendonly to sendrecv
                else -> modifiedSdp.append(line).append("\n")
            }
        }

        // Example: Force VP8 as the preferred codec (modify as needed)
        return prioritizeCodec(modifiedSdp.toString(), "VP8")
    }

    // Function to prioritize a specific codec (e.g., VP8)
    fun prioritizeCodec(sdp: String, codec: String): String {
        val lines = sdp.split("\n").toMutableList()
        val codecLineIndex = lines.indexOfFirst { it.contains("a=rtpmap") && it.contains(codec) }

        if (codecLineIndex != -1) {
            val codecPayload = lines[codecLineIndex].split(" ")[0].substringAfter(":") // Extract codec payload number
            val mVideoIndex = lines.indexOfFirst { it.startsWith("m=video") }

            if (mVideoIndex != -1) {
                val mLineParts = lines[mVideoIndex].split(" ").toMutableList()
                // Reorder codecs: prioritize selected codec
                val newMLine = buildString {
                    append(mLineParts[0]).append(" ") // Keep "m=video"
                    append(mLineParts[1]).append(" ") // Keep port
                    append(mLineParts[2]).append(" ") // Keep protocol
                    append(codecPayload).append(" ") // Prioritize selected codec
                    append(mLineParts.drop(3).filter { it != codecPayload }.joinToString(" ")) // Append remaining codecs
                }
                lines[mVideoIndex] = newMLine
            }
        }

        return lines.joinToString("\n")
    }
}


