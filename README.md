# Game Booster

Fitur: info RAM/baterai/suhu, mode fokus (Jangan Ganggu + kunci kecerahan), overlay monitor,
peluncur game, atur resolusi (720p, 1080p, atau ukuran kustom), atur DPI / lebar kecil, pintasan penghemat baterai dan penyimpanan.

## Build APK
Push ke GitHub (branch `main`). GitHub Actions otomatis build, APK ada di tab **Releases** (tag `latest`)
dan di **Actions → Build APK → Artifacts**.

## Aktifkan fitur resolusi & DPI (sekali saja)
Jalankan dari PC dengan USB debugging menyala:

    adb shell pm grant com.gamebooster.app android.permission.WRITE_SECURE_SETTINGS

Darurat kalau layar jadi aneh:

    adb shell wm size reset && adb shell wm density reset
