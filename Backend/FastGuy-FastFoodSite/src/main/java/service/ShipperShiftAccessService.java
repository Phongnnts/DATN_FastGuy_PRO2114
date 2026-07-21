package service;

import dao.UserDAO;
import entity.User;

public class ShipperShiftAccessService {
    private final WorkShiftService workShiftService = new WorkShiftService();
    private final UserDAO userDAO = new UserDAO();

    public boolean hasValidShipperIdentity(int userId) {
        User user = userDAO.findById(userId);
        return user != null && "SHIPPER".equals(user.getRole()) && "ACTIVE".equals(user.getStatus());
    }

    public boolean hasCheckedInShift(int userId) {
        return "CHECKED_IN".equals(workShiftService.current(userId).get("state"));
    }
}
