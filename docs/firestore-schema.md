# Firestore schema y consultas

## Colecciones

### `users/{uid}`

Campos principales:
- `uid`: string
- `email`: string
- `displayName`: string
- `role`: `"user" | "admin"`
- `isActive`: boolean
- `fcmTokens`: string[] (opcional)
- `createdAt`, `updatedAt`: timestamp

### `conversations/{conversationId}`

Campos principales:
- `type`: `"direct" | "group"`
- `title`: string
- `participantIds`: string[]
- `participantKey`: string (solo direct, ordenado para idempotencia)
- `lastMessagePreview`: string
- `lastMessageAt`, `updatedAt`, `createdAt`: timestamp

Subcolecciones:

#### `conversations/{conversationId}/participants/{uid}`
- `uid`: string
- `lastReadAt`: timestamp
- `lastReadMessageId`: string (opcional)
- `mute`: boolean

#### `conversations/{conversationId}/messages/{messageId}`
- `senderId`: string
- `text`: string
- `status`: `"sent" | "delivered" | "read" | "failed"`
- `createdAt`: timestamp

### `reports/{reportId}`

Campos:
- `reporterUid`: string
- `conversationId`: string
- `messageId`: string
- `reason`: string
- `state`: `"open" | "in_review" | "closed"`
- `createdAt`: timestamp
- `moderatedBy`, `moderatedAt`: opcionales

## Estrategia de lectura y paginacion

- Conversaciones:
  - query: `whereArrayContains(participantIds, uid)` + `orderBy(updatedAt desc)` + `limit(30)`.
- Mensajes:
  - query: `orderBy(createdAt desc)` + `limit(50)` para ventana reciente.
  - UI invierte a orden ascendente para render natural.
- Reportes:
  - query admin: `whereIn(state, ["open","in_review"])` + `orderBy(createdAt desc)` + `limit(50)`.
- Usuarios:
  - query admin: `orderBy(updatedAt desc)` + `limit(50)`.

## Indices

Definidos en `firebase/firestore.indexes.json` para:
- conversaciones por participante + `updatedAt`
- conversaciones directas por `type + participantKey`
- mensajes por `createdAt`
- reportes por `state + createdAt`
- usuarios por `updatedAt`
