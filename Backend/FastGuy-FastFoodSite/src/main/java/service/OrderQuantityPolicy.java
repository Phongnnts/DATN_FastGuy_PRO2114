package service;

import java.util.Map;

public final class OrderQuantityPolicy {

    public static final int MAX_PER_PRODUCT = 20;
    public static final String MESSAGE =
        "Mỗi sản phẩm chỉ được đặt tối đa 20 cái để đảm bảo đơn hàng hợp lệ.";

    private OrderQuantityPolicy() {}

    public static boolean allows(int quantity) {
        return quantity >= 0 && quantity <= MAX_PER_PRODUCT;
    }

    public static void require(Map<Integer, Integer> quantities) {
        if (
            quantities
                .values()
                .stream()
                .anyMatch(quantity -> !allows(quantity))
        ) throw new IllegalArgumentException(MESSAGE);
    }
}
