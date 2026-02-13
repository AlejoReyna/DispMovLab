# Plan de pruebas

## Objetivos

- Verificar autenticacion, mensajeria y administracion por rol.
- Validar que reglas de seguridad bloquean accesos indebidos.
- Confirmar eventos de notificacion y moderacion.

## Pruebas funcionales (Android)

- Auth:
  - registro nuevo usuario
  - login correcto/incorrecto
  - cierre de sesion
- Mensajeria:
  - crear conversacion directa
  - enviar/recibir mensajes entre dos usuarios
  - actualizar lectura (`lastReadAt`)
- Admin:
  - visibilidad del boton/pantalla admin solo para admins
  - activar/desactivar usuario
  - promover/quitar rol admin
- Reportes:
  - crear reporte por usuario
  - visibilidad de reportes abiertos para admin

## Pruebas de seguridad (Rules + Functions)

- Firestore Rules:
  - usuario no participante no puede leer mensajes de otra conversacion
  - usuario normal no puede modificar rol/isActive de otros
  - usuario normal no puede actualizar reportes ajenos
- Storage Rules:
  - denegar upload sin auth
  - denegar archivo >10MB
  - denegar tipo MIME no permitido
- Functions:
  - callable admin rechaza `unauthenticated` y `permission-denied`
  - `setUserRole` valida payload y persiste claim + documento

## Escenarios de abuso

- Flood de mensajes en poco tiempo.
- Reportes falsos masivos.
- Reuso de tokens FCM revocados.
- Intentos de invocar callables admin desde usuario normal.

## Herramientas sugeridas

- Firebase Emulator Suite para reglas/functions.
- JUnit + coroutines test para capa domain/data.
- UI tests con Compose Test para auth/chat/admin.
