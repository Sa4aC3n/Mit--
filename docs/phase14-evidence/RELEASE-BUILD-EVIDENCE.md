# PHASE 14 — Release Build Evidence Report
## مشروع دليل ميت غمر (Mit Ghamr Directory)

### 1. أوامر البناء
```bash
gradle :app:assembleRelease
```
* **النتيجة**: `BUILD SUCCESSFUL`

### 2. قياس الحجم وبصمة SHA-256 لحزمة APK
* **مسار الحزمة**: `app/build/outputs/apk/release/app-release.apk`
* **الحجم الفعلي**: `25.0 MB` (بعد إزالة الملفات المؤقتة وتفعيل R8 Resource Shrinking).
* **بصمة SHA-256**:
  ```text
  eadeb0c5d1712fcf9f2cec4877198990e4d256666b2181fe12e7dca2bf78c4de  app/build/outputs/apk/release/app-release.apk
  ```
