# DispMov Chat

Aplicacion Android de mensajeria en Kotlin con backend Firebase, con soporte de autenticacion, conversaciones en tiempo real, panel admin en app, reportes y notificaciones push.

## Estructura

- `app/`: cliente Android (Kotlin + Jetpack Compose)
- `firebase/`: reglas, indices y Cloud Functions
- `docs/`: arquitectura, esquema de datos y estrategia de pruebas

## Funcionalidades implementadas

- Auth con Firebase (`email/password`), registro y cierre de sesion.
- Modelo por capas en Android:
  - `presentation`: pantallas, navegacion, ViewModels
  - `domain`: modelos y contratos de repositorio
  - `data`: integracion con Firebase Auth/Firestore/Functions
  - `core`: wiring base de dependencias
- Mensajeria:
  - listado de conversaciones y sala de chat en tiempo real
  - envio de mensajes y actualizacion de metadata de conversacion
- Administracion en app:
  - listado de usuarios
  - activar/desactivar usuarios
  - promover/quitar rol admin (via Cloud Functions)
- Reportes:
  - coleccion `reports` preparada para moderacion
- Seguridad:
  - reglas Firestore y Storage con principio de minimo privilegio
  - operaciones sensibles delegadas a Cloud Functions

## Setup rapido

1. Crear proyecto Firebase.
2. Copiar `app/google-services.template.json` a `app/google-services.json` y completar datos reales.
3. Configurar `firebase/.firebaserc` con el `project_id`.
4. En `firebase/functions`:
   - `npm install`
   - `npm run build`
5. Desplegar backend:
   - desde `firebase/`: `firebase deploy`

## Documentacion adicional

- `docs/firestore-schema.md`
- `docs/security-hardening.md`
- `docs/test-plan.md`
