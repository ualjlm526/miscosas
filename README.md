# Objetivo 70

Aplicación Android offline para seguimiento de peso, alimentación, pasos, entrenamiento y hábitos.

## Stack
- Kotlin 2.4.10
- Jetpack Compose + Material 3 (BOM 2026.09.00)
- Room 2.8.5
- MVVM con Repository + ViewModel
- Navigation Compose
- AlarmManager para recordatorios locales
- Sensor `TYPE_STEP_COUNTER` con entrada manual de respaldo

## Abrir y compilar
1. Abre esta carpeta en Android Studio Quail 4 (2026.1.4) o posterior.
2. Deja que Android Studio sincronice Gradle y descargue el SDK 37 y dependencias.
3. Ejecuta en un dispositivo/emulador con Android 8.0 (API 26) o superior.


## Datos y privacidad
Todos los registros se guardan localmente en Room (`objetivo70.db`). No hay red, cuentas ni analítica.

## Sensor de pasos
En Android 10+ se solicita `ACTIVITY_RECOGNITION`. El contador hardware expone pasos acumulados desde el reinicio del dispositivo; esta app establece una base al primer uso del día y registra el incremento desde esa base. Por eso, si se abre por primera vez tarde, el valor sensor puede no cubrir todo el día. La entrada manual permite corregirlo.

## Recordatorios
Se usan alarmas repetitivas inexactas para evitar depender del permiso de alarmas exactas. Android puede mover ligeramente el instante por ahorro de batería.

## Nota de salud
La aplicación es una herramienta de registro personal y no sustituye asesoramiento médico o nutricional profesional. El plan de comidas se incorpora tal como fue solicitado, sin cálculo de calorías ni valoración clínica.


## Compatibilidad Galaxy A50 / Android 11

Esta edición está ajustada específicamente para Samsung Galaxy A50 con Android 11:

- `minSdk = 30` (Android 11).
- `targetSdk = 30` para mantener el comportamiento del sistema alineado con Android 11.
- `compileSdk = 37` únicamente para compilar con las herramientas actuales; esto no obliga al teléfono a tener Android 37.
- No se exige sensor de pasos: si el Galaxy no expone `TYPE_STEP_COUNTER`, la app permite introducir los pasos manualmente.
- Los recordatorios usan `AlarmManager` y notificaciones locales, sin Internet.
- Base de datos Room completamente local.
- APK universal, sin limitar la compilación a una ABI concreta, para mantener compatibilidad con Android Studio/emuladores además del Galaxy A50.

### Instalación en el Galaxy A50

En Android 11: **Ajustes → Datos biométricos y seguridad → Instalar aplicaciones desconocidas** y permite la instalación desde el gestor de archivos que uses. Después abre `Objetivo70.apk`.

> La APK debe generarse desde Android Studio/Gradle; este entorno no incluye el SDK Android ni el `gradle-wrapper.jar`, por lo que aquí se entrega el proyecto fuente listo para compilar.
