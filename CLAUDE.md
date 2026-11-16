# LezzetKodu API - Claude Kuralları

## Kod yazma kuralları
- Sadece istenen işi yap. İstenmeyen özellik, endpoint veya ayar ekleme.
- Gelecekte gerekeceği belli olan bir şey varsa onu implement etme; kodu ona engel olmayacak şekilde kur ve bir satırlık not düş.
- Emin değilsen eklemeden önce sor.
- Tüm dosyayı yeniden yazma, sadece değişen kısmı ver.

## Proje
- Java 21, Spring Boot 3.5, PostgreSQL, Flyway, JWT.
- Paketler özelliğe göre: auth, recipe, review, favorite, category, user.
- Şema değişikliği yeni bir Flyway dosyasıyla yapılır (V3__...sql), eski dosyalara dokunulmaz.
- Testler: mvn test
