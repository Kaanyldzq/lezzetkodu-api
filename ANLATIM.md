# LezzetKodu API — Nasıl Yapıldı?

Bu dosya projeyi baştan sona anlamak için. Her bölümün sonunda "Sorarlarsa" kısmında olası sorular ve kısa cevapları var.

---

## 1. Büyük resim: bir istek nasıl işleniyor?

Mobil uygulama `POST /api/recipes` gönderdiğinde sırayla şunlar oluyor:

```
İstek
  │
  ▼
JwtAuthenticationFilter   → "Authorization: Bearer ..." başlığındaki token'ı doğrular, kullanıcıyı tanır
  │
  ▼
SecurityConfig kuralları  → Bu adres giriş istiyor mu? Token yoksa 401 döner, istek controller'a hiç ulaşmaz
  │
  ▼
RecipeController          → JSON'u RecipeRequest'e çevirir, @Valid ile kontrol eder (hatalıysa 400)
  │
  ▼
RecipeService             → İş kuralları: kategori var mı, kullanıcı tarifin sahibi mi, malzemeleri sırala
  │
  ▼
RecipeRepository          → Hibernate SQL'i üretir, PostgreSQL'e yazar
  │
  ▼
RecipeMapper              → Entity'yi RecipeDetailResponse DTO'suna çevirir
  │
  ▼
Cevap: 201 Created + Location başlığı + JSON
```

Bu yolun herhangi bir yerinde hata olursa `GlobalExceptionHandler` yakalar ve hepsini aynı JSON biçiminde döndürür.

---

## 2. Katmanlar ve neden ayrıldılar

| Katman | Görevi | Neden ayrı? |
| --- | --- | --- |
| **Controller** | HTTP ile ilgili her şey: adres, metot, status kodu, parametre | İş kuralı içermez. HTTP yerine başka bir giriş noktası (ör. mesaj kuyruğu) gelse servis aynen kullanılır |
| **Service** | İş kuralları ve transaction | "Sadece sahibi silebilir" gibi kurallar tek yerde durur, test etmesi kolay |
| **Repository** | Veritabanı erişimi | SQL detayları servise sızmaz |
| **DTO** | Dışarıya açılan veri biçimi | Aşağıda ayrıca anlatıldı |

**Paketleme:** Kod `controller/`, `service/` diye katmana göre değil, `recipe/`, `review/`, `auth/` diye **özelliğe göre** paketlendi. "Yorumlarla ilgili bir şey değişecek" dediğinde bakacağın her şey tek klasörde.

**Sorarlarsa:**
- *"Neden controller'da iş kuralı yazmadın?"* Controller HTTP'nin işini yapar. İş kuralı serviste olursa hem tekrar kullanılabilir hem de HTTP olmadan test edilebilir.
- *"Neden constructor injection, neden @Autowired alan değil?"* Bağımlılıklar `final` olur, sınıf eksik bağımlılıkla oluşturulamaz, testte elle nesne oluşturmak kolaylaşır.

---

## 3. Veritabanı

**Tablolar:** `users`, `categories`, `recipes`, `recipe_ingredients`, `favorites`, `reviews`.

İlişkiler:
- Bir kullanıcının çok tarifi var (`recipes.author_id`).
- Bir tarifin bir kategorisi ve çok malzemesi var.
- `favorites` bir **çoka çok** tablo: kullanıcı ↔ tarif. Ayrı bir `id` yok, `(user_id, recipe_id)` birlikte anahtar. Böylece aynı tarif iki kez favorilenemez.
- `reviews` tablosunda `UNIQUE (recipe_id, user_id)`: bir kullanıcı bir tarife tek yorum yapar.

**Veritabanı seviyesinde kurallar:** `CHECK (rating BETWEEN 1 AND 5)`, `CHECK (prep_minutes > 0)`, `NOT NULL`, `UNIQUE`. Kod tarafında da kontrol var, ama veritabanı son savunma hattı. Biri doğrudan SQL ile veri eklese bile kurallar korunur.

**ON DELETE CASCADE:** Tarif silinince malzemeleri, favorileri ve yorumları da otomatik silinir. Yetim kayıt kalmaz.

**Flyway:** Şema `V1__init_schema.sql`, `V2__seed_categories.sql` dosyalarıyla kuruluyor. Uygulama açılırken Flyway hangi dosyaların çalıştığını `flyway_schema_history` tablosundan kontrol eder, yenileri sırayla çalıştırır. Şemayı değiştirmek gerekirse `V3__...sql` eklenir. Eski dosyalara dokunulmaz.

`ddl-auto: validate`: Hibernate şemayı **oluşturmaz**, sadece entity'lerle uyumlu mu diye bakar. Uyumsuzsa uygulama açılmaz.

**Sorarlarsa:**
- *"Neden `ddl-auto: update` kullanmadın?"* `update` neyi nasıl değiştireceği belli olmayan otomatik değişiklikler yapar, veri kaybettirebilir ve geri alınamaz. Flyway ile her değişiklik bir dosya, git'te görünür, her ortamda aynı sırayla çalışır.
- *"Malzeme miktarı neden sayı değil?"* "1 tutam", "yarım su bardağı" gibi değerler sayıya sığmıyor.
- *"Index'leri neden koydun?"* Yabancı anahtarlarla sık filtreleme yapılıyor (kategoriye göre tarif, tarife göre yorum). PostgreSQL yabancı anahtar sütunlarına otomatik index koymaz.

---

## 4. Entity, DTO, Mapper

- **Entity** (`Recipe`, `User`...): veritabanı tablosunun Java karşılığı.
- **DTO** (`RecipeRequest`, `RecipeDetailResponse`...): API'nin aldığı ve döndürdüğü veri. Java `record` olarak yazıldılar: değişmez ve kısa.
- **Mapper**: Entity'yi DTO'ya çevirir.

**Neden entity'yi direkt döndürmüyoruz?**
1. `User` entity'sinde `passwordHash` var. Direkt döndürsek şifre hash'i JSON'a çıkar.
2. Veritabanında bir sütun değişince API sözleşmesi bozulmaz.
3. Lazy ilişkiler JSON'a çevrilirken ya hata verir ya da fark edilmeden onlarca sorgu atılır.

**Request ile response neden ayrı?** İstemci `id`, `authorId`, `createdAt` gönderemesin. Bunları sunucu belirler.

**Sorarlarsa:**
- *"Neden Lombok kullanmadın?"* Kod açık ve okunur olsun diye. DTO'lar zaten `record`, getter yazmaya gerek yok.

---

## 5. Güvenlik

### JWT nasıl çalışıyor?

1. Kullanıcı `/api/auth/login`'e kullanıcı adı ve şifre gönderir.
2. Sunucu şifreyi BCrypt hash'iyle karşılaştırır. Doğruysa bir **JWT** üretir: içinde kullanıcı id'si, adı, rolü ve son kullanma tarihi var. Sunucu bunu **gizli anahtarla imzalar**.
3. İstemci sonraki her istekte `Authorization: Bearer <token>` gönderir.
4. `JwtAuthenticationFilter` imzayı doğrular. Biri token'ın içindeki rolü `ADMIN` yapmaya çalışırsa imza tutmaz ve istek reddedilir.

Token'ın üç parçası var: `header.payload.signature`. Payload şifreli **değil**, sadece base64. Herkes okuyabilir ama kimse değiştiremez. Bu yüzden içine şifre gibi gizli bilgi konmaz.

**Stateless:** Sunucu oturum tutmuyor. Her istek kendi kimliğini taşıyor. Sunucuyu çoğaltmak kolay, çünkü hangi sunucuya gelirse gelsin token doğrulanabilir.

### Diğer kararlar

- **BCrypt:** Bilerek yavaş ve her şifre için farklı "tuz" (salt) ekleniyor. Veritabanı sızsa bile şifreleri toplu kırmak çok pahalı.
- **Giriş hatası mesajı:** Kullanıcı yoksa da, şifre yanlışsa da aynı mesaj dönüyor. Böylece "bu kullanıcı adı var mı?" bilgisi sızmıyor.
- **Kayıtta rol yok:** Herkes `USER` olarak kayıt olur. Admin sadece ortam değişkeniyle, sunucu açılırken oluşturulur.
- **JWT_SECRET zorunlu:** Tanımlı değilse uygulama hiç açılmıyor. Koda gömülü sabit anahtar büyük bir güvenlik açığı olurdu.
- **Sahiplik kontrolü:** `RecipeService.loadForChange` başkasının tarifini değiştirmeye çalışana 403 döndürüyor. Bu kontrol URL kuralıyla yapılamaz, çünkü tarifin sahibi veritabanından okunmadan bilinmiyor.
- **CSRF kapalı:** CSRF, tarayıcının çerezleri otomatik göndermesini kullanan bir saldırı. Biz çerez değil `Authorization` başlığı kullandığımız için gereksiz.

### 401 ile 403 farkı

- **401 Unauthorized:** "Seni tanımıyorum." Token yok, geçersiz ya da süresi dolmuş.
- **403 Forbidden:** "Seni tanıyorum ama buna yetkin yok." Normal kullanıcı kategori eklemeye çalışırsa ya da başkasının tarifini silmeye çalışırsa.

**Sorarlarsa:**
- *"Token çalınırsa ne olur?"* Süresi dolana kadar (24 saat) kullanılabilir. Bu yüzden HTTPS şart, gerçek bir sistemde kısa ömürlü access token + refresh token kullanılır (aşağıda "Geliştirilebilecekler").
- *"Kullanıcı silinirse token'ı hâlâ çalışır mı?"* Evet, süresi dolana kadar. Filtre her istekte veritabanına gitmiyor (hız için). Gerekirse ya veritabanı kontrolü ya da token kara listesi eklenir. Bu bilinçli bir ödünleşim.
- *"HTTPS nerede?"* Uygulama HTTP dinliyor. Üretimde önüne Nginx gibi bir reverse proxy ya da bulut yük dengeleyicisi konur, sertifikayı o taşır. Uygulamaya sertifika gömmek yaygın değil.
- *"Neden filtre `@Component` değil?"* Spring Boot `@Component` olan her filtreyi otomatik olarak normal servlet filtresi de yapar. O zaman her istekte iki kez çalışırdı. O yüzden `SecurityConfig` içinde elle ekleniyor.

---

## 6. Hata yönetimi

`GlobalExceptionHandler` (`@RestControllerAdvice`) bütün hataları yakalayıp aynı biçimde döndürüyor:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found",
  "message": "Tarif bulunamadı: 999", "path": "/api/recipes/999" }
```

- Kendi exception'larımız: `NotFoundException` → 404, `ConflictException` → 409.
- Validation hatalarında `fieldErrors` ile hangi alanın neden hatalı olduğu tek tek dönüyor. Mesajlar Türkçe (`spring.web.locale: tr_TR`).
- Beklenmeyen hatalarda detay loglanıyor ama istemciye sadece "Beklenmeyen bir hata oluştu" gidiyor. Stack trace istemciye sızarsa saldırgana iç yapı hakkında bilgi verir.
- Güvenlik hataları (401/403) controller'a ulaşmadan oluştuğu için `SecurityConfig` içinde ayrıca aynı biçimde yazılıyor.

---

## 7. Performans kararları

- **N+1 problemi:** 20 tarifi listelerken her birinin kategorisi ve yazarı ayrı sorgularla yüklenirse 1 + 20 + 20 = 41 sorgu atılır. `@EntityGraph(attributePaths = {"category", "author"})` ile hepsi tek sorguda geliyor. Detayda da `join fetch` kullanılıyor.
- **`open-in-view: false`:** Açık olursa veritabanı bağlantısı istek bitene kadar açık kalır ve JSON'a çevrilirken fark edilmeden sorgular atılır. Kapalıyken böyle bir hata hemen görünür.
- **Sayfalama:** Liste her zaman sayfalı ve sayfa boyutu en fazla 50. İstemci `size=100000` gönderip sunucuyu yoramaz.
- **Ortalama puan:** `avg()` ve `count()` veritabanında hesaplanıyor. Bütün yorumları belleğe çekip Java'da toplamak gereksiz.
- **`@Transactional(readOnly = true)`:** Okuma işlemlerinde Hibernate değişiklik takibi yapmıyor, biraz daha hızlı.

---

## 8. REST tasarımı

- URL'lerde fiil yok, kaynak var: `/api/recipes/5`, `/api/recipes/5/reviews`.
- Oluşturma `201 Created` ve yeni kaydın adresini `Location` başlığında döndürüyor.
- Silme `204 No Content` döndürüyor (gövde yok).
- **Favoriler idempotent:** `PUT /favorite` iki kez gönderilse de sonuç aynı, hata yok. Mobilde butona iki kez basılması ya da ağ hatasından sonra isteğin tekrarlanması sorun çıkarmıyor. Bu yüzden `POST` değil `PUT`.
- Filtreler `?categoryId=2&difficulty=EASY&q=mercimek` gibi sorgu parametresi. Filtreler `Specification` ile sadece verildiyse sorguya ekleniyor.

---

## 9. Docker

### Dockerfile: iki aşamalı (multi-stage) build

1. **Derleme aşaması:** Maven + JDK içeren imajda proje derlenip `.jar` üretiliyor.
2. **Çalıştırma aşaması:** Sadece JRE içeren küçük imaja yalnızca `.jar` kopyalanıyor. Maven, kaynak kod ve derleme araçları son imajda yok. İmaj küçük ve saldırı yüzeyi dar.

**Katman önbelleği:** Önce sadece `pom.xml` kopyalanıp bağımlılıklar indiriliyor, sonra kod kopyalanıyor. Kod değişip `pom.xml` değişmediyse Docker bağımlılık katmanını önbellekten kullanıyor ve build dakikalar yerine saniyeler sürüyor.

**Root olmayan kullanıcı:** Uygulama `appuser` ile çalışıyor. Uygulamada bir açık bulunsa bile saldırgan container içinde root olmuyor.

### docker-compose.yml

- İki servis: `db` (PostgreSQL) ve `api`.
- **healthcheck + `depends_on: condition: service_healthy`:** API, PostgreSQL gerçekten bağlantı kabul etmeye başlayınca açılıyor. Sadece "container başladı" yetmez, PostgreSQL'in hazır olması birkaç saniye sürer.
- **Servis adı = adres:** API veritabanına `localhost` ile değil `db:5432` ile bağlanıyor. Compose kendi ağında servis adlarını DNS ismi yapıyor.
- **Volume (`pgdata`):** Container silinse bile veriler kalıyor. `docker compose down -v` deyince volume da silinir.
- **`.env`:** Şifreler ve JWT anahtarı dosyada değil ortam değişkenlerinde. `.env` git'e eklenmiyor, sadece örneği (`.env.example`) ekleniyor.

**Sorarlarsa:**
- *"Image ile container farkı?"* Image tarif, container o tariften yapılmış yemek. Bir image'dan birden fazla container çalışabilir.
- *"Container ile sanal makine farkı?"* Sanal makine kendi işletim sistemini taşır, ağırdır. Container ana makinenin çekirdeğini paylaşır, sadece uygulamayı ve bağımlılıklarını taşır. Saniyeler içinde açılır.

---

## 10. Testler

`ApiIntegrationTest` içinde 13 test var. Uygulamayı uçtan uca (filtre → controller → servis → veritabanı) çalıştırıyorlar. Testlerde PostgreSQL yerine bellek içi H2 kullanılıyor, böylece `mvn test` hiçbir kurulum gerektirmiyor.

Test edilenler: kayıt/giriş, aynı kullanıcı adı (409), yanlış şifre (401), validation hataları (400), token'sız istek (401), bozulmuş token (401), admin yetkisi (403/201), tarif CRUD, başkasının tarifini düzenleme (403), olmayan kategori (404), favorilerin idempotent olması, yorum ortalaması ve tekrar yorum engeli.

Her test kendi benzersiz kullanıcılarını oluşturuyor, testler birbirini etkilemiyor.

---

## 11. Bilerek yapılmayanlar / geliştirilebilecekler

Bunları bilmek ve sorulunca söyleyebilmek, yapmış olmak kadar önemli:

- **Refresh token:** Şu an tek token var, 24 saat geçerli. Daha güvenli yapı: 15 dakikalık access token + uzun ömürlü refresh token.
- **Rate limiting:** Giriş endpoint'ine kaba kuvvet (brute force) denemelerini sınırlamak gerekir.
- **Görsel yükleme:** Tarif fotoğrafı yok. Eklenirse dosya veritabanına değil S3 gibi bir depolamaya konur, veritabanında sadece adresi tutulur.
- **Testcontainers:** Testler H2 ile çalışıyor. Gerçek PostgreSQL ile test için Testcontainers kullanılabilir (Docker gerektirir).
- **Yorum güncelleme/silme, kullanıcı profili güncelleme:** İstenmediği için eklenmedi.

---

## 12. Projeyi çalıştırma

```bash
cp .env.example .env            # şifreleri ve JWT_SECRET'ı doldur
docker compose up -d --build    # her şeyi başlat
# http://localhost:8080/swagger-ui.html
```

Swagger'da önce `/api/auth/register` ile kayıt ol, dönen `accessToken`'ı sağ üstteki **Authorize** butonuna yapıştır. Sonra korumalı endpoint'leri doğrudan deneyebilirsin.
