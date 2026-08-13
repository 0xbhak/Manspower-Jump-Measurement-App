# Penjas Jump Measure
Aplikasi Android pendeteksi lompatan otomatis berbasis AI yang dirancang untuk mengukur dan mendokumentasikan kinerja atlet secara presisi menggunakan **CameraX**, **MediaPipe**, dan **Room Database**.

## Fitur Utama
1. **Vertical & Horizontal Jump Measurement**: Mendeteksi waktu *Takeoff* dan *Landing* secara *real-time* dan memproyeksikannya menjadi ketinggian lompatan vertikal dan jarak horizontal (dalam CM).
2. **Offline First AI**: Seluruh proses Computer Vision berjalan di dalam perangkat (on-device) menggunakan MediaPipe Pose Landmarker tanpa membutuhkan koneksi internet.
3. **Geometry Calibrator**: Mesin konversi yang mengubah hasil koordinat piksel di layar (Pinhole Camera Model) menjadi jarak di dunia nyata (Meter) menggunakan referensi posisi Tripod Kamera (Standar 10 Meter).
4. **Neon Futuristic UI**: Antarmuka modern bergaya cyberpunk dengan dominasi gelap (Dark Mode), warna Neon Purple, Neon Cyan, dan Skeleton Green yang memukau.
5. **Local History & CSV Export**: Semua pengukuran disimpan dengan aman di perangkat menggunakan *Room Database*. Riwayat dapat dikelola, dihapus, atau diekspor ke dalam file .csv menggunakan Storage Access Framework (SAF).

## Arsitektur Proyek
Aplikasi dibangun di atas pondasi **Clean Architecture** (MVI) dengan fokus kuat pada uji coba dan logika fungsional murni.
- **UI Layer (Jetpack Compose)**: `MainScreen`, `CameraScreen`, `HistoryScreen`, `SplashScreen`.
- **Domain Layer**: Entitas murni Kotlin (`JumpMetrics`, `WorldPoint`, `MeasurementState`) untuk analisis data pose.
- **Data Layer (Hilt & Room)**: `AppDatabase` dengan `JumpDao` untuk persistensi, dibungkus menggunakan Dagger Hilt untuk kemudahan Injeksi Dependensi (DI).

## Persyaratan Sistem
- Android Minimum SDK: 26 (Android 8.0 Oreo)
- Perangkat keras: Minimum RAM 3GB dengan CPU yang sanggup melakukan rendering ML inferensi 30FPS. (Disarankan menggunakan Tripod).

## Build Setup (Developer)
1. **Gradle**: Menggunakan *Kotlin Symbol Processing (KSP)*. Sangat direkomendasikan pada Windows agar tidak ada masalah `SQLite.dll AccessDeniedException`.
2. Buka folder root `penjas-jump` di **Android Studio**.
3. Sync Project with Gradle Files.
4. Run `app` module.

## Kontribusi
Aplikasi dirancang khusus dengan pendekatan MVP yang sangat fokus pada satu tugas. Harap buat file Unit Test jika mengubah `GeometryCalibrator` karena sistem proyeksi matematika sangat sensitif.
