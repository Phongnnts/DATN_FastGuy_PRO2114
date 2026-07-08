package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PayOSService;
import utils.ApiResponse;
import utils.JsonUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/payment/payos-webhook")
public class PayOSWebhookServlet extends HttpServlet {
    private PayOSService payOSService = new PayOSService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        boolean ok = payOSService.handleWebhook(body);
        if (ok) {
            ApiResponse.ok(resp, null, "Payment confirmed");
        } else {
            ApiResponse.error(resp, "Processing failed", 500);
        }
    }
}
