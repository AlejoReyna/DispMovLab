package com.example.chat.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Firebase Firestore–based WebRTC signaling.
 *
 * Mirrors the Firebase WebRTC codelab (https://webrtc.org/getting-started/firebase-rtc-codelab)
 * but uses Firestore instead of Realtime Database.
 *
 * Firestore schema:
 *
 *  videoRooms/{conversationId}
 *    offer   : { type, sdp }
 *    answer  : { type, sdp }
 *    callerId: String
 *    status  : "calling" | "connected" | "ended"
 *    createdAt
 *
 *  videoRooms/{conversationId}/callerCandidates/{id}
 *    candidate, sdpMid, sdpMLineIndex
 *
 *  videoRooms/{conversationId}/calleeCandidates/{id}
 *    candidate, sdpMid, sdpMLineIndex
 */
class VideoCallSignalingDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    // ── Offer ─────────────────────────────────────────────────────────────────

    /** Stores an SDP offer and marks this device as the caller. */
    suspend fun storeOffer(conversationId: String, sdp: SessionDescription) {
        firestore.collection("videoRooms").document(conversationId)
            .set(
                mapOf(
                    "offer" to mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description),
                    "callerId" to (auth.currentUser?.uid ?: ""),
                    "status" to "calling",
                    "createdAt" to Timestamp.now()
                )
            ).await()
    }

    /**
     * Returns the active SDP offer for this conversation, or null if none exists
     * (meaning we are the caller and should create one).
     */
    suspend fun getOffer(conversationId: String): SessionDescription? {
        val doc = firestore.collection("videoRooms").document(conversationId)
            .get().await()
        if (!doc.exists()) return null
        val status = doc.getString("status") ?: return null
        if (status == "ended") return null          // stale room
        val offerMap = doc.get("offer") as? Map<*, *> ?: return null
        val type = offerMap["type"] as? String ?: return null
        val sdpStr = offerMap["sdp"] as? String ?: return null
        return SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdpStr)
    }

    // ── Answer ────────────────────────────────────────────────────────────────

    suspend fun storeAnswer(conversationId: String, sdp: SessionDescription) {
        firestore.collection("videoRooms").document(conversationId)
            .update(
                mapOf(
                    "answer" to mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description),
                    "status" to "connected"
                )
            ).await()
    }

    /**
     * Emits the remote SDP answer once it appears in Firestore (caller side).
     * Suspends until the answer is available or the flow is cancelled.
     */
    suspend fun awaitAnswer(conversationId: String): SessionDescription {
        return listenForAnswer(conversationId)
            .filter { it != null }
            .first()!!
    }

    private fun listenForAnswer(conversationId: String): Flow<SessionDescription?> = callbackFlow {
        val listener = firestore.collection("videoRooms").document(conversationId)
            .addSnapshotListener { doc, _ ->
                val answerMap = doc?.get("answer") as? Map<*, *> ?: return@addSnapshotListener
                val type = answerMap["type"] as? String ?: return@addSnapshotListener
                val sdpStr = answerMap["sdp"] as? String ?: return@addSnapshotListener
                trySend(SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdpStr))
            }
        awaitClose { listener.remove() }
    }

    // ── ICE candidates ────────────────────────────────────────────────────────

    /**
     * Stores an ICE candidate in the appropriate sub-collection.
     * @param isCaller pass true when the local device created the offer.
     */
    suspend fun addIceCandidate(
        conversationId: String,
        candidate: IceCandidate,
        isCaller: Boolean
    ) {
        val collection = if (isCaller) "callerCandidates" else "calleeCandidates"
        firestore.collection("videoRooms").document(conversationId)
            .collection(collection)
            .add(
                mapOf(
                    "candidate" to candidate.sdp,
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex
                )
            ).await()
    }

    /**
     * Emits remote ICE candidates as they are written by the other peer.
     * The caller listens on "calleeCandidates" and vice-versa.
     */
    fun listenForIceCandidates(conversationId: String, isCaller: Boolean): Flow<IceCandidate> =
        callbackFlow {
            val collection = if (isCaller) "calleeCandidates" else "callerCandidates"
            val listener = firestore.collection("videoRooms").document(conversationId)
                .collection(collection)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val sdp = doc.getString("candidate") ?: return@forEach
                            val sdpMid = doc.getString("sdpMid") ?: return@forEach
                            val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: return@forEach
                            trySend(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                        }
                    }
                }
            awaitClose { listener.remove() }
        }

    // ── Room lifecycle ────────────────────────────────────────────────────────

    suspend fun endCall(conversationId: String) {
        try {
            firestore.collection("videoRooms").document(conversationId)
                .update(mapOf("status" to "ended"))
                .await()
        } catch (_: Exception) { /* Room may not exist yet — ignore */ }
    }
}
