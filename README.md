# Universal ESP32 Home — Codemagic-ready starter project

## Files
- `codemagic.yaml`: Codemagic workflow that builds a debug APK and publishes it as an artifact.
- `app/`: native Android Kotlin app.
- `esp32_firmware/UniversalESP32_AP.ino`: starter ESP32 Arduino firmware.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`: Gradle project configuration.

## Upload to GitHub
1. Download and extract this ZIP.
2. Create a GitHub repository.
3. Upload the *contents* of this folder (not the ZIP itself) to the repository root.
4. Ensure `codemagic.yaml`, `settings.gradle.kts`, `build.gradle.kts`, and the `app` folder are visible at the repository root.
5. In Codemagic, connect that repository and click **Check for configuration files**.
6. Select `android-debug` and start the build.
7. Download the APK from the build's Artifacts section.

The workflow uses the Gradle command installed on the Codemagic build image if a Gradle wrapper script is not present. A `gradle/wrapper/gradle-wrapper.properties` file is included for a wrapper-based setup if you later add the standard wrapper scripts/JAR.

## Current MVP limitations
- Android app opens Android Wi-Fi settings; Android controls network selection and connection.
- App currently supports one active device profile at a time; multi-device management UI is not yet implemented.
- Status is read from `GET /api/status` when refreshed and after commands; WebSocket push is not yet implemented.
- API expects `GET /api/status` to return `{"switches":[false,false,...]}` and `POST /api/switch/{1..8}` with JSON `{"state":true|false}`.
- Change the sample relay GPIO mapping and active polarity for the exact ESP32 board/relay module before connecting hardware.
- Test with low voltage first. Mains wiring must use correctly rated isolated hardware and appropriate electrical safety measures.
