# Seguridad y hardening

## Cifrado

- En transito: TLS (gestionado por Firebase SDK).
- En reposo: cifrado gestionado por Firebase.
- No se implementa E2EE en esta version.

## Controles clave

- Auth obligatorio para acceso a datos.
- Regla de minimo privilegio en Firestore y Storage.
- Operaciones sensibles de admin solo por Cloud Functions:
  - `setUserActiveState`
  - `setUserRole`
  - `moderateReport`
- Verificacion de rol admin con custom claim `admin`.
- Persistencia de rol en `users/{uid}` para visibilidad funcional.

## Reglas Firestore

- `users`:
  - usuario lee/escribe su perfil limitado
  - admin puede gestionar usuarios completos
- `conversations`:
  - lectura solo participantes (o admin)
  - escritura de metadata limitada a participantes
- `messages`:
  - crear solo si emisor es usuario autenticado participante
  - texto validado por tamano
- `reports`:
  - creacion por usuario autenticado
  - moderacion solo admin

## Reglas Storage

- uploads autenticados con limite de 10MB.
- tipos de contenido permitidos: imagen/video/pdf.
- borrado solo admin.

## Endurecimiento recomendado siguiente fase

- App Check (Play Integrity / reCAPTCHA Enterprise).
- Rotacion y saneamiento de `fcmTokens`.
- Rate limiting en Cloud Functions (por uid e IP).
- Auditoria centralizada de acciones admin en coleccion `adminAudit`.
- Deteccion de abuso (spam reporting / flood messages).
