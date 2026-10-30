package com.example.unilink.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.unilink.databinding.ActivityVideoCallBinding
import com.example.unilink.webrtc.WebRtcClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.*

class VideoCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoCallBinding
    private var webRtcClient: WebRtcClient? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var channelId = ""
    private var isOffer = false

    private val sdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            webRtcClient?.setLocalDescription(sdp, this)
            val data = hashMapOf(
                "type" to sdp.type.canonicalForm(),
                "description" to sdp.description,
                "senderId" to auth.currentUser?.uid
            )
            // Use set with merge to avoid overwriting the whole document if both write
            db.collection("calls").document(channelId).set(data, com.google.firebase.firestore.SetOptions.merge())
        }

        override fun onSetSuccess() {
            Log.d("WebRTC", "SDP Set Success")
        }

        override fun onCreateFailure(s: String?) {}
        override fun onSetFailure(s: String?) {}
    }

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            val candidateData = hashMapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "candidate" to candidate.sdp,
                "senderId" to auth.currentUser?.uid
            )
            db.collection("calls").document(channelId)
                .collection("candidates").add(candidateData)
        }

        override fun onAddStream(stream: MediaStream) {
            Log.d("WebRTC", "Remote stream added: ${stream.id}")
            if (stream.videoTracks.isNotEmpty()) {
                runOnUiThread {
                    stream.videoTracks[0].addSink(binding.remoteVideoView)
                }
            }
        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            Log.d("WebRTC", "ICE Connection Change: $p0")
            if (p0 == PeerConnection.IceConnectionState.DISCONNECTED || 
                p0 == PeerConnection.IceConnectionState.FAILED ||
                p0 == PeerConnection.IceConnectionState.CLOSED) {
                runOnUiThread {
                    Toast.makeText(this@VideoCallActivity, "Call ended", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelId = intent.getStringExtra("channelId") ?: ""
        isOffer = intent.getBooleanExtra("isOffer", false)

        if (checkPermissions()) {
            initWebRtc()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 101)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnEndCall.setOnClickListener { finish() }
        binding.btnMute.setOnClickListener {
            webRtcClient?.toggleAudio()
            val isMuted = webRtcClient?.isAudioMuted() ?: false
            binding.btnMute.setImageResource(if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now)
            Toast.makeText(this, if (isMuted) "Audio Muted" else "Audio Unmuted", Toast.LENGTH_SHORT).show()
        }
        binding.btnSwitchCamera.setOnClickListener {
            webRtcClient?.switchCamera()
        }
    }

    private fun initWebRtc() {
        webRtcClient = WebRtcClient(this, peerConnectionObserver)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        webRtcClient?.createPeerConnection(iceServers)
        
        // Initialize remote view
        binding.remoteVideoView.init(webRtcClient?.getEglContext(), null)
        binding.remoteVideoView.setEnableHardwareScaler(true)

        webRtcClient?.startLocalVideo(binding.localVideoView)

        if (isOffer) {
            webRtcClient?.createOffer(sdpObserver)
        }
        
        listenForRemoteSdp()
        listenForIceCandidates()
    }

    private fun listenForRemoteSdp() {
        db.collection("calls").document(channelId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val type = snapshot.getString("type")
                val description = snapshot.getString("description")
                val senderId = snapshot.getString("senderId")

                // Don't process our own SDP
                if (senderId == auth.currentUser?.uid) return@addSnapshotListener
                
                if (isOffer && type == "answer") {
                    val sdp = SessionDescription(SessionDescription.Type.ANSWER, description)
                    webRtcClient?.setRemoteDescription(sdp, sdpObserver)
                } else if (!isOffer && type == "offer") {
                    val sdp = SessionDescription(SessionDescription.Type.OFFER, description)
                    webRtcClient?.setRemoteDescription(sdp, sdpObserver)
                    webRtcClient?.createAnswer(sdpObserver)
                }
            }
        }
    }

    private fun listenForIceCandidates() {
        db.collection("calls").document(channelId)
            .collection("candidates").addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val senderId = data["senderId"] as? String
                        
                        // ONLY add candidates sent by the other user
                        if (senderId != null && senderId != auth.currentUser?.uid) {
                            val candidate = IceCandidate(
                                data["sdpMid"] as String,
                                (data["sdpMLineIndex"] as Long).toInt(),
                                data["candidate"] as String
                            )
                            webRtcClient?.addIceCandidate(candidate)
                        }
                    }
                }
            }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initWebRtc()
            } else {
                Toast.makeText(this, "Camera and Microphone permissions are required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            webRtcClient?.close()
            // Only host deletes the root doc to avoid race conditions
            if (isOffer) {
                db.collection("calls").document(channelId).delete()
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Error in onDestroy", e)
        }
    }
}
