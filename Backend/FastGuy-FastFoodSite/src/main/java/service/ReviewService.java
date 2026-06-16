package service;

import dto.ReviewDTO;
import entity.Orders;
import entity.Product;
import entity.Review;
import entity.User;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import utils.ValidationUtil;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.ReviewRepository;
import repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ReviewService {
    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final ProductRepository productRepository = new ProductRepository();

    public List<ReviewDTO> getByProduct(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ReviewDTO createOrderReview(Long userId, Long orderId, Integer rating, String comment) {
        ValidationUtil.notNull(orderId, "Đơn hàng");
        ValidationUtil.notNull(rating, "Đánh giá");

        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Đánh giá phải từ 1 đến 5 sao");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn hàng đã giao");
        }
        if (!order.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Không thể đánh giá đơn hàng của người khác");
        }

        boolean hasOrderReview = reviewRepository.findAll().stream()
                .anyMatch(r -> r.getOrder().getOrderId().equals(orderId)
                        && r.getUser().getUserId().equals(userId)
                        && r.getProduct() == null);
        if (hasOrderReview) {
            throw new BadRequestException("Bạn đã đánh giá đơn hàng này rồi");
        }

        Review review = new Review();
        review.setUser(user);
        review.setOrder(order);
        review.setProduct(null);
        review.setRating(rating);
        review.setComment(comment);
        review = reviewRepository.save(review);
        return toDTO(review);
    }

    public ReviewDTO create(Long userId, ReviewDTO dto) {
        ValidationUtil.notNull(dto.getOrderId(), "Đơn hàng");
        ValidationUtil.notNull(dto.getRating(), "Đánh giá");

        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BadRequestException("Đánh giá phải từ 1 đến 5 sao");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Orders order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", dto.getOrderId()));

        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn hàng đã giao");
        }
        if (!order.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Không thể đánh giá đơn hàng của người khác");
        }
        if (reviewRepository.existsByOrderAndUser(dto.getOrderId(), userId)) {
            throw new BadRequestException("Bạn đã đánh giá đơn hàng này rồi");
        }

        Review review = new Review();
        review.setUser(user);
        review.setOrder(order);
        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", dto.getProductId()));
            review.setProduct(product);
        }
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review = reviewRepository.save(review);
        return toDTO(review);
    }

    private ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(review.getReviewId());
        dto.setUserId(review.getUser().getUserId());
        dto.setFullName(review.getUser().getFullName());
        dto.setAvatarUrl(review.getUser().getAvatarUrl());
        dto.setOrderId(review.getOrder().getOrderId());
        dto.setProductId(review.getProduct() != null ? review.getProduct().getProductId() : null);
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
