# Mobil dönüşüm raporu

## İncelenen yapı

Kaynak ve hedef depoların `master` dalları aynı commit üzerindedir. Proje aşağıdaki ana katmanlardan oluşan bir Nx monoreposudur:

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

Electron süreçlerini Android'e doğrudan taşımak yerine, kaynak ürünün temel kullanım akışları Android'in yerel yetenekleriyle yeniden uygulanmıştır. Bunun nedenleri:

1. Electron masaüstü IPC ve dosya sistemi API'leri Android'de kullanılamaz.
2. Web video oynatıcıları yerine Android Media3 daha kararlı arka plan, codec ve HLS desteği sağlar.
3. Android belge seçici, kullanıcıdan geniş depolama izni istemeden M3U dosyası açabilir.
4. Bağımsız modül, mevcut masaüstü ve web derlemelerini etkilemez.

## Uygulanan özellikler

- Yerel Android uygulama modülü: `apps/mobile-android`
- Türkçe varsayılan kullanıcı arayüzü
- URL ve cihaz dosyası üzerinden M3U/M3U8 içe aktarma
- `tvg-name`, `tvg-logo` ve `group-title` özniteliklerini ayrıştırma
- Yinelenen yayın adreslerini temizleme
- Kanal arama ve grup filtreleme
- Cihazda kalıcı favoriler
- Media3/ExoPlayer tabanlı yayın oynatma
- Kanal logoları ve mobil kart görünümü
- Açık/koyu tema uyumu
- Türkçe hata, bilgilendirme ve yasal metinler
- M3U ayrıştırıcı birim testleri
- GitHub Actions ile test ve debug APK artifact üretimi

## Marka kararı

Kaynak deponun `TRADEMARK.md` belgesine göre IPTVnator adı ve logosu yeniden dağıtımda kullanılamaz. Bu nedenle uygulama **Cep IPTV** olarak adlandırılmış ve yeni bir simge hazırlanmıştır.

## APK

GitHub Actions başarılı olduğunda APK şu artifact içinde yayımlanır:

```text
cep-iptv-debug-apk/app-debug.apk
```

Bu APK test ve doğrudan kurulum içindir. Mağaza dağıtımı için imzalı release APK/AAB, gizli keystore bilgileri ve mağaza politikası kontrolleri ayrıca yapılandırılmalıdır.

## Sonraki genişletmeler

Mevcut ilk mobil sürüm M3U/M3U8 odaklıdır. Kaynak projedeki EPG, Xtream Codes ve Stalker portal özellikleri daha sonraki mobil aşamalarda ayrı veri kaynakları olarak eklenebilir. Bu özellikler için kimlik bilgisi saklama, SSL/HTTP politikası, büyük liste sayfalama ve cihaz codec testleri ayrıca ele alınmalıdır.
