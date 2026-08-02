package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuestReturnProofTest {
    @Test
    void storesHashAndVerifiesOnlyOriginalToken() {
        String token = GuestReturnProof.generate();
        String hash = GuestReturnProof.hash(token);

        assertNotEquals(token, hash);
        assertTrue(GuestReturnProof.verify(token, hash));
        assertFalse(GuestReturnProof.verify(token + "x", hash));
        assertFalse(GuestReturnProof.verify(null, hash));
    }
}
