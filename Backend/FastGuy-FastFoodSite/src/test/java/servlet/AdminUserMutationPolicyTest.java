package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AdminUserMutationPolicyTest {
    @Test
    void blocksDangerousSelfMutations() {
        assertEquals("Không thể xóa tài khoản của chính bạn", AdminUserServlet.mutationConflict(7, 7, "ADMIN", "ACTIVE", "DELETE", 2));
        assertEquals("Không thể vô hiệu hóa tài khoản của chính bạn", AdminUserServlet.mutationConflict(7, 7, "ADMIN", "ACTIVE", "DISABLE", 2));
        assertEquals("Không thể hạ quyền quản trị của chính bạn", AdminUserServlet.mutationConflict(7, 7, "ADMIN", "ACTIVE", "DEMOTE", 2));
    }

    @Test
    void blocksMutationOfLastActiveAdmin() {
        assertEquals("Phải giữ lại ít nhất một quản trị viên đang hoạt động", AdminUserServlet.mutationConflict(3, 7, "ADMIN", "ACTIVE", "DELETE", 1));
        assertEquals("Phải giữ lại ít nhất một quản trị viên đang hoạt động", AdminUserServlet.mutationConflict(3, 7, "ADMIN", "ACTIVE", "DISABLE", 1));
        assertEquals("Phải giữ lại ít nhất một quản trị viên đang hoạt động", AdminUserServlet.mutationConflict(3, 7, "ADMIN", "ACTIVE", "DEMOTE", 1));
    }

    @Test
    void allowsSafeMutations() {
        assertNull(AdminUserServlet.mutationConflict(3, 7, "ADMIN", "ACTIVE", "DELETE", 2));
        assertNull(AdminUserServlet.mutationConflict(7, 7, "ADMIN", "INACTIVE", "ENABLE", 1));
        assertNull(AdminUserServlet.mutationConflict(7, 7, "ADMIN", "ACTIVE", "EDIT", 1));
    }
}
