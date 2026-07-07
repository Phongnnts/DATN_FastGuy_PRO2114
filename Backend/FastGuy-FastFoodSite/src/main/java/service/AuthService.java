package service;

import java.util.Map;

import dao.RoleDAO;
import dao.UserDAO;
import entity.Role;
import entity.User;
import utils.PasswordUtil;

public class AuthService {
    private UserDAO userDAO = new UserDAO();
    private RoleDAO roleDAO = new RoleDAO();

    public User login(String login, String password) {
        User user = userDAO.findByPhone(login);
        if (user == null) {
            user = userDAO.findByEmail(login);
        }
        if (user != null && PasswordUtil.check(password, user.getPasswordHash())) {
            if ("ACTIVE".equals(user.getStatus())) {
                if (!PasswordUtil.isHashed(user.getPasswordHash())) {
                    user.setPasswordHash(PasswordUtil.hash(password));
                    userDAO.save(user);
                }
                return user;
            }
        }
        return null;
    }

    public User register(String fullName, String phone, String email, String password) {
        if (userDAO.findByPhone(phone) != null) {
            return null;
        }
        if (email != null && !email.isEmpty() && userDAO.findByEmail(email) != null) {
            return null;
        }

        Role userRole = roleDAO.findByName("USER");
        if (userRole == null) {
            return null;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(userRole);
        user.setStatus("ACTIVE");

        userDAO.save(user);
        return user;
    }

    public User getProfile(int userId) {
        return userDAO.findById(userId);
    }

    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) return false;
        if (!PasswordUtil.check(currentPassword, user.getPasswordHash())) return false;
        user.setPasswordHash(PasswordUtil.hash(newPassword));
        userDAO.save(user);
        return true;
    }

    public User updateProfile(int userId, Map<String, Object> data) {
        User user = userDAO.findById(userId);
        if (user == null) return null;

        String fullName = (String) data.get("fullName");
        String phone = (String) data.get("phone");
        String email = (String) data.get("email");

        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }
        if (phone != null && !phone.isEmpty()) {
            if (userDAO.findByPhone(phone) != null && !phone.equals(user.getPhone())) {
                return null;
            }
            user.setPhone(phone);
        }
        if (email != null && !email.isEmpty()) {
            if (userDAO.findByEmail(email) != null && !email.equals(user.getEmail())) {
                return null;
            }
            user.setEmail(email);
        }

        userDAO.save(user);
        return user;
    }
}
