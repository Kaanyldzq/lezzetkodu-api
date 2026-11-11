package com.lezzetkodu.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uygulamayi bastan sona (controller -> servis -> veritabani -> guvenlik) birlikte test eder.
 * Her test kendi benzersiz kullanicilarini olusturur, boylece testler birbirini etkilemez.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---------- Kimlik dogrulama ----------

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String username = uniqueName();
        register(username);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"gizli-sifre-123"}""".formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        String username = uniqueName();
        register(username);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, "baska-" + username + "@test.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String username = uniqueName();
        register(username);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"yanlis-sifre"}""".formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Kullanıcı adı veya şifre hatalı"));
    }

    @Test
    void invalidRegisterReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"a","email":"bu-eposta-degil","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ---------- Yetkilendirme ----------

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(recipeBody("Deneme")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Giriş yapmanız gerekiyor"));
    }

    @Test
    void tamperedTokenReturns401() throws Exception {
        String token = register(uniqueName()) + "bozuk";
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminCanCreateCategory() throws Exception {
        String userToken = register(uniqueName());
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Deniz Ürünleri"}"""))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "admin-test-123");
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Deniz Ürünleri"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Deniz Ürünleri"));
    }

    // ---------- Tarifler ----------

    @Test
    void recipeCrudFlow() throws Exception {
        String token = register(uniqueName());

        // Olustur: 201 + Location basligi + malzemeler sirali
        MvcResult created = mvc.perform(post("/api/recipes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(recipeBody("Mercimek Çorbası")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/recipes/")))
                .andExpect(jsonPath("$.title").value("Mercimek Çorbası"))
                .andExpect(jsonPath("$.ingredients", hasSize(2)))
                .andExpect(jsonPath("$.ingredients[0].name").value("Kırmızı mercimek"))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andReturn();
        long id = idOf(created);

        // Herkese acik detay ve liste (token'siz)
        mvc.perform(get("/api/recipes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").exists());
        mvc.perform(get("/api/recipes").param("q", "mercimek"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Mercimek Çorbası"));

        // Guncelle: malzeme listesi tamamen degisir
        mvc.perform(put("/api/recipes/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Süzme Mercimek Çorbası","instructions":"Pişir ve blenderdan geçir.",
                                 "prepMinutes":40,"servings":6,"difficulty":"MEDIUM","categoryId":1,
                                 "ingredients":[{"name":"Mercimek","quantity":"2 su bardağı"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Süzme Mercimek Çorbası"))
                .andExpect(jsonPath("$.ingredients", hasSize(1)));

        // Sil
        mvc.perform(delete("/api/recipes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUserCannotEditRecipe() throws Exception {
        String owner = register(uniqueName());
        String stranger = register(uniqueName());
        long id = createRecipe(owner, "Karnıyarık");

        mvc.perform(put("/api/recipes/" + id).header("Authorization", "Bearer " + stranger)
                        .contentType(MediaType.APPLICATION_JSON).content(recipeBody("Ele geçirildi")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/recipes/" + id).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownCategoryReturns404() throws Exception {
        String token = register(uniqueName());
        mvc.perform(post("/api/recipes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recipeBody("Deneme").replace("\"categoryId\":1", "\"categoryId\":9999")))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidDifficultyReturns400() throws Exception {
        mvc.perform(get("/api/recipes").param("difficulty", "COK_ZOR"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Favoriler ve yorumlar ----------

    @Test
    void favoritesAreIdempotent() throws Exception {
        String owner = register(uniqueName());
        String fan = register(uniqueName());
        long id = createRecipe(owner, "Baklava");

        // Iki kez eklemek hata vermez, listede bir kez gorunur
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/recipes/" + id + "/favorite").header("Authorization", "Bearer " + fan))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get("/api/me/favorites").header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Baklava"));

        mvc.perform(delete("/api/recipes/" + id + "/favorite").header("Authorization", "Bearer " + fan))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me/favorites").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void reviewsUpdateAverageAndBlockDuplicates() throws Exception {
        String owner = register(uniqueName());
        String a = register(uniqueName());
        String b = register(uniqueName());
        long id = createRecipe(owner, "Menemen");

        review(a, id, 5).andExpect(status().isCreated());
        review(b, id, 4).andExpect(status().isCreated());
        review(a, id, 1).andExpect(status().isConflict());     // ayni kullanici ikinci kez
        review(owner, id, 5).andExpect(status().isConflict()); // kendi tarifine puan

        mvc.perform(get("/api/recipes/" + id))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviewCount").value(2));
        mvc.perform(get("/api/recipes/" + id + "/reviews"))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ---------- Yardimcilar ----------

    private org.springframework.test.web.servlet.ResultActions review(String token, long recipeId, int rating)
            throws Exception {
        return mvc.perform(post("/api/recipes/" + recipeId + "/reviews").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rating":%d,"comment":"Güzel olmuş"}""".formatted(rating)));
    }

    private String register(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, username + "@test.com")))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createRecipe(String token, String title) throws Exception {
        MvcResult result = mvc.perform(post("/api/recipes").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(recipeBody(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private static String registerBody(String username, String email) {
        return """
                {"username":"%s","email":"%s","password":"gizli-sifre-123"}""".formatted(username, email);
    }

    private static String recipeBody(String title) {
        return """
                {"title":"%s","description":"Ev usulü","instructions":"Malzemeleri haşla, blenderdan geçir.",
                 "prepMinutes":30,"servings":4,"difficulty":"EASY","categoryId":1,
                 "ingredients":[{"name":"Kırmızı mercimek","quantity":"1 su bardağı"},
                                {"name":"Soğan","quantity":"1 adet"}]}""".formatted(title);
    }

    private static String uniqueName() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }
}
