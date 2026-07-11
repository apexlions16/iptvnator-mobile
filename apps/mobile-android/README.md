# Cep IPTV — Android uygulaması

Bu klasör, IPTVnator kaynak projesinin ürün alanı ve veri akışları incelenerek Android için bağımsız şekilde geliştirilen mobil uygulamayı içerir.

> **Marka notu:** IPTVnator adı ve logosu kaynak projenin sahibine aittir. Bu nedenle mobil dağıtımda özgün **Cep IPTV** adı ve özgün simge kullanılmıştır.

## Sürüm 1.1 özellikleri

### Kaynaklar

- M3U ve M3U8 oynatma listelerini URL üzerinden yükleme
- Android belge seçici ile yerel M3U/M3U8 dosyası açma
- M3U için özel `User-Agent` tanımlama
- Kaydedilmiş M3U listesini uygulama açılışında otomatik yenileme
- Xtream Codes kullanıcı adı/parola ile hesap doğrulama
- Xtream canlı TV, radyo, film, dizi, sezon ve bölüm katalogları
- Stalker / Ministra portal el sıkışması, kanal listesi ve `create_link` çözümleme
- XMLTV adresinden EPG yükleme

### Katalog ve oynatma

- Canlı TV, radyo, film, dizi ve bölüm türlerine göre filtreleme
- Kanal, içerik açıklaması ve grup içinde arama
- Grup filtreleme
- Tüm kaynaklarda kalıcı favoriler
- Son izlenenler
- Film ve bölümlerde kaldığı yerden devam
- Kanal ve içerik logoları
- Geçerli ve sonraki EPG programını listede gösterme
- Media3 / ExoPlayer ile HLS ve HTTP yayın oynatma
- Kaynağa özel `User-Agent` ile yayın açma
- Açık ve koyu sistem temasına uyum

### Dil ve güvenlik

- Uygulama arayüzü, hata metinleri, CI adımları ve belgeler Türkçedir.
- Xtream parolası ve Stalker MAC bilgisi cihazda kalıcı olarak saklanmaz; yalnızca açık uygulama oturumunda bellekte tutulur.
- Dosya seçimi Android belge sağlayıcısı üzerinden yapılır; genel depolama izni istenmez.

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

Kök dizindeki `.github/workflows/android-apk.yml` iş akışı her mobil değişiklikte testleri çalıştırır ve kurulabilir debug APK dosyasını `cep-iptv-debug-apk` adlı artifact olarak yayımlar. Derleme başarısız olsa bile Gradle günlükleri `android-gradle-gunlukleri` artifact'ına eklenir.

## Güvenlik ve içerik sorumluluğu

Uygulama herhangi bir kanal, oynatma listesi, abonelik veya dijital içerik sağlamaz. Kullanıcı yalnızca erişim hakkına sahip olduğu yayın kaynaklarını eklemelidir.

Bazı IPTV hizmetleri şifresiz HTTP kullandığı için Android ağ yapılandırmasında cleartext trafiğe izin verilmiştir. Üretim dağıtımında mümkün olduğunda HTTPS tercih edilmelidir.

## Masaüstünden farklı olan özellikler

Android sürümünde masaüstüne özgü gömülü MPV pencere entegrasyonu, harici MPV/VLC/IINA süreç yönetimi, klavye kısayolları ve masaüstü indirme yöneticisi bulunmaz. Mobil karşılık olarak Media3 oynatıcı, Android yaşam döngüsü yönetimi ve VOD devam konumu kullanılır.

## İmzalı mağaza sürümü

GitHub Actions kurulabilir debug APK üretir. Google Play veya son kullanıcı dağıtımı için bir Android keystore oluşturulmalı; keystore ve parolalar GitHub Actions Secrets alanına eklenmeli ve `release` buildType imzalama yapılandırması yapılmalıdır. Gizli anahtar hiçbir zaman depoya eklenmemelidir.
