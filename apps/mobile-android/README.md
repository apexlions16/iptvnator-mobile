# Cep IPTV — Android uygulaması

Bu klasör, IPTVnator kaynak projesinin ürün alanı ve veri akışları incelenerek Android için bağımsız şekilde geliştirilen mobil uygulamayı içerir.

> **Marka notu:** IPTVnator adı ve logosu kaynak projenin sahibine aittir. Bu nedenle mobil dağıtımda özgün **Cep IPTV** adı ve özgün simge kullanılmıştır.

## Özellikler

- M3U ve M3U8 oynatma listelerini URL üzerinden yükleme
- Android dosya seçici ile yerel M3U/M3U8 dosyası açma
- Kanal adı ve grup içinde arama
- Grup filtreleme
- Kalıcı favoriler
- Kanal logoları
- Media3 / ExoPlayer ile HLS ve HTTP yayın oynatma
- Türkçe kullanıcı arayüzü, hata metinleri ve belgeler
- Açık ve koyu sistem temasına uyum

## Yerel derleme

JDK 17 ve Android SDK 35 kurulu olmalıdır.

```bash
gradle -p apps/mobile-android testDebugUnitTest assembleDebug
```

APK çıktısı:

```text
apps/mobile-android/app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Kök dizindeki `.github/workflows/android-apk.yml` iş akışı her mobil değişiklikte testleri çalıştırır ve kurulabilir debug APK dosyasını `cep-iptv-debug-apk` adlı artifact olarak yayımlar.

## Güvenlik ve içerik sorumluluğu

Uygulama herhangi bir kanal, oynatma listesi veya dijital içerik sağlamaz. Kullanıcı yalnızca erişim hakkına sahip olduğu yayın adreslerini eklemelidir.

Bazı IPTV hizmetleri şifresiz HTTP kullandığı için Android ağ yapılandırmasında cleartext trafiğe izin verilmiştir. Üretim dağıtımında mümkün olduğunda HTTPS tercih edilmelidir.

## İmzalı sürüm

Bu ilk aşama kurulabilir debug APK üretir. Google Play veya doğrudan dağıtım için bir Android keystore oluşturulmalı; keystore ve parolalar GitHub Actions Secrets alanına eklenmeli ve `release` buildType imzalama yapılandırması yapılmalıdır.
