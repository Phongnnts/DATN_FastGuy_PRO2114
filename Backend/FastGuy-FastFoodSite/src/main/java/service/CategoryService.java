package service;

import dto.CategoryDTO;
import repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryService {
    private final CategoryRepository categoryRepository = new CategoryRepository();

    public List<CategoryDTO> getAllActive() {
        return categoryRepository.findAllActive().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CategoryDTO toDTO(entity.Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setStatus(category.getStatus());
        return dto;
    }
}
