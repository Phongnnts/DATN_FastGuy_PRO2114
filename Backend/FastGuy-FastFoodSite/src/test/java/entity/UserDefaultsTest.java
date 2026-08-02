package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserDefaultsTest {
    @Test
    void favoritesDefaultForNewAndPrePersistedUsers() {
        User user = new User();
        assertEquals("[]", user.getFavoriteIdsJson());
        user.setFavoriteIdsJson(" ");
        user.prePersist();
        assertEquals("[]", user.getFavoriteIdsJson());
    }
}
