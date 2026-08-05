# Feature Specification: Profil Fotoğrafı Yükleme, Değiştirme ve Silme

**Feature Branch**: `003-profile-photo-upload`

**Created**: 2026-08-05

**Status**: Draft

**Input**: User description: "profil fotoğrafı yükleme alanı yapmak istiyorum. user profil imgesine tıplayıp mevcut fotoğrafını silebilmeli fotoraf ekleyeb ilmeli ya da fotoğrafı varsa değiştirebilmeli"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Profil fotoğrafı olmayan bir kullanıcının fotoğraf eklemesi (Priority: P1)

Henüz profil fotoğrafı bulunmayan bir kullanıcı, kendi profilindeki avatar alanına dokunur ve
cihazından bir fotoğraf seçerek (galeriden seçme veya kamerayla çekme) profiline ekler. Fotoğraf
yüklendikten sonra, kullanıcının avatarı artık baş harfinden oluşan simge yerine seçtiği fotoğrafı
gösterir.

**Why this priority**: Bu, özelliğin temel değer önerisidir — fotoğrafsız bir profilin ilk kez
fotoğraf kazanması. Bu olmadan "değiştirme" ve "silme" hikayelerinin hiçbir anlamı yoktur.

**Independent Test**: Fotoğrafı olmayan bir hesapla giriş yap, kendi profilindeki avatar alanına
dokun, galeriden bir fotoğraf seç, yüklemenin tamamlandığını ve avatarın artık o fotoğrafı
gösterdiğini doğrula.

**Acceptance Scenarios**:

1. **Given** kullanıcının profilinde henüz fotoğraf yok (baş harfi gösteriliyor), **When**
   kullanıcı kendi profilindeki avatar alanına dokunur, **Then** "Fotoğraf Ekle" seçeneğini içeren
   bir işlem menüsü/seçenek listesi açılır.
2. **Given** işlem menüsü açık, **When** kullanıcı "Fotoğraf Ekle"yi seçip cihazından bir görsel
   seçer/çeker, **Then** görsel yüklenir ve kullanıcı yükleme sırasında bir ilerleme/durum
   göstergesi görür.
3. **Given** yükleme başarıyla tamamlanmış, **When** kullanıcı profiline (kendi profil görünümü,
   arama sonuçları, arkadaş listeleri gibi avatarın göründüğü her yer) bakar, **Then** baş harfi
   simgesi yerine yüklenen fotoğraf gösterilir.
4. **Given** kullanıcı bir görsel seçer, **When** seçilen dosya desteklenmeyen bir biçimde veya
   izin verilen boyut sınırının üzerindeyse, **Then** sistem yüklemeyi reddeder ve kullanıcıya
   anlaşılır bir hata mesajı gösterir; mevcut avatar durumu (baş harfi) değişmeden kalır.

---

### User Story 2 - Mevcut fotoğrafı olan bir kullanıcının fotoğrafını değiştirmesi (Priority: P1)

Profilinde zaten bir fotoğraf bulunan bir kullanıcı, avatarına dokunarak mevcut fotoğrafı yeni bir
fotoğrafla değiştirmek ister. Yeni fotoğraf yüklendiğinde eski fotoğrafın yerini alır; eski fotoğraf
artık hiçbir yerde görünmez.

**Why this priority**: Fotoğrafını güncel tutmak (ör. profil fotoğrafını yenilemek), fotoğraf
ekleme kadar temel ve sık kullanılan bir ihtiyaçtır; P1 fotoğraf ekleme hikayesiyle birlikte
özelliğin asıl talep edilme sebebinin ikinci yarısını oluşturur.

**Independent Test**: Profilinde fotoğraf olan bir hesapla giriş yap, avatara dokun, "Fotoğrafı
Değiştir"i seç, yeni bir görsel seç, yeni fotoğrafın eskisinin yerini aldığını doğrula.

**Acceptance Scenarios**:

1. **Given** kullanıcının profilinde zaten bir fotoğraf var, **When** kullanıcı avatarına dokunur,
   **Then** açılan işlem menüsünde "Fotoğrafı Değiştir" ve "Fotoğrafı Sil" seçenekleri (fotoğrafsız
   durumdaki "Fotoğraf Ekle" yerine) sunulur.
2. **Given** işlem menüsü açık, **When** kullanıcı "Fotoğrafı Değiştir"i seçip yeni bir görsel
   seçer/çeker, **Then** yeni görsel yüklenir ve yükleme tamamlandığında eski fotoğrafın yerini
   alır.
3. **Given** değiştirme işlemi tamamlanmış, **When** kullanıcı profiline tekrar bakar, **Then**
   yalnızca yeni fotoğraf görünür; eski fotoğrafa hiçbir yerden erişilemez.

---

### User Story 3 - Kullanıcının mevcut fotoğrafını silmesi (Priority: P2)

Profilinde bir fotoğraf bulunan bir kullanıcı, bu fotoğrafı tamamen kaldırıp profilinin yeniden
baş harfinden oluşan varsayılan simgeyi göstermesini ister.

**Why this priority**: Ekleme ve değiştirmeye göre daha az sık kullanılsa da (kullanıcı genelde
fotoğrafını değiştirmeyi tercih eder), gizlilik/fikir değiştirme ihtiyacı için gereklidir ve
kullanıcı isteğinde açıkça belirtilmiştir.

**Independent Test**: Profilinde fotoğraf olan bir hesapla giriş yap, avatara dokun, "Fotoğrafı
Sil"i seç, onayla, profilin yeniden baş harfi simgesini gösterdiğini doğrula.

**Acceptance Scenarios**:

1. **Given** kullanıcının profilinde bir fotoğraf var, **When** kullanıcı işlem menüsünden
   "Fotoğrafı Sil"i seçer, **Then** geri alınamaz bir işlem olduğu için sistem onay ister (ör.
   "Fotoğrafını silmek istediğine emin misin?").
2. **Given** kullanıcı silme işlemini onaylar, **When** silme tamamlanır, **Then** profildeki
   fotoğraf kaldırılır ve kullanıcının avatarı, fotoğrafsız durumdaki gibi baş harfini gösteren
   simgeye döner.
3. **Given** kullanıcı onay isteminde vazgeçer, **When** iptali seçer, **Then** mevcut fotoğraf
   değişmeden kalır.

---

### Edge Cases

- Kullanıcı fotoğraf seçme/çekme işlemini (galeri veya kamera izni istemi dahil) yarıda
  bırakırsa ne olur? (Mevcut avatar durumu değişmeden kalmalı, hiçbir kısmi/bozuk yükleme
  kaydedilmemelidir.)
- Kullanıcı yükleme sırasında uygulamadan çıkarsa veya bağlantı kesilirse ne olur? (Yükleme
  tamamlanmamış sayılmalı; mevcut fotoğraf (varsa) değişmeden kalmalıdır.)
- Bir kullanıcı, kendi profili dışında başka bir kullanıcının profilini görüntülerken avatara
  dokunursa ne olur? (Fotoğraf ekleme/değiştirme/silme işlem menüsü yalnızca kullanıcı kendi
  profilini görüntülerken sunulmalıdır; başkasının profilinde avatar salt görünümdür.)
- Kullanıcı, galeri/kamera erişim izni vermemişse ne olur? (Sistem, izin verilmediğini açıklayan
  anlaşılır bir mesaj göstermeli, uygulama çökmemelidir.)
- Aynı anda birden fazla cihazdan/sekmeden fotoğraf yükleme/silme denenirse ne olur? (Son
  tamamlanan işlem geçerli olmalı; yarım kalan bir işlem diğerini bozmamalıdır.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Sistem, kullanıcı kendi profilini görüntülerken avatar/profil fotoğrafı alanının
  dokunulabilir olmasını sağlamalıdır.
- **FR-002**: Kullanıcı kendi avatarına dokunduğunda sistem, mevcut fotoğraf durumuna göre uygun
  seçenekleri içeren bir işlem menüsü göstermelidir: fotoğraf yoksa yalnızca "Fotoğraf Ekle";
  fotoğraf varsa "Fotoğrafı Değiştir" ve "Fotoğrafı Sil".
- **FR-003**: Kullanıcılar, cihazlarındaki mevcut bir fotoğrafı seçerek ya da yeni bir fotoğraf
  çekerek profil fotoğrafı olarak yükleyebilmelidir.
- **FR-004**: Sistem, yeni bir fotoğraf yüklendiğinde (ekleme ya da değiştirme durumunda) yükleme
  tamamlanana kadar kullanıcıya bir ilerleme/durum göstergesi sunmalıdır.
- **FR-005**: Sistem, desteklenmeyen dosya biçimi ya da izin verilen boyut sınırını aşan
  dosyaları reddetmeli ve kullanıcıya anlaşılır bir hata mesajı göstermelidir; reddedilen bir
  yükleme mevcut fotoğraf durumunu değiştirmemelidir.
- **FR-006**: Bir fotoğraf değiştirildiğinde, yeni fotoğraf eski fotoğrafın yerini tamamen almalı
  ve eski fotoğraf uygulamanın hiçbir yerinde (kendi profili, arama sonuçları, arkadaş listeleri
  vb.) görüntülenmemelidir.
- **FR-007**: Kullanıcılar mevcut profil fotoğraflarını silebilmelidir; silme işlemi geri
  alınamaz olduğundan sistem silmeden önce açık bir onay istemelidir.
- **FR-008**: Bir fotoğraf silindiğinde, kullanıcının avatarı uygulamanın her yerinde (kendi
  profili, arama sonuçları, arkadaş listeleri vb.) fotoğrafsız durumdaki varsayılan (baş harfi)
  gösterime dönmelidir.
- **FR-009**: Sistem, bir kullanıcının profil fotoğrafını yalnızca o kullanıcının kendisinin
  ekleyebilmesini/değiştirebilmesini/silebilmesini sağlamalıdır; başka bir kullanıcının profilini
  görüntüleyen biri bu işlemleri yapamamalıdır.
- **FR-010**: Sistem, yüklenen profil fotoğrafını, kullanıcının profilinin göründüğü her yerde
  (kendi profil görünümü, başkalarının profil görünümü, arama sonuçları, arkadaş/mevcut oyuncu
  listeleri) tutarlı şekilde göstermelidir.

### Key Entities *(include if feature involves data)*

- **Profil Fotoğrafı**: Bir kullanıcının profiline eklediği görsel. Kullanıcı profiliyle bire-bir
  ilişkilidir (bir kullanıcının aynı anda en fazla bir aktif profil fotoğrafı olabilir);
  fotoğrafın kendisi yoksa kullanıcı varsayılan olarak baş harfinden oluşan bir simgeyle temsil
  edilir. Ekleme/değiştirme/silme işlemleri bu tekil ilişkiyi günceller.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Kullanıcılar bir profil fotoğrafını, avatara dokunmaktan yükleme onayına kadar 3
  dokunuştan/adımdan fazla olmadan ekleyebilir/değiştirebilir.
- **SC-002**: Yüklenen bir fotoğraf, yükleme tamamlandıktan hemen sonra kullanıcının profilinin
  göründüğü tüm ekranlarda (kendi profili, arama sonuçları, arkadaş listeleri) tutarlı şekilde
  görünür — hiçbir ekranda eski/baş harfi simgesi kalmaz.
- **SC-003**: Fotoğraf değiştirme veya silme işlemlerinin %100'ünde, işlem tamamlandıktan sonra
  eski fotoğrafa uygulamanın hiçbir yerinden erişilemez.
- **SC-004**: Geçersiz (desteklenmeyen biçim/boyut aşımı) bir yükleme denemelerinin %100'ü, mevcut
  fotoğraf durumunu bozmadan anlaşılır bir hata mesajıyla reddedilir.
- **SC-005**: Kullanıcıların %95'i, fotoğraf silme işlemini yanlışlıkla değil yalnızca onay
  adımından geçerek tamamlar (kazara silme oranı %5'in altında).

## Assumptions

- Fotoğraf ekleme/değiştirme akışı, kullanıcıya hem cihaz galerisinden mevcut bir fotoğraf seçme
  hem de kamerayla yeni bir fotoğraf çekme seçeneğini sunar (günümüz mobil uygulamalarında
  standart beklenti); kullanıcı isteği bu ikisi arasında ayrım yapmamıştır.
- Fotoğraf silme, geri alınamaz bir işlem olduğundan, uygulamadaki mevcut geri alınamaz işlem
  kalıbına (ör. çıkış yapma onayı) benzer şekilde bir onay istemi gerektirir.
- Kabul edilebilir dosya biçimleri (ör. JPEG/PNG) ve azami dosya boyutu, sektör standardı mobil
  görsel yükleme sınırlarıyla (birkaç megabayt mertebesinde) uyumlu, makul varsayılan değerlerle
  belirlenecektir; bu spesifik sınırlar bu özelliğin planlama aşamasında netleştirilecektir.
- Bu özellik yalnızca kullanıcının kendi profil fotoğrafını yönetmesini kapsar; başka bir
  kullanıcının fotoğrafını uygunsuz bulan biri için herhangi bir bildirme/moderasyon akışı bu
  kapsamda değildir (uygulamada zaten var olan genel kullanıcı bildirme mekanizması, gerekirse bu
  amaçla ayrıca kullanılabilir).
- Profil fotoğrafı, mevcut "Avatar URL" metin girişi alanının yerini alır; kullanıcılar artık bir
  URL yazarak değil, doğrudan cihazlarından bir görsel yükleyerek profil fotoğrafı belirler.
- Sistem, yüklenen fotoğrafı kalıcı olarak saklar ve kullanıcı oturumları/cihazları arasında
  tutarlı şekilde sunar; bu saklama mekanizmasının teknik detayları bu özelliğin planlama
  aşamasında belirlenecektir.
