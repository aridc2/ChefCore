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

app/src/main/java/es/chefcore/app/
├── data/           # Entidades Room, DAOs y repositorios
├── logic/          # Parser OCR, gestión de voz, CocinaManager (PMP)
├── ui/             # Pantallas y componentes Jetpack Compose
├── viewmodel/      # 9 ViewModels (patrón MVVM)
└── workers/        # Sincronización Firebase en segundo plano

Por motivos de seguridad, el archivo `google-services.json` de Firebase ha sido excluido del repositorio mediante `.gitignore`. Para compilar desde código fuente es necesario añadir este fichero en `app/` con una configuración propia de Firebase.

---

## 3. Stack tecnológico

| Tecnología | Uso |
|---|---|
| Kotlin 2.0 + Jetpack Compose | Lenguaje e interfaz |
| Room v8 (SQLite) | Base de datos local |
| Firebase Auth + Firestore | Autenticación y sincronización |
| Google ML Kit | OCR de albaranes |
| Android SpeechRecognizer | Voz offline |
| WorkManager | Sincronización en segundo plano |