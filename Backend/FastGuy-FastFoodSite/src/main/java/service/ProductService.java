package service;

import dto.ProductDTO;
import dto.ProductOptionDTO;
import entity.Product;
import exception.ResourceNotFoundException;
import repository.ProductRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductRepository productRepository = new ProductRepository();

    public List<ProductDTO> getAllAvailable() {
        return productRepository.findAllAvailable().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        return productRepository.searchByName(keyword).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return toDTO(product);
    }

    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setCategoryId(product.getCategory().getCategoryId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setStatus(product.getStatus());
        if (product.getOptions() != null) {
            dto.setOptions(product.getOptions().stream().map(opt -> {
                ProductOptionDTO optDTO = new ProductOptionDTO();
                optDTO.setOptionId(opt.getOptionId());
                optDTO.setOptionName(opt.getOptionName());
                optDTO.setExtraPrice(opt.getExtraPrice());
                optDTO.setStockControlled(opt.getStockControlled());
                optDTO.setQuantityAvailable(opt.getQuantityAvailable());
                return optDTO;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
