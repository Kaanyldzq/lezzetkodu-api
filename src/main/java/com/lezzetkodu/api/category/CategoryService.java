package com.lezzetkodu.api.category;

import com.lezzetkodu.api.category.CategoryDtos.CategoryRequest;
import com.lezzetkodu.api.category.CategoryDtos.CategoryResponse;
import com.lezzetkodu.api.common.ConflictException;
import com.lezzetkodu.api.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Veritabaninin varsayilan siralamasi Turkce harfleri (C, I, O, S, U) yanlis yere koyar; Java'da Turkce kurallarla siraliyoruz.
    private static final Collator TURKISH = Collator.getInstance(Locale.forLanguageTag("tr-TR"));

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, TURKISH))
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Bu isimde bir kategori zaten var: " + name);
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name)));
    }

    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kategori bulunamadı: " + id));
    }
}
