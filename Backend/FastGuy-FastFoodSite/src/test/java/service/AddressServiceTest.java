package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import utils.AddressValidator;

class AddressValidatorTest {

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Nguyen Van A");
        body.put("phone", "0912345678");
        body.put("street", "123 Nguyen Hue");
        body.put("wardName", "Ben Nghe");
        body.put("districtName", "Quan 1");
        body.put("provinceName", "TP. Ho Chi Minh");
        body.put("ghnProvinceId", 202);
        body.put("ghnDistrictId", 1442);
        body.put("ghnWardCode", "20107");
        return body;
    }

    @Test
    @DisplayName("Valid address returns null")
    void validAddress() {
        assertEquals(null, AddressValidator.validate(validBody()));
    }

    @Test
    @DisplayName("Short recipient name returns error")
    void shortRecipient() {
        Map<String, Object> body = validBody();
        body.put("recipientName", "A");
        assertEquals("Ten nguoi nhan phai tu 2 den 100 ky tu", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Invalid phone returns error")
    void invalidPhone() {
        Map<String, Object> body = validBody();
        body.put("phone", "123");
        assertEquals("So dien khong khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Empty street returns error")
    void emptyStreet() {
        Map<String, Object> body = validBody();
        body.put("street", "123");
        assertEquals("So nha/ten duong phai tu 5 den 255 ky tu", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Missing GHN province returns error")
    void missingGhnProvince() {
        Map<String, Object> body = validBody();
        body.put("ghnProvinceId", -1);
        assertEquals("Tinh/thanh pho GHN khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Missing GHN district returns error")
    void missingGhnDistrict() {
        Map<String, Object> body = validBody();
        body.remove("ghnDistrictId");
        assertEquals("Quan/huyen GHN khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Non-string GHN ward code returns error")
    void nonStringGhnWard() {
        Map<String, Object> body = validBody();
        body.put("ghnWardCode", 20107);
        assertEquals("Phuong/xa GHN khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Empty GHN ward code returns error")
    void emptyGhnWard() {
        Map<String, Object> body = validBody();
        body.put("ghnWardCode", "");
        assertEquals("Phuong/xa GHN khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("+84 phone format is valid")
    void plus84Phone() {
        Map<String, Object> body = validBody();
        body.put("phone", "+84912345678");
        assertEquals(null, AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Validation priority: recipient checked first")
    void validationPriority() {
        Map<String, Object> body = validBody();
        body.put("recipientName", "X");
        body.put("phone", "bad");
        assertEquals("Ten nguoi nhan phai tu 2 den 100 ky tu", AddressValidator.validate(body));
    }
}
