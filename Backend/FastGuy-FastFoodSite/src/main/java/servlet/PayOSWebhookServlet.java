package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PayOSPaymentService;
import utils.ApiResponse;
import utils.JsonUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payment/payos-webhook")
public class PayOSWebhookServlet extends HttpServlet {
    private final PayOSPaymentService payOSPaymentService = new PayOSPaymentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Map<String, Object> payload = JsonUtil.fromJson(req.getReader(), Map.class);
        if (payload == null) {
            ApiResponse.error(resp, "Invalid payment webhook", 400);
            return;
        }
        if (payOSPaymentService.processWebhook(payload)) {
            ApiResponse.ok(resp, null, "Payment confirmed");
            return;
        }
        if (payOSPaymentService.isValidWebhook(payload)) {
            ApiResponse.ok(resp, null, "Webhook verified");
            return;
        }
        ApiResponse.error(resp, "Invalid payment webhook", 400);
    }
}
