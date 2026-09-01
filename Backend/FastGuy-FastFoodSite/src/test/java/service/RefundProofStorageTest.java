package service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RefundProofStorageTest {
    @Test void acceptsJpegPngAndWebpMagicWithinFiveMiB() {
        assertDoesNotThrow(() -> RefundProofStorage.validate(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,1}, "image/jpeg"));
        assertDoesNotThrow(() -> RefundProofStorage.validate(new byte[]{(byte)0x89,0x50,0x4e,0x47,13,10,26,10}, "image/png"));
        assertDoesNotThrow(() -> RefundProofStorage.validate("RIFF0000WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII), "image/webp"));
    }

    @Test void proofIdentityIsStableForSameOrderAndContent() {
        byte[] content = new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff};
        assertEquals(RefundProofStorage.publicId("9", content), RefundProofStorage.publicId("9", content));
        assertNotEquals(RefundProofStorage.publicId("9", content), RefundProofStorage.publicId("10", content));
    }

    @Test void signedPrivateDownloadUsesCloudinaryAuthenticatedDownloadParameters() {
        RefundProofStorage storage = new RefundProofStorage(java.net.http.HttpClient.newHttpClient(), "demo", "key", "secret");
        RefundProofStorage.SignedProofUrl value = storage.signedViewUrl("fastguy/refunds/9-hash", "image/png", java.time.Duration.ofMinutes(5));
        assertTrue(value.viewUrl().startsWith("https://api.cloudinary.com/v1_1/demo/image/download?"));
        for (String token : new String[]{"expires_at=", "format=png", "public_id=fastguy/refunds/9-hash", "timestamp=", "type=authenticated", "api_key=key", "signature="}) assertTrue(value.viewUrl().contains(token), token);
        assertFalse(value.viewUrl().contains("secret"));
    }

    @Test void rejectsWrongTypeMagicEmptyAndOversize() {
        assertThrows(IllegalArgumentException.class, () -> RefundProofStorage.validate(new byte[0], "image/png"));
        assertThrows(IllegalArgumentException.class, () -> RefundProofStorage.validate(new byte[]{1,2,3}, "image/jpeg"));
        assertThrows(IllegalArgumentException.class, () -> RefundProofStorage.validate(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff}, "image/gif"));
        byte[] large = new byte[RefundProofStorage.MAX_BYTES + 1];
        Arrays.fill(large, (byte)1);
        assertThrows(IllegalArgumentException.class, () -> RefundProofStorage.validate(large, "image/png"));
    }
}
