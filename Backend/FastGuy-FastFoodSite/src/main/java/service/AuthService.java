package service;

import dao.RoleDAO;
import dao.UserDAO;
import entity.Role;
import entity.User;
import utils.PasswordUtil;
import utils.JwtUtil;

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
}
