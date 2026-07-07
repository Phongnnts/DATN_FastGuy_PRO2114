package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.SePayService;
import utils.ApiResponse;
import utils.AppConfig;
import utils.JsonUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payment/sepay-webhook")
public class SePayWebhookServlet extends HttpServlet {
    private SePayService sePayService = new SePayService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String secret = AppConfig.getSePayWebhookSecret();
        if (secret != null && !secret.isBlank()) {
            String provided = req.getHeader("X-SePay-Webhook-Secret");
            if (provided == null || !secret.equals(provided)) {
                ApiResponse.error(resp, "Invalid webhook secret", 401);
                return;
            }
        }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        boolean ok = sePayService.processWebhook(body);
        if (ok) {
            ApiResponse.ok(resp, null, "Payment confirmed");
        } else {
            ApiResponse.error(resp, "Processing failed", 500);
        }
    }
}
