package utils;

import exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

public class RoleRequired {

    public static void require(HttpServletRequest req, String... allowedRoles) {
        String role = (String) req.getAttribute("role");
        if (role == null) {
            throw new ForbiddenException("Bạn không có quyền truy cập");
        }
        for (String r : allowedRoles) {
            if (r.equals(role)) return;
        }
        throw new ForbiddenException("Bạn không có quyền truy cập");
    }
}
