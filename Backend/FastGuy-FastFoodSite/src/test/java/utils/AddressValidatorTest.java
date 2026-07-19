package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class AddressValidatorTest {

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Nguyen Van A");
        body.put("phone", "0912345678");
        body.put("street", "123 Nguyen Hue");
        body.put("wardName", "Ben Nghé");
        body.put("districtName", "Quan 1");
        body.put("provinceName", "TP. Ho Chi Minh");
        body.put("ghnProvinceId", 202);
        body.put("ghnDistrictId", 1442);
        body.put("ghnWardCode", "20107");
        return body;
    }

    @Test
    @DisplayName("Valid address returns null (no error)")
    void validAddress_noError() {
        assertNull(AddressValidator.validate(validBody()));
    }

    @Test
    @DisplayName("Recipient name too short returns error")
    void shortRecipientName_returnsError() {
        Map<String, Object> body = validBody();
        body.put("recipientName", "A");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("Ten nguoi nhan phai tu 2 den 100 ky tu", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Recipient name too long returns error")
    void longRecipientName_returnsError() {
        Map<String, Object> body = validBody();
        body.put("recipientName", "A".repeat(101));
        assertNotNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Invalid phone format returns error")
    void invalidPhone_returnsError() {
        Map<String, Object> body = validBody();
        body.put("phone", "12345");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("So dien khong khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Phone with wrong prefix returns error")
    void wrongPrefixPhone_returnsError() {
        Map<String, Object> body = validBody();
        body.put("phone", "0123456789");
        assertNotNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("+84 phone format is valid")
    void plus84Phone_valid() {
        Map<String, Object> body = validBody();
        body.put("phone", "+84912345678");
        assertNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Street too short returns error")
    void shortStreet_returnsError() {
        Map<String, Object> body = validBody();
        body.put("street", "123");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("So nha/ten duong phai tu 5 den 255 ky tu", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Street too long returns error")
    void longStreet_returnsError() {
        Map<String, Object> body = validBody();
        body.put("street", "X".repeat(256));
        assertNotNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Empty ward returns error")
    void emptyWard_returnsError() {
        Map<String, Object> body = validBody();
        body.put("wardName", "");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("Phuong/xa khong duoc de trong", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Empty district returns error")
    void emptyDistrict_returnsError() {
        Map<String, Object> body = validBody();
        body.put("districtName", "");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("Quan/huyen khong duoc de trong", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Empty province returns error")
    void emptyProvince_returnsError() {
        Map<String, Object> body = validBody();
        body.put("provinceName", "");
        assertNotNull(AddressValidator.validate(body));
        assertEquals("Tinh/thanh pho khong duoc de trong", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Null recipient name returns error")
    void nullRecipientName_returnsError() {
        Map<String, Object> body = validBody();
        body.put("recipientName", null);
        assertNotNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Whitespace-only phone returns error")
    void whitespacePhone_returnsError() {
        Map<String, Object> body = validBody();
        body.put("phone", "   ");
        assertNotNull(AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Validation field priority: recipientName checked first")
    void validationPriority_recipientFirst() {
        Map<String, Object> body = validBody();
        body.put("recipientName", "X");
        body.put("phone", "invalid");
        String error = AddressValidator.validate(body);
        assertEquals("Ten nguoi nhan phai tu 2 den 100 ky tu", error);
    }

    @Test
    @DisplayName("Missing GHN location returns error")
    void missingGhnLocation_returnsError() {
        Map<String, Object> body = validBody();
        body.remove("ghnDistrictId");
        assertEquals("Quan/huyen GHN khong hop le", AddressValidator.validate(body));
    }

    @Test
    @DisplayName("Invalid GHN ward type returns error")
    void invalidGhnWardType_returnsError() {
        Map<String, Object> body = validBody();
        body.put("ghnWardCode", 20107);
        assertEquals("Phuong/xa GHN khong hop le", AddressValidator.validate(body));
    }
}
