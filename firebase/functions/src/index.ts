import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

initializeApp();

const db = getFirestore();
const auth = getAuth();
const messaging = getMessaging();

function requireAdmin(request: { auth?: { token?: Record<string, unknown>; uid?: string } | null }) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  if (request.auth.token?.admin !== true) {
    throw new HttpsError("permission-denied", "Admin privileges required.");
  }
}

export const setUserActiveState = onCall(async (request) => {
  requireAdmin(request);
  const uid = String(request.data?.uid ?? "");
  const active = Boolean(request.data?.active);
  if (!uid) {
    throw new HttpsError("invalid-argument", "uid is required");
  }

  await db.collection("users").doc(uid).set(
    {
      isActive: active,
      updatedAt: Timestamp.now(),
      updatedBy: request.auth?.uid ?? "unknown"
    },
    { merge: true }
  );

  return { ok: true };
});

export const setUserRole = onCall(async (request) => {
  requireAdmin(request);
  const uid = String(request.data?.uid ?? "");
  const role = String(request.data?.role ?? "");
  if (!uid || (role !== "admin" && role !== "user")) {
    throw new HttpsError("invalid-argument", "uid and valid role are required");
  }

  await auth.setCustomUserClaims(uid, { admin: role === "admin" });
  await db.collection("users").doc(uid).set(
    {
      role,
      updatedAt: Timestamp.now(),
      updatedBy: request.auth?.uid ?? "unknown"
    },
    { merge: true }
  );

  return { ok: true };
});

export const moderateReport = onCall(async (request) => {
  requireAdmin(request);
  const reportId = String(request.data?.reportId ?? "");
  const state = String(request.data?.state ?? "");
  if (!reportId || !["open", "in_review", "closed"].includes(state)) {
    throw new HttpsError("invalid-argument", "reportId and valid state are required");
  }

  await db.collection("reports").doc(reportId).set(
    {
      state,
      moderatedBy: request.auth?.uid ?? "unknown",
      moderatedAt: Timestamp.now()
    },
    { merge: true }
  );
  return { ok: true };
});

export const onMessageCreated = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const conversationId = event.params.conversationId;
    const data = event.data?.data();
    if (!data) return;

    const senderId = String(data.senderId ?? "");
    const text = String(data.text ?? "");
    if (!senderId) return;

    const conversationSnap = await db.collection("conversations").doc(conversationId).get();
    const participants = (conversationSnap.get("participantIds") as string[] | undefined) ?? [];
    const targetUids = participants.filter((uid) => uid !== senderId);
    if (targetUids.length === 0) return;

    const usersSnapshot = await db.collection("users")
      .where("uid", "in", targetUids.slice(0, 10))
      .get();

    const tokens: string[] = [];
    usersSnapshot.docs.forEach((doc) => {
      const userTokens = (doc.get("fcmTokens") as string[] | undefined) ?? [];
      userTokens.forEach((t) => tokens.push(t));
    });

    if (tokens.length === 0) return;
    await messaging.sendEachForMulticast({
      tokens,
      notification: {
        title: "Nuevo mensaje",
        body: text.slice(0, 140)
      },
      data: {
        conversationId
      }
    });
  }
);

export const onUserCreated = onCall(async (request) => {
  // Optional bootstrap helper for scripts/backoffice.
  const uid = String(request.data?.uid ?? "");
  const email = String(request.data?.email ?? "");
  const displayName = String(request.data?.displayName ?? "");
  if (!uid || !email) {
    throw new HttpsError("invalid-argument", "uid and email are required");
  }
  await db.collection("users").doc(uid).set(
    {
      uid,
      email,
      displayName,
      role: "user",
      isActive: true,
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now()
    },
    { merge: true }
  );
  return { ok: true };
});
