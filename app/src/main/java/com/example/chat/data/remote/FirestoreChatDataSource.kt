package com.example.chat.data.remote

import com.example.chat.domain.model.Conversation
import com.example.chat.domain.model.ConversationType
import com.example.chat.domain.model.Message
import com.example.chat.domain.model.MessageStatus
import com.example.chat.domain.model.ModerationReport
import com.example.chat.domain.model.ReportState
import com.example.chat.domain.model.UserProfile
import com.example.chat.domain.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreChatDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    fun observeConversations(limit: Long): Flow<List<Conversation>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val type = if (doc.getString("type") == "group") ConversationType.GROUP else ConversationType.DIRECT
                    // Per-user title map: { uid → name that uid sees }
                    @Suppress("UNCHECKED_CAST")
                    val titlesMap = doc.get("titles") as? Map<String, String>
                    val title = titlesMap?.get(uid) ?: doc.getString("title").orEmpty()
                    Conversation(
                        id = doc.id,
                        title = title,
                        type = type,
                        lastMessagePreview = doc.getString("lastMessagePreview"),
                        updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String, limit: Long): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val status = when (doc.getString("status")) {
                        "delivered" -> MessageStatus.DELIVERED
                        "read" -> MessageStatus.READ
                        "failed" -> MessageStatus.FAILED
                        else -> MessageStatus.SENT
                    }
                    Message(
                        id = doc.id,
                        conversationId = conversationId,
                        senderId = doc.getString("senderId").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        status = status
                    )
                }.sortedBy { it.createdAtMillis }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(conversationId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        val messageId = UUID.randomUUID().toString()
        val conversationRef = firestore.collection("conversations").document(conversationId)
        val messageRef = conversationRef.collection("messages").document(messageId)
        val now = Timestamp.now()
        firestore.runBatch { batch ->
            batch.set(
                messageRef,
                mapOf(
                    "senderId" to senderId,
                    "text" to text,
                    "status" to "sent",
                    "createdAt" to now
                )
            )
            batch.update(
                conversationRef,
                mapOf(
                    "lastMessagePreview" to text.take(120),
                    "lastMessageAt" to now,
                    "updatedAt" to now
                )
            )
        }.await()
    }

    suspend fun createDirectConversation(otherUid: String): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")

        // Query with BOTH participantIds (array-contains) AND participantKey so that:
        //  a) Firestore security rules are satisfied — the list rule requires
        //     request.auth.uid in resource.data.participantIds, and whereArrayContains
        //     guarantees exactly that for every returned document.
        //  b) The composite index (participantIds CONTAINS + participantKey ASC) handles
        //     the query efficiently.
        val participantKey = listOf(uid, otherUid).sorted().joinToString("_")
        val existing = firestore.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .whereEqualTo("participantKey", participantKey)
            .limit(1)
            .get()
            .await()
        if (!existing.isEmpty) return existing.documents.first().id

        // Use document-ID lookups (no index required) to get display names.
        val myDoc = firestore.collection("users").document(uid).get().await()
        val myName = myDoc.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: myDoc.getString("email").orEmpty()

        val otherDoc = firestore.collection("users").document(otherUid).get().await()
        val otherName = otherDoc.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: otherDoc.getString("email").orEmpty()

        val ref = firestore.collection("conversations").document()
        val now = Timestamp.now()
        ref.set(
            mapOf(
                "type" to "direct",
                // Legacy title for backwards compatibility
                "title" to otherName.ifBlank { "Direct chat" },
                // Per-user titles: each participant sees the OTHER person's name
                "titles" to mapOf(uid to otherName.ifBlank { "Chat" }, otherUid to myName.ifBlank { "Chat" }),
                "participantIds" to listOf(uid, otherUid),
                "participantKey" to participantKey,
                "lastMessagePreview" to "",
                "lastMessageAt" to now,
                "updatedAt" to now,
                "createdAt" to now
            )
        ).await()

        val participants = ref.collection("participants")
        participants.document(uid).set(
            mapOf(
                "uid" to uid,
                "lastReadAt" to now,
                "mute" to false
            )
        ).await()
        participants.document(otherUid).set(
            mapOf(
                "uid" to otherUid,
                "lastReadAt" to now,
                "mute" to false
            )
        ).await()
        return ref.id
    }

    /** Returns the title this user should see for the given conversation. */
    suspend fun getConversationTitle(conversationId: String): String {
        val uid = auth.currentUser?.uid.orEmpty()
        val doc = firestore.collection("conversations").document(conversationId).get().await()
        @Suppress("UNCHECKED_CAST")
        val titlesMap = doc.get("titles") as? Map<String, String>
        return titlesMap?.get(uid)
            ?: doc.getString("title").orEmpty()
    }

    suspend fun markAsRead(conversationId: String, messageId: String) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        val participantRef = firestore.collection("conversations")
            .document(conversationId)
            .collection("participants")
            .document(uid)
        participantRef.update(
            mapOf(
                "lastReadAt" to Timestamp.now(),
                "lastReadMessageId" to messageId
            )
        ).await()
    }

    suspend fun searchUserByPhone(phoneNumber: String): UserProfile? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("phone", phoneNumber)
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        val uid = doc.getString("uid") ?: return null
        val role = if (doc.getString("role") == "admin") UserRole.ADMIN else UserRole.USER
        return UserProfile(
            uid = uid,
            email = doc.getString("email").orEmpty(),
            displayName = doc.getString("displayName").orEmpty(),
            phone = doc.getString("phone").orEmpty(),
            role = role,
            isActive = doc.getBoolean("isActive") ?: true
        )
    }

    suspend fun getUsers(limit: Long): List<UserProfile> {
        val snapshot = firestore.collection("users")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: return@mapNotNull null
            val role = if (doc.getString("role") == "admin") UserRole.ADMIN else UserRole.USER
            UserProfile(
                uid = uid,
                email = doc.getString("email").orEmpty(),
                displayName = doc.getString("displayName").orEmpty(),
                phone = doc.getString("phone").orEmpty(),
                role = role,
                isActive = doc.getBoolean("isActive") ?: true
            )
        }
    }

    suspend fun updateUserActiveState(uid: String, active: Boolean) {
        firestore.collection("users").document(uid)
            .update(mapOf("isActive" to active, "updatedAt" to Timestamp.now()))
            .await()
    }

    suspend fun updateUserRole(uid: String, role: UserRole) {
        firestore.collection("users").document(uid)
            .update(mapOf("role" to if (role == UserRole.ADMIN) "admin" else "user", "updatedAt" to Timestamp.now()))
            .await()
    }

    suspend fun createReport(conversationId: String, messageId: String, reason: String) {
        val reporterUid = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        firestore.collection("reports").add(
            mapOf(
                "reporterUid" to reporterUid,
                "conversationId" to conversationId,
                "messageId" to messageId,
                "reason" to reason,
                "state" to "open",
                "createdAt" to Timestamp.now()
            )
        ).await()
    }

    fun observeOpenReports(limit: Long): Flow<List<ModerationReport>> = callbackFlow {
        val listener = firestore.collection("reports")
            .whereIn("state", listOf("open", "in_review"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    ModerationReport(
                        id = doc.id,
                        reporterUid = doc.getString("reporterUid").orEmpty(),
                        conversationId = doc.getString("conversationId").orEmpty(),
                        messageId = doc.getString("messageId").orEmpty(),
                        reason = doc.getString("reason").orEmpty(),
                        state = when (doc.getString("state")) {
                            "in_review" -> ReportState.IN_REVIEW
                            "closed" -> ReportState.CLOSED
                            else -> ReportState.OPEN
                        }
                    )
                }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }
}
