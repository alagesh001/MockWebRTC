package com.p2pcall.mockwebrtc

import android.util.Log
import com.google.firebase.database.*
import org.webrtc.IceCandidate

class FirebaseSignaling {

    private val database = FirebaseDatabase.getInstance().reference.child("calls")

    fun sendOffer(offer: String) {
        Log.e("dafaadf","Yes send ${FirebaseDatabase.getInstance().reference}")
        database.child("offer").setValue(offer)
      /*  database.child("1234").setValue(mapOf(
            "offer" to offer,
            "answer" to "aa",
            "candidates" to mapOf("caller" to listOf<Any>(), "callee" to listOf<Any>())
        ))*/
    }

    fun sendAnswer(answer: String) {
        Log.e("dafaadf","Yes answer")

        database.child("answer").setValue(answer)
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        database.child("candidates").push().setValue(candidate)
    }

    fun listenForOffer(callback: (String) -> Unit) {
        database.child("offer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.value?.let { callback(it.toString()) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForAnswer(callback: (String) -> Unit) {
        database.child("answer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.value?.let { callback(it.toString()) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForIceCandidates(callback: (IceCandidate) -> Unit) {
        database.child("candidates").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue(IceCandidate::class.java)?.let { callback(it) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
