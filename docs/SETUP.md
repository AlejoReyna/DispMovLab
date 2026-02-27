# Guía de configuración — DispMov Chat

## 1. Prerrequisitos

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 17 |
| Android SDK | API 35 (se instala desde Android Studio) |
| Cuenta Firebase | gratuita |
| Cuenta Agora | gratuita (para videollamadas) |

---

## 2. Firebase — Configuración obligatoria

### 2.1 Crear el proyecto

1. Abre [console.firebase.google.com](https://console.firebase.google.com) e inicia sesión.
2. Haz clic en **Agregar proyecto** → pon un nombre → acepta los pasos.
3. En el menú lateral ve a **Configuración del proyecto** (ícono de engranaje) → **Tus apps** → **Android**.
4. Registra la app con el package name: `com.example.chat`
5. Descarga el archivo `google-services.json` y cópialo exactamente aquí:
   ```
   DispMovLab/app/google-services.json
   ```

### 2.2 Habilitar Authentication

1. En la consola de Firebase → **Authentication** → **Comenzar**.
2. Pestaña **Sign-in method** → habilita **Correo electrónico/contraseña**.
3. En la misma pestaña habilita también **Teléfono** (Phone). Este proveedor es obligatorio para el registro de nuevos usuarios (flujo estilo WhatsApp con código SMS).
4. En **Plantillas de correo** puedes personalizar el correo de verificación.

> **Nota:** Para probar Phone Auth en un emulador sin recibir SMS reales, ve a **Authentication → Sign-in method → Teléfono** y agrega un número de prueba (ej. `+15555215554` con código `123456`). En dispositivo físico funciona con SMS real y Google Play Services.

### 2.3 Crear base de datos Firestore

1. **Firestore Database** → **Crear base de datos**.
2. Elige modo **Producción** o **Prueba** (prueba para desarrollo).
3. Selecciona la región más cercana a tus usuarios.
4. Aplica las reglas que están en `firebase/firestore.rules`.

### 2.4 Desplegar Cloud Functions

Las funciones `setUserActiveState` y `setUserRole` ya están escritas en
`firebase/functions/src/index.ts`.

```bash
# Instala Firebase CLI si no lo tienes
npm install -g firebase-tools

cd DispMovLab/firebase
firebase login
firebase use --add          # elige tu proyecto
cd functions && npm install
cd ..
firebase deploy --only functions
```

---

## 3. Agora — Videollamadas

### 3.1 Obtener el App ID

1. Abre [console.agora.io](https://console.agora.io) y regístrate (gratis).
2. **New Project** → nombre → haz clic en **Create**.
3. En la página del proyecto copia el **App ID**.

### 3.2 Agregar el App ID al proyecto

Abre `DispMovLab/local.properties` y descomenta/edita la línea:

```properties
agora.app.id=TU_APP_ID_AQUI
```

> ⚠️ **Nunca subas `local.properties` a un repositorio público.** Ya está en `.gitignore`.

### 3.3 Token para producción

En modo de prueba Agora permite unirse a canales sin token (`token = null`).
Para producción debes generar tokens en tu propio servidor usando el
[Agora Token Builder](https://github.com/AgoraIO/Tools/tree/master/DynamicKey).
Actualiza `VideoCallViewModel.initAndJoin()` para pasar el token generado.

### 3.4 Verificar la dependencia de Agora

En `app/build.gradle.kts` está:
```kotlin
implementation("io.agora.rtc:full-sdk:4.5.0")
```
y en `settings.gradle.kts`:
```kotlin
maven { url = uri("https://agora-repo.jfrog.io/artifactory/libs-release") }
```
Si Gradle no encuentra la dependencia, consulta la
[documentación oficial](https://docs.agora.io/en/voice-calling/get-started/get-started-sdk?platform=android)
para el repositorio y versión actuales.

---

## 4. Abrir y compilar en Android Studio

```
File → Open → selecciona la carpeta DispMovLab
```

1. Espera a que Gradle sincronice (puede tardar 2-5 min la primera vez).
2. Selecciona un emulador o conecta un dispositivo físico.
3. Presiona **▶ Run 'app'** o `Shift + F10`.

---

## 5. Flujo de la app

```
Registro / Inicio de sesión
        ↓
Verificación de correo electrónico
        ↓
Lista de conversaciones  ──────────────── [+] Nuevo chat → Elige usuario
        ↓ (tap conversación)
Chat  ──────────────────────────────────── [📹] Videollamada
        ↓ (solo admin)
Panel de administración → Gestionar usuarios y roles
```

## 6. Roles de usuario

| Rol | Permisos |
|---|---|
| `user` | Ver conversaciones propias, enviar mensajes, videollamada |
| `admin` | Todo lo anterior + panel admin, cambiar roles, activar/desactivar usuarios |

El primer usuario que se registre tendrá rol `user`.
Para asignar el primer admin, edita directamente en Firestore:
`users/{uid}` → campo `role` → valor `"admin"`.

---

## 7. Resumen de archivos clave

| Archivo | Propósito |
|---|---|
| `local.properties` | App ID de Agora (nunca subir a Git) |
| `app/google-services.json` | Configuración de Firebase (nunca subir a Git) |
| `firebase/firestore.rules` | Reglas de seguridad de Firestore |
| `firebase/functions/src/index.ts` | Cloud Functions para roles y moderación |
| `core/AgoraManager.kt` | Wrapper del SDK de videollamadas |
| `core/LanguagePrefs.kt` | Preferencia de idioma (ES / EN) |
