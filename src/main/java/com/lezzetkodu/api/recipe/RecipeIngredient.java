package com.lezzetkodu.api.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipe_ingredients")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false, length = 100)
    private String name;

    // "2 su bardagi", "1 tutam" gibi serbest metin oldugu icin String.
    @Column(nullable = false, length = 50)
    private String quantity;

    // Malzemelerin listede hangi sirada gosterilecegi.
    @Column(nullable = false)
    private int position;

    protected RecipeIngredient() {
    }

    RecipeIngredient(Recipe recipe, String name, String quantity, int position) {
        this.recipe = recipe;
        this.name = name;
        this.quantity = quantity;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getQuantity() {
        return quantity;
    }

    public int getPosition() {
        return position;
    }
}
