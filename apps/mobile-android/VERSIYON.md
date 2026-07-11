# Cep IPTV 1.1.0-debug

## Yeni özellikler

- XMLTV / EPG: geçerli ve sonraki program bilgisi
- M3U için özel User-Agent
- M3U listesini uygulama açılışında otomatik yenileme
- Xtream Codes canlı TV, radyo, film, dizi ve bölüm desteği
- Stalker / Ministra portal kanal ve oynatma bağlantısı desteği
- İçerik türü ve grup filtreleri
- Tüm kaynaklarda arama ve kalıcı favoriler
- Son izlenenler
- Film ve bölümlerde kaldığı yerden devam
- Portal kimlik bilgilerini kalıcı saklamama
- Tam Türkçe mobil arayüz ve belgeler

## Doğrulama

- Android birim testleri: başarılı
- Debug APK derlemesi: başarılı
- Paket: `com.apexlions.cepiptv.debug`
- Sürüm: `1.1.0-debug` (`versionCode=2`)
- Minimum Android: SDK 24
- Hedef Android: SDK 35
- APK imzası: APK Signature Scheme v2

## Bilinen kapsam farkları

Masaüstü Electron uygulamasına özel gömülü MPV, harici oynatıcı süreç yönetimi, klavye kısayolları ve indirme yöneticisi Android sürümüne taşınmamıştır. Mobil oynatma Media3/ExoPlayer ile yapılır.
