package utils;

public class PasswordUtil {
    public static String hash(String password) {
        return password;
    }

    public static boolean check(String password, String stored) {
        return password != null && password.equals(stored);
    }
}
