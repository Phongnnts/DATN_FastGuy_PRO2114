package service;

import dto.AddToCartRequest;
import dto.CartDTO;
import dto.CartItemDTO;
import dto.MigrateCartRequest;
import entity.Cart;
import entity.CartItem;
import entity.Product;
import entity.User;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import repository.CartRepository;
import repository.ProductRepository;
import repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import utils.ValidationUtil;
import java.util.Optional;
import java.util.stream.Collectors;

public class CartService {
    private final CartRepository cartRepository = new CartRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final UserRepository userRepository = new UserRepository();

    public CartDTO getCart(Long userId) {
        Cart cart = findOrCreateCart(userId);
        return toDTO(cart);
    }

    public CartDTO addItem(Long userId, AddToCartRequest req) {
        ValidationUtil.notNull(req.getProductId(), "Sản phẩm");
        ValidationUtil.positive(req.getQuantity(), "Số lượng");

        Cart cart = findOrCreateCart(userId);
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        if (!"AVAILABLE".equals(product.getStatus())) {
            throw new BadRequestException("Sản phẩm hiện không khả dụng");
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(req.getProductId())
                        && isSameOptions(item.getOptionData(), req.getOptionData()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            CartItem item = new CartItem(cart, product, req.getQuantity(),
                    req.getOptionData(), product.getPrice());
            if (cart.getItems() == null) cart.setItems(new ArrayList<>());
            cart.getItems().add(item);
        }

        cart = cartRepository.save(cart);
        return toDTO(cart);
    }

    public CartDTO updateItemQuantity(Long userId, Long cartItemId, Integer quantity) {
        ValidationUtil.positive(quantity, "Số lượng");

        Cart cart = findOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        item.setQuantity(quantity);
        cart = cartRepository.save(cart);
        return toDTO(cart);
    }

    public CartDTO removeItem(Long userId, Long cartItemId) {
        Cart cart = findOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getCartItemId().equals(cartItemId));
        cart = cartRepository.save(cart);
        return toDTO(cart);
    }

    public CartDTO migrateCart(Long userId, MigrateCartRequest req) {
        Cart cart = findOrCreateCart(userId);
        if (req.getItems() != null) {
            for (AddToCartRequest itemReq : req.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));
                CartItem item = new CartItem(cart, product, itemReq.getQuantity(),
                        itemReq.getOptionData(), product.getPrice());
                if (cart.getItems() == null) cart.setItems(new ArrayList<>());
                cart.getItems().add(item);
            }
        }
        cart = cartRepository.save(cart);
        return toDTO(cart);
    }

    private Cart findOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    private boolean isSameOptions(String opt1, String opt2) {
        if (opt1 == null && opt2 == null) return true;
        if (opt1 == null || opt2 == null) return false;
        return opt1.equals(opt2);
    }

    private CartDTO toDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getCartId());
        if (cart.getItems() != null) {
            List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
                CartItemDTO itemDTO = new CartItemDTO();
                itemDTO.setCartItemId(item.getCartItemId());
                itemDTO.setProductId(item.getProduct().getProductId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setImageUrl(item.getProduct().getImageUrl());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setOptionData(item.getOptionData());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                return itemDTO;
            }).collect(Collectors.toList());
            dto.setItems(itemDTOs);
            dto.setTotalPrice(itemDTOs.stream()
                    .map(CartItemDTO::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return dto;
    }
}
