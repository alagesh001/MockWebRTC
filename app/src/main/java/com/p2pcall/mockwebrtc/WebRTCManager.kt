package com.p2pcall.mockwebrtc

import android.content.Context
import org.webrtc.*

class WebRTCManager(private val context: Context) {

    private val eglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null

    init {
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        startLocalMedia()
    }

    private fun startLocalMedia() {
        val videoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer = createCameraCapturer()
        videoCapturer?.initialize(
            SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext),
            context,
            videoSource.capturerObserver
        )
        videoCapturer?.startCapture(1024, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", videoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio",
            peerConnectionFactory.createAudioSource(MediaConstraints()))
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack
    fun getLocalAudioTrack(): AudioTrack? = localAudioTrack
}
