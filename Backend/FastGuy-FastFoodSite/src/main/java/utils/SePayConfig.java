package utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SePayConfig {
    private static final String BANK_ACCOUNT = "6513527";
    private static final String BANK_CODE = "MB";
    private static final String QR_TEMPLATE = "compact";

    public static String buildQrUrl(long amount, String description) {
        return "https://qr.sepay.vn/img"
            + "?acc=" + BANK_ACCOUNT
            + "&bank=" + BANK_CODE
            + "&amount=" + amount
            + "&des=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
            + "&template=" + QR_TEMPLATE;
    }
}
