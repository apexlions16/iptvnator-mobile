# Mobil dönüşüm raporu

## İncelenen yapı

Kaynak ve hedef depoların başlangıçtaki `master` dalları aynı commit üzerindeydi. Proje aşağıdaki ana katmanlardan oluşan bir Nx monoreposudur:

- `apps/web`: Angular tabanlı kullanıcı arayüzü ve PWA hedefi
- `apps/electron-backend`: masaüstü Electron süreçleri, yerel dosya ve ağ entegrasyonları
- `apps/remote-control-web`: uzaktan kumanda arayüzü
- `apps/web-backend`: web hedefinin sunucu katmanı
- `libs/playlist`: oynatma listesi alanı
- `libs/m3u-state`: M3U durum yönetimi
- `libs/epg/data-access`: EPG veri erişimi
- `libs/portal`: Xtream ve Stalker portal işlevleri
- `libs/services`, `libs/shared`, `libs/ui`: ortak servisler, tipler ve kullanıcı arayüzü bileşenleri

Kaynak uygulama Angular 21, Nx 22, Electron 41, NgRx, HLS.js, Video.js ve M3U/EPG ayrıştırma paketleri kullanır.

## Mobil strateji

Electron süreçlerini Android'e doğrudan taşımak yerine, kaynak ürünün mobilde anlamlı kullanım akışları Android'in yerel yetenekleriyle yeniden uygulanmıştır. Bunun nedenleri:

1. Electron masaüstü IPC ve dosya sistemi API'leri Android'de kullanılamaz.
2. Web video oynatıcıları yerine Android Media3 cihaz codec, HLS ve yaşam döngüsü entegrasyonu sağlar.
3. Android belge seçici, kullanıcıdan geniş depolama izni istemeden M3U dosyası açabilir.
4. Bağımsız modül, mevcut masaüstü ve web derlemelerini etkilemez.
5. Masaüstü süreç başlatma özelliklerinin mobil karşılığı harici süreç değil, yerel oynatıcı ve uygulama durumudur.

## Uygulanan özellikler

### Kaynaklar ve kataloglar

- Yerel Android uygulama modülü: `apps/mobile-android`
- URL ve cihaz dosyası üzerinden M3U/M3U8 içe aktarma
- `tvg-name`, `tvg-logo`, `tvg-id`, `group-title` ve `radio` özniteliklerini ayrıştırma
- Yinelenen yayın adreslerini temizleme
- Özel M3U User-Agent
- Uygulama açılışında M3U otomatik yenileme
- XMLTV üzerinden EPG yükleme
- Xtream Codes hesap doğrulama
- Xtream canlı TV, radyo, film, dizi, sezon ve bölüm katalogları
- Stalker / Ministra el sıkışma, tür, kanal ve `create_link` oynatma bağlantısı çözümleme

### Mobil kullanıcı deneyimi

- Türkçe varsayılan kullanıcı arayüzü
- Canlı TV, radyo, film, dizi ve bölüm türlerine göre filtreleme
- Kanal, içerik ve grup araması
- Grup filtreleme
- Tüm kaynaklarda kalıcı favoriler
- Son izlenenler
- Film ve bölümlerde kaldığı yerden devam
- Geçerli ve sonraki EPG programının kanal kartında gösterilmesi
- Media3/ExoPlayer tabanlı yayın oynatma
- Kaynağa özel User-Agent ile oynatma
- Kanal ve içerik logoları
- Açık/koyu tema uyumu
- Türkçe hata, bilgilendirme, güvenlik ve yasal metinler

### Güvenlik ve kalite

- Xtream parolası ve Stalker MAC bilgisi kalıcı depolamaya yazılmaz.
- Genel depolama izni yerine Android belge sağlayıcısı kullanılır.
- Bazı IPTV kaynakları nedeniyle HTTP trafiğine izin verilir; arayüzde HTTPS önerilir.
- M3U ayrıştırıcı birim testleri
- GitHub Actions ile birim test, debug APK ve hata günlüğü artifact üretimi

## Mobil kapsam farkları

Aşağıdaki özellikler masaüstü işletim sistemi süreçlerine veya pencere entegrasyonuna bağlı olduğundan Android'e birebir taşınmamıştır:

- Gömülü MPV pencere entegrasyonu
- Harici MPV, VLC ve IINA süreç yönetimi
- Masaüstü klavye kısayolları ve komut paleti
- Masaüstü indirme yöneticisi
- Electron uzaktan kumanda süreci ve masaüstü otomatik güncelleyici

Mobil karşılık olarak Media3 oynatıcı, Android yaşam döngüsü, dokunmatik filtreler, izleme geçmişi ve VOD devam konumu uygulanmıştır.

## Marka kararı

Kaynak deponun `TRADEMARK.md` belgesine göre IPTVnator adı ve logosu yeniden dağıtımda kullanılamaz. Bu nedenle uygulama **Cep IPTV** olarak adlandırılmış ve yeni bir simge hazırlanmıştır.

## APK

GitHub Actions başarılı olduğunda APK şu artifact içinde yayımlanır:

```text
cep-iptv-debug-apk/app-debug.apk
```

Doğrulanmış Android 1.1.0 debug paketi:

- Paket: `com.apexlions.cepiptv.debug`
- Sürüm: `1.1.0-debug`
- Sürüm kodu: `2`
- Minimum SDK: `24`
- Hedef / derleme SDK: `35`
- İmza: APK Signature Scheme v2

Bu APK test ve doğrudan kurulum içindir. Mağaza dağıtımı için gizli Android keystore ile imzalı release APK/AAB ve mağaza politikası kontrolleri ayrıca yapılandırılmalıdır.
