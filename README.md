#  ChefCore - Sistema Inteligente de Gestión para Hostelería

## 1. Despliegue y Pruebas (Instalación Rápida)
Para facilitar la evaluación por parte del tribunal, la aplicación ha sido compilada y desplegada en formato APK.

* **Descarga directa del instalable:** [⬇️ Descargar ChefCore.apk](https://raw.githubusercontent.com/aridc2/ChefCore/master/releases/chefcore.apk)
* **Requisitos mínimos:** Android 8.0 o superior.
* **Dispositivo de desarrollo:** Tablet Lenovo Tab M11 (Android 13).

> Al instalar desde fuera de Google Play, el dispositivo pedirá permiso para instalar apps de origen desconocido. Ir a **Ajustes → Seguridad → Instalar apps desconocidas** y habilitarlo para el gestor de archivos.

---

## 2. Código Fuente

El código fuente completo se encuentra en la carpeta `/app`. La estructura principal es:

```
app/src/main/java/es/chefcore/app/
├── data/        # Entidades Room, DAOs y repositorios
├── logic/       # Parser OCR, gestión de voz, CocinaManager (PMP)
├── ui/          # Pantallas y componentes Jetpack Compose
├── viewmodel/   # 9 ViewModels (patrón MVVM)
└── workers/     # Sincronización Firebase en segundo plano
```

Por motivos de seguridad, el archivo `google-services.json` de Firebase ha sido excluido...

## 3. Stack tecnológico

| Tecnología | Uso |
|---|---|
| Kotlin 2.0 + Jetpack Compose | Lenguaje e interfaz |
| Room v8 (SQLite) | Base de datos local |
| Firebase Auth + Firestore | Autenticación y sincronización |
| Google ML Kit | OCR de albaranes |
| Android SpeechRecognizer | Voz offline |
| WorkManager | Sincronización en segundo plano |

## 4. Manual Técnico

### Requisitos del entorno de desarrollo
- Android Studio Hedgehog 2023.1.1 o superior
- JDK 17
- Kotlin 2.0
- Gradle 8.x

### Configuración de Firebase
1. Crear proyecto en [Firebase Console](https://console.firebase.google.com)
2. Registrar app Android con package name `es.chefcore.app`
3. Descargar `google-services.json` y colocarlo en `/app`
4. Habilitar **Authentication** con proveedor Email/Password
5. Crear base de datos **Firestore** en modo producción

### Extender el parser OCR a un nuevo proveedor
1. Añadir el nuevo valor en el enum `TipoAlbaran` en `AlbaranOcrParser.kt`
2. Añadir su palabra clave en `detectarTipo()`
3. Implementar la función `parseNuevoProveedor()` siguiendo el patrón de `parseSercodi()`
4. Añadirlo al `when` dentro de `extraerItems()`

### Extender el sistema de voz con nuevos comandos
1. Abrir `VoiceCommander.kt`
2. Añadir una nueva rama `when` con la palabra clave del comando
3. Implementar la lógica usando los DAOs disponibles (`iDao`, `rDao`)
4. Si el comando necesita parsear texto, añadir el patrón en `VoiceParser.kt`