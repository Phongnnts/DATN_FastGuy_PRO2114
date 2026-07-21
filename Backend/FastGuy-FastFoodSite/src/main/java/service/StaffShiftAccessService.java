package service;

import dao.UserDAO;
import entity.User;

public class StaffShiftAccessService {
    private final WorkShiftService workShiftService = new WorkShiftService();
    private final UserDAO userDAO = new UserDAO();

    public boolean hasValidStaffIdentity(int userId) {
        User user = userDAO.findById(userId);
        return user != null && isValidStaffIdentity(user.getRole(), user.getStatus());
    }

    public boolean hasCheckedInShift(int userId) {
        return isCheckedIn((String) workShiftService.current(userId).get("state"));
    }

    static boolean isValidStaffIdentity(String role, String status) {
        return "STAFF".equals(role) && "ACTIVE".equals(status);
    }

    static boolean isCheckedIn(String shiftState) {
        return "CHECKED_IN".equals(shiftState);
    }
}
