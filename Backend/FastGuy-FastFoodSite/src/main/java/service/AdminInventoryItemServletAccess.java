package service;

import java.math.BigDecimal;

final class AdminInventoryItemServletAccess {

    private AdminInventoryItemServletAccess() {}

    static int positiveInt(Object value, String name) {
        if (
            !(value instanceof Number number)
        ) throw new IllegalArgumentException("Invalid " + name);
        try {
            BigDecimal decimal = new BigDecimal(number.toString());
            if (
                decimal.stripTrailingZeros().scale() > 0 ||
                decimal.compareTo(BigDecimal.ONE) < 0 ||
                decimal.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0
            ) throw new IllegalArgumentException("Invalid " + name);
            return decimal.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid " + name, e);
        }
    }
}
