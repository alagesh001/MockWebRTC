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
import org.webrtc.PeerConnection.IceConnectionState
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
            iamoffer = true
            createOffer()
           //setRemoteAnswer("h")

        }
        btnAnswerOffer.setOnClickListener {
           // createOffer()
            Log.d("afdasasf","btnAnswerOffer click")
            iamoffer = false
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


        remoteView.init(eglBaseContext, null)
        remoteView.setMirror(false)
        remoteView.setEnableHardwareScaler(true)


        // localView.init(EglBase.create().eglBaseContext, null)
       // remoteView.init(EglBase.create().eglBaseContext, null)

        // Create Peer Connection
        val iceServers = listOf(IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        Log.d("afdasasf"," iceServers $iceServers")

        localPeer = peerConnectionFactory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Log.d("afdasasf"," onSignalingChange ${Gson().toJson(p0)} ")

            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                transceiver?.receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        Log.d("WebRTCdsf", "✅ Remote video track received!")
                        track.addSink(remoteView) // Attach track to SurfaceViewRenderer
                    }
                }
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
                Log.d("afdasasf","BBB iamoffer $iamoffer")

                Log.d("afdasasf"," onIceCandidate $iamoffer")
               // localPeer.addIceCandidate(candidate)
                storeIceCandidate(candidate!!,iamoffer)
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
    private var iamoffer = true
    fun storeIceCandidate(candidate: IceCandidate,isOffer: Boolean) {
        val cantype = if (isOffer) "offerCandidate" else "remoteCandidate"

        val candidateMap = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )

        database.child(cantype).push().setValue(candidateMap)
    }
    private fun createOffer() {
        val constraints = MediaConstraints()
        Log.d("afdasasf","AAA iamoffer $iamoffer")

        localPeer.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
              //  val modifiedSdp = SessionDescription(sessionDescription!!.type, modifySdp(sessionDescription.description))
                localPeer.setLocalDescription(this, sessionDescription)
                Log.d("afdasasf","oncreate success ${Gson().toJson(sessionDescription)}")
                storeSessionDescription(sessionDescription!!, true)
                fetchAndSetRemoteSdp(false)
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
        database.child("answer").setValue("")

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
        Log.d("afdasasfAA","fetchAndSetRemoteSdp setupppp")
        database.child(sdpType).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("afdasasfAA","fetchAndSetRemoteSdp setupppp")

                if (snapshot.exists()) {

                    val data = snapshot.value as? Map<String, String> ?: null

                    val sdp = SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(data?.get("type") ?: "offer"),
                        data?.get("description") ?: ""
                    )
                    localPeer.setRemoteDescription(object : SdpObserver {

                        override fun onCreateFailure(error: String?) {
                            Log.d("afdasasf", "oncreate onCreateFailure ${Gson().toJson(error)}")

                        }

                        override fun onCreateSuccess(p0: SessionDescription?) {
                            Log.d(
                                "afdasasf", "oncreate onCreateSuccess ${Gson().toJson(p0)}"

                            )

                        }

                        override fun onSetSuccess() {
                            Log.d("afdasasf", "oncreate $sdpType  onSetSuccess ${Gson().toJson(sdp)}")
                            if(isOffer) {
                                localPeer.createAnswer(object : SdpObserver {
                                    override fun onCreateSuccess(answer: SessionDescription?) {
                                        Log.d("afdasasf", "✅ Created answer SDP.")
                                        localPeer.setLocalDescription(this, answer)
                                        storeSessionDescription(answer!!, false)
                                        // Send the answer to Firebase
                                        // sendSessionDescriptionToFirebase(answer)
                                    }

                                    override fun onCreateFailure(error: String?) {
                                        Log.e("WebRTC", "❌ Failed to create answer: $error")
                                    }

                                    override fun onSetSuccess() {}
                                    override fun onSetFailure(error: String?) {}
                                }, MediaConstraints())
                            }
                            localPeer.iceConnectionState().let { state ->
                                Log.d("WebRTsaC", "ICE Connection State: $state")

                                when (state) {
                                    IceConnectionState.CONNECTED -> {
                                        Log.d("WebRTsaC", "ICE Connection Successful! 🔥")

                                    }

                                    IceConnectionState.FAILED -> {
                                        Log.e("WebRTsaC", "ICE Connection Failed! ❌")
                                    }

                                    IceConnectionState.CHECKING -> {
                                        Log.d("WebRTsaC", "Checking ICE Connection... ⏳")
                                    }

                                    IceConnectionState.DISCONNECTED -> {
                                        Log.w("WebRTsaC", "ICE Disconnected! 🔄")
                                    }

                                    else -> Log.d("WebRTsaC", "ICE State: $state")
                                }
                            }
                            fetchIceCandidates()
                        }

                        override fun onSetFailure(error: String?) {

                            Log.d("afdasasf", "oncreate onSetFailure ${Gson().toJson(error)}")

                        }
                    }, sdp)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "❌ Database error: ${error.message}")
            }
        })

    }
    fun fetchIceCandidates() {
        val cantype = if (iamoffer) "remoteCandidate" else "offerCandidate"

        database.child(cantype).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val data = snapshot.value as? Map<String, Any> ?: return
                Log.d("afdasasf","$cantype  ${Gson().toJson(data)}")

                val candidate = IceCandidate(
                    data["sdpMid"] as String?,
                    (data["sdpMLineIndex"] as Long).toInt(),
                    data["candidate"] as String
                )
                localPeer.addIceCandidate(candidate)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
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


