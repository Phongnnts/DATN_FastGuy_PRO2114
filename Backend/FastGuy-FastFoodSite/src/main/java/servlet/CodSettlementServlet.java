package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.CodSettlementService;
import service.CodSettlementService.SettlementConflictException;
import service.CodSettlementService.SettlementNotFoundException;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.PrivilegedAuth;

@WebServlet("/api/cod-settlements/*")
public class CodSettlementServlet extends HttpServlet {
    record AuthIdentity(int userId, String role) {}
    interface TokenReader { Map<String, Object> read(String token); }

    private final ObjectMapper mapper;
    private final CodSettlementService service;
    private final TokenReader tokenReader;
    private final BiPredicate<Integer, String> activeRole;

    public CodSettlementServlet() {
        this(new CodSettlementService(), token -> {
            Claims claims = JwtUtil.validate(token);
            return claims == null ? null : claims;
        }, PrivilegedAuth::isActiveRole);
    }

    CodSettlementServlet(CodSettlementService service, TokenReader tokenReader, BiPredicate<Integer, String> activeRole) {
        this.mapper = new ObjectMapper();
        this.service = service;
        this.tokenReader = tokenReader;
        this.activeRole = activeRole;
    }

    private int requireRole(HttpServletRequest req, HttpServletResponse resp, String requiredRole) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() == 7) { ApiResponse.error(resp, "Missing token", 401); return -1; }
        AuthIdentity identity;
        try {
            identity = extractIdentity(tokenReader.read(header.substring(7)));
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Invalid token", 401);
            return -1;
        }
        if (identity == null) { ApiResponse.error(resp, "Invalid token", 401); return -1; }
        if (!requiredRole.equals(identity.role()) || !activeRole.test(identity.userId(), identity.role())) { ApiResponse.error(resp, "Forbidden", 403); return -1; }
        return identity.userId();
    }

    static AuthIdentity extractIdentity(Map<String, Object> claims) {
        if (claims == null) return null;
        Object rawUserId = claims.get("userId");
        Object rawRole = claims.get("role");
        if (!(rawUserId instanceof Integer userId) || userId <= 0 || !(rawRole instanceof String role) || role.isBlank()) return null;
        return new AuthIdentity(userId, role);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            if ("/current".equals(path)) {
                int shipperId = requireRole(req, resp, "SHIPPER");
                if (shipperId < 0) return;
                ApiResponse.ok(resp, service.getShipperCurrent(shipperId));
            } else if ("/mine".equals(path)) {
                int shipperId = requireRole(req, resp, "SHIPPER");
                if (shipperId < 0) return;
                ApiResponse.ok(resp, service.listForShipper(shipperId));
            } else if ("/admin".equals(path)) {
                int adminId = requireRole(req, resp, "ADMIN");
                if (adminId < 0) return;
                String status = req.getParameter("status");
                if (!Set.of("SUBMITTED", "SHORT", "OVER", "SETTLED").contains(status)) throw new IllegalArgumentException("Invalid status");
                ApiResponse.ok(resp, service.listForAdmin(status));
            } else {
                ApiResponse.error(resp, "Not found", 404);
            }
        } catch (SettlementNotFoundException e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (SettlementConflictException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (SecurityException e) {
            ApiResponse.error(resp, "Forbidden", 403);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            if (path != null && !"/".equals(path)) { ApiResponse.error(resp, "Not found", 404); return; }
            int shipperId = requireRole(req, resp, "SHIPPER");
            if (shipperId < 0) return;
            Map<?, ?> body = readBody(req);
            int shiftId = positiveInt(body.get("shiftId"), "Invalid shiftId");
            BigDecimal submittedAmount = decimal(body.get("submittedAmount"), "Invalid submittedAmount");
            ApiResponse.ok(resp, service.submit(shipperId, shiftId, submittedAmount));
        } catch (SettlementNotFoundException e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (SettlementConflictException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (SecurityException e) {
            ApiResponse.error(resp, "Forbidden", 403);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            if (path == null || !path.matches("/[1-9]\\d*/verify")) { ApiResponse.error(resp, "Not found", 404); return; }
            int adminId = requireRole(req, resp, "ADMIN");
            if (adminId < 0) return;
            int settlementId = Integer.parseInt(path.substring(1, path.indexOf('/', 1)));
            Map<?, ?> body = readBody(req);
            if (!Set.of("expectedStatus", "status", "verifiedAmount", "reason").containsAll(body.keySet())
                    || !body.keySet().containsAll(Set.of("expectedStatus", "status", "verifiedAmount"))) {
                throw new IllegalArgumentException("Invalid verification fields");
            }
            String expectedStatus = string(body.get("expectedStatus"), "Invalid expectedStatus");
            if (!"SUBMITTED".equals(expectedStatus)) throw new IllegalArgumentException("Invalid expectedStatus");
            String status = string(body.get("status"), "Invalid status");
            BigDecimal verifiedAmount = decimal(body.get("verifiedAmount"), "Invalid verifiedAmount");
            Object rawReason = body.get("reason");
            String reason = rawReason == null ? null : string(rawReason, "Invalid reason");
            ApiResponse.ok(resp, service.verify(adminId, settlementId, expectedStatus, status, verifiedAmount, reason));
        } catch (SettlementNotFoundException e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (SettlementConflictException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (SecurityException e) {
            ApiResponse.error(resp, "Forbidden", 403);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    private Map<?, ?> readBody(HttpServletRequest req) {
        try {
            return mapper.readValue(req.getReader(), Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON");
        }
    }

    private static int positiveInt(Object value, String message) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException(message);
        int result = number.intValue();
        if (result <= 0 || BigDecimal.valueOf(result).compareTo(new BigDecimal(number.toString())) != 0) throw new IllegalArgumentException(message);
        return result;
    }

    private static BigDecimal decimal(Object value, String message) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException(message);
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String string(Object value, String message) {
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(message);
        return text;
    }
}
