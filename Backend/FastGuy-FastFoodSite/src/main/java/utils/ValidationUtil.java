package utils;

import exception.BadRequestException;

public class ValidationUtil {

    public static void notBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " không được để trống");
        }
    }

    public static void minLength(String value, int min, String fieldName) {
        if (value != null && value.trim().length() < min) {
            throw new BadRequestException(fieldName + " phải có ít nhất " + min + " ký tự");
        }
    }

    public static void isPhone(String phone) {
        if (phone == null || !phone.matches("^0\\d{9,10}$")) {
            throw new BadRequestException("Số điện thoại không hợp lệ (phải 10-11 số, bắt đầu bằng 0)");
        }
    }

    public static void isEmail(String email) {
        if (email != null && !email.trim().isEmpty()
                && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("Email không hợp lệ");
        }
    }

    public static void positive(long value, String fieldName) {
        if (value <= 0) {
            throw new BadRequestException(fieldName + " phải lớn hơn 0");
        }
    }

    public static void notNull(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " không được để trống");
        }
    }
}
