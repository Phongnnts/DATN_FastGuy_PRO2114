package utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SePayConfig {
    private static final String BANK_ACCOUNT = "19074734124014";
    private static final String BANK_CODE = "Techcombank";
    private static final String QR_TEMPLATE = "compact";

    public static String buildQrUrl(long amount, String description) {
        return "https://vietqr.app/img"
            + "?acc=" + BANK_ACCOUNT
            + "&bank=" + BANK_CODE
            + "&amount=" + amount
            + "&des=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
            + "&template=" + QR_TEMPLATE
            + "&showinfo=true";
    }
}
