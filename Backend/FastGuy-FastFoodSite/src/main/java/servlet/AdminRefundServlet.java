package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import service.RefundProofStorage;
import service.RefundService;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.PrivilegedAuth;

@WebServlet("/api/admin/refunds/*")
@MultipartConfig(maxFileSize = RefundProofStorage.MAX_BYTES, maxRequestSize = RefundProofStorage.MAX_BYTES + 65536)
public class AdminRefundServlet extends HttpServlet {
    private RefundService refundService = new RefundService();
    private RefundProofStorage proofStorage;
    public AdminRefundServlet() {}
    AdminRefundServlet(RefundService refundService, RefundProofStorage proofStorage) { this.refundService = refundService; this.proofStorage = proofStorage; }
    private OrdersDAO ordersDAO = new OrdersDAO();
    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        String token = header.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token)) || !PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), "ADMIN")) { ApiResponse.error(resp, "Forbidden", 403); return; }

        try {
            String path = req.getPathInfo();
            if (path != null && path.matches("/[1-9]\\d*/proof-url")) {
                int orderId = Integer.parseInt(path.substring(1, path.indexOf('/', 1)));
                RefundProofStorage.SignedProofUrl value = refundService.proofViewUrl(orderId, storage());
                ApiResponse.ok(resp, Map.of("viewUrl", value.viewUrl(), "expiresAt", value.expiresAt().toString()));
                return;
            }
            if (path != null && !"/".equals(path)) { ApiResponse.error(resp, "Not found", 404); return; }
            LocalDate from = toLocalDate(req.getParameter("fromDate"));
            LocalDate to = toLocalDate(req.getParameter("toDate"));
            if (from != null && to != null && from.isAfter(to)) {
                ApiResponse.error(resp, "fromDate must not be after toDate", 400);
                return;
            }
            String status = req.getParameter("status");
            if (status != null && !status.isBlank() && !Set.of("PENDING","REFUNDED","REJECTED").contains(status)) throw new IllegalArgumentException("Invalid refund status");
            List<Orders> pending = ordersDAO.findRefunds(status, from, to, req.getParameter("search"));
            Map<Integer, String> processorNames = resolveProcessorNames(pending, userDAO::findByIds);
            List<Map<String, Object>> result = pending.stream().map(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderId", o.getOrderId());
                m.put("orderCode", o.getOrderCode());
                m.put("customerName", o.getCustomerName());
                m.put("customerPhone", o.getCustomerPhone());
                m.put("finalAmount", o.getFinalAmount());
                m.put("paymentMethod", o.getPaymentMethod());
                m.put("paymentStatus", o.getPaymentStatus());
                m.put("refundStatus", o.getRefundStatus());
                m.put("refundAmount", o.getRefundAmount());
                m.put("refundNote", o.getRefundNote());
                m.put("refundReference", o.getRefundReference());
                m.put("proofAvailable", o.getRefundProofPublicId() != null && !o.getRefundProofPublicId().isBlank());
                Integer processorId = o.getRefundProcessedBy();
                m.put("refundProcessedBy", processorId);
                String processorName = processorId == null ? null : processorNames.get(processorId);
                m.put("refundProcessedByName", processorName == null || processorName.isBlank() ? null : processorName);
                m.put("cancelledAt", o.getCancelledAt() != null ? o.getCancelledAt().toString() : null);
                m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : null);
                m.put("refundedAt", o.getRefundedAt() != null ? o.getRefundedAt().toString() : null);
                m.put("failureReason", o.getFailureReason());
                m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                return m;
            }).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
        } catch (RefundService.RefundNotFoundException e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    public static Map<Integer, String> resolveProcessorNames(List<Orders> orders,
            Function<Set<Integer>, List<User>> batchLoader) {
        Set<Integer> processorIds = orders.stream()
                .map(Orders::getRefundProcessedBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (processorIds.isEmpty()) return Map.of();
        Map<Integer, String> names = new LinkedHashMap<>();
        processorIds.forEach(id -> names.put(id, null));
        for (User user : batchLoader.apply(processorIds)) {
            String fullName = user.getFullName();
            names.put(user.getUserId(), fullName == null || fullName.isBlank() ? null : fullName);
        }
        return names;
    }

    private LocalDate toLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException, jakarta.servlet.ServletException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        String token = header.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token)) || !PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), "ADMIN")) { ApiResponse.error(resp, "Forbidden", 403); return; }
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() < 2) { ApiResponse.error(resp, "Invalid order ID", 400); return; }
            if (!pathInfo.matches("/[1-9]\\d*")) { ApiResponse.error(resp, "Invalid order ID", 400); return; }
            int orderId = Integer.parseInt(pathInfo.substring(1));
            String expectedStatus = req.getParameter("expectedStatus");
            if (!"PENDING".equals(expectedStatus)) { ApiResponse.error(resp, "Invalid expectedStatus", 400); return; }
            String status = req.getParameter("status");
            String note = req.getParameter("refundNote");
            String reference = req.getParameter("refundReference");
            String rawAmount = req.getParameter("refundAmount");
            BigDecimal amount = rawAmount == null || rawAmount.isBlank() ? null : new BigDecimal(rawAmount);
            RefundProofStorage.UploadedProof proof = null;
            Part proofPart;
            try { proofPart = req.getPart("proof"); } catch (IllegalStateException e) { throw new IllegalArgumentException("Refund proof exceeds 5 MiB"); }
            if (proofPart != null && proofPart.getSize() > 0) {
                String filename = proofPart.getSubmittedFileName() == null ? "" : proofPart.getSubmittedFileName().toLowerCase(java.util.Locale.ROOT);
                if (!(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".webp"))) throw new IllegalArgumentException("Invalid refund proof extension");
                proof = storage().uploadPrivate(String.valueOf(orderId), proofPart.getInputStream().readAllBytes(), proofPart.getContentType());
            }
            try {
                refundService.update(orderId, expectedStatus, status, amount, note, reference, proof, JwtUtil.getUserId(token));
            } catch (RuntimeException e) {
                if (proof != null) storage().delete(proof.publicId());
                throw e;
            }
            ApiResponse.ok(resp, null, "Refund updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid refund amount or order ID", 400);
        } catch (RefundService.RefundNotFoundException e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (RefundService.RefundConflictException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    private RefundProofStorage storage() { if (proofStorage == null) proofStorage = new RefundProofStorage(); return proofStorage; }
}
