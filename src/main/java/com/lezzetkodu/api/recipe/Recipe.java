package com.lezzetkodu.api.recipe;

import com.lezzetkodu.api.category.Category;
import com.lezzetkodu.api.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "prep_minutes", nullable = false)
    private int prepMinutes;

    @Column(nullable = false)
    private int servings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    // LAZY: kategori ve yazar sadece gerektiginde yuklenir. Listede ihtiyac olunca EntityGraph ile tek sorguda alinir.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // Malzemeler tarifin parcasi: tarifle birlikte kaydedilir, listeden cikarilan malzeme silinir (orphanRemoval).
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recipe() {
    }

    public Recipe(User author) {
        this.author = author;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Olusturma ve guncellemede ayni alanlar dolduruldugu icin tek metot. */
    public void update(String title, String description, String instructions, int prepMinutes,
                       int servings, Difficulty difficulty, Category category) {
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.prepMinutes = prepMinutes;
        this.servings = servings;
        this.difficulty = difficulty;
        this.category = category;
    }

    /** Malzeme listesini yenisiyle degistirir; siralamayi gelen listenin sirasina gore verir. */
    public void replaceIngredients(List<IngredientData> newIngredients) {
        ingredients.clear();
        for (int i = 0; i < newIngredients.size(); i++) {
            IngredientData data = newIngredients.get(i);
            ingredients.add(new RecipeIngredient(this, data.name(), data.quantity(), i));
        }
    }

    public boolean isOwnedBy(Long userId) {
        return author.getId().equals(userId);
    }

    public record IngredientData(String name, String quantity) {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        return instructions;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public int getServings() {
        return servings;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Category getCategory() {
        return category;
    }

    public User getAuthor() {
        return author;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
