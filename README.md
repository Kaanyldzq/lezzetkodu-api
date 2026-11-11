# LezzetKodu API

LezzetKodu tarif uygulaması için REST API. Kullanıcılar kayıt olup giriş yapar, tarif paylaşır, başkalarının tariflerini favoriler ve puanlar.

**Teknolojiler:** Java 21, Spring Boot 3.5, Spring Security + JWT, Spring Data JPA (Hibernate), PostgreSQL 16, Flyway, Swagger/OpenAPI, Docker.

## Hızlı başlangıç (Docker)

```bash
cp .env.example .env          # sonra .env içindeki şifreleri ve JWT_SECRET'ı doldur
openssl rand -base64 32       # JWT_SECRET için
docker compose up -d --build  # API + PostgreSQL ayağa kalkar
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Durdurmak: `docker compose down` (veriler `pgdata` volume'unda kalır; silmek için `docker compose down -v`).

## Docker olmadan çalıştırma

PostgreSQL'de `lezzetkodu` adında bir veritabanı ve kullanıcı oluşturduktan sonra:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

Testler (PostgreSQL gerekmez, bellek içi H2 kullanır):

```bash
mvn test
```

## Veritabanı şeması

```
users ──< recipes >── categories
  │          │
  │          ├──< recipe_ingredients
  │          │
  ├──< favorites >──┤      (kullanıcı-tarif çifti, birleşik anahtar)
  └──< reviews  >───┘      (kullanıcı başına tarif başına 1 yorum, puan 1-5)
```

Şema Flyway ile `src/main/resources/db/migration` altındaki SQL dosyalarından kurulur. Hibernate şemayı değiştirmez, sadece uyumluluğu kontrol eder (`ddl-auto: validate`).

## Endpoint'ler

| Metot | Adres | Yetki | Açıklama |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Herkes | Kayıt olur, token döner |
| POST | `/api/auth/login` | Herkes | Giriş yapar, token döner |
| GET | `/api/me` | Giriş yapmış | Profil bilgisi |
| GET | `/api/me/favorites` | Giriş yapmış | Favori tarifler |
| GET | `/api/categories` | Herkes | Kategoriler |
| POST | `/api/categories` | Admin | Kategori ekler |
| GET | `/api/recipes` | Herkes | Liste. Filtreler: `categoryId`, `difficulty`, `q`, `authorId`, `page`, `size` |
| GET | `/api/recipes/{id}` | Herkes | Detay (malzemeler, puan ortalaması) |
| POST | `/api/recipes` | Giriş yapmış | Tarif oluşturur |
| PUT | `/api/recipes/{id}` | Sahibi / admin | Tarifi günceller |
| DELETE | `/api/recipes/{id}` | Sahibi / admin | Tarifi siler |
| PUT | `/api/recipes/{id}/favorite` | Giriş yapmış | Favorilere ekler |
| DELETE | `/api/recipes/{id}/favorite` | Giriş yapmış | Favorilerden çıkarır |
| GET | `/api/recipes/{id}/reviews` | Herkes | Yorumlar |
| POST | `/api/recipes/{id}/reviews` | Giriş yapmış | Yorum + puan ekler |

Korumalı istekler `Authorization: Bearer <token>` başlığıyla gönderilir.

### Örnek

```bash
# Kayıt ol ve token'ı al
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"kaan","email":"kaan@example.com","password":"gizli-sifre-123"}' | jq -r .accessToken)

# Tarif oluştur
curl -X POST localhost:8080/api/recipes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Mercimek Çorbası","instructions":"Haşla, blenderdan geçir.","prepMinutes":30,
       "servings":4,"difficulty":"EASY","categoryId":1,
       "ingredients":[{"name":"Kırmızı mercimek","quantity":"1 su bardağı"}]}'
```

### Hata biçimi

Bütün hatalar aynı JSON biçiminde döner:

```json
{
  "timestamp": "2026-11-11T13:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Gönderilen veriler geçersiz",
  "path": "/api/auth/register",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

| Kod | Ne zaman |
| --- | --- |
| 400 | Geçersiz veri veya parametre |
| 401 | Token yok, geçersiz veya süresi dolmuş; hatalı giriş |
| 403 | Token geçerli ama yetki yok (başkasının tarifi, admin işlemi) |
| 404 | Kayıt bulunamadı |
| 409 | Çakışma (alınmış kullanıcı adı, ikinci yorum) |

## Proje yapısı

Kod katmana göre değil özelliğe göre paketlenmiştir; her paket kendi controller, servis, repository ve DTO'larını içerir:

```
com.lezzetkodu.api
├── auth       kayıt / giriş
├── user       kullanıcı, /api/me, admin oluşturma
├── category   kategoriler
├── recipe     tarifler ve malzemeler
├── favorite   favoriler
├── review     yorumlar ve puanlar
├── security   JWT, filtre, güvenlik ayarları
├── common     ortak hata yönetimi ve sayfalama
└── config     Swagger ayarları
```
