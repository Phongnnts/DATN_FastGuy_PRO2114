package service;

import java.time.LocalDateTime;
import java.util.Map;

import dao.UserDAO;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;
import utils.PasswordUtil;

public class AuthService {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 15;
    private static final String LOCKED_MESSAGE = "Tài khoản đã bị khóa tạm thời, vui lòng thử lại sau";

    private UserDAO userDAO = new UserDAO();

    public User login(String login, String password) {
        User found = userDAO.findByPhone(login);
        if (found == null) {
            found = userDAO.findByEmail(login);
        }
        if (found == null) return null;

        LocalDateTime now = LocalDateTime.now();
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, found.getUserId(), LockModeType.PESSIMISTIC_WRITE);
            if (user == null) {
                em.getTransaction().rollback();
                return null;
            }
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                em.getTransaction().rollback();
                throw new IllegalStateException(LOCKED_MESSAGE);
            }
            if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
            if (PasswordUtil.check(password, user.getPasswordHash())) {
                if ("ACTIVE".equals(user.getStatus())) {
                    if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                        user.setFailedLoginAttempts(0);
                        user.setLockedUntil(null);
                    }
                    if (!PasswordUtil.isHashed(user.getPasswordHash())) {
                        user.setPasswordHash(PasswordUtil.hash(password));
                    }
                    em.getTransaction().commit();
                    return user;
                }
                em.getTransaction().rollback();
                return null;
            }
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(LOCK_DURATION_MINUTES));
                em.getTransaction().commit();
                throw new IllegalStateException(LOCKED_MESSAGE);
            }
            em.getTransaction().commit();
            return null;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public User register(String fullName, String phone, String email, String password) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User existing = em.createQuery("SELECT u FROM User u WHERE u.phone = :phone", User.class)
                    .setParameter("phone", phone).setMaxResults(1).getResultStream().findFirst().orElse(null);
            if (existing != null) { em.getTransaction().rollback(); return null; }
            if (email != null && !email.isEmpty()) {
                existing = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                        .setParameter("email", email).setMaxResults(1).getResultStream().findFirst().orElse(null);
                if (existing != null) { em.getTransaction().rollback(); return null; }
            }

            User user = new User();
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setEmail(email);
            user.setPasswordHash(PasswordUtil.hash(password));
            user.setRole("USER");
            user.setStatus("ACTIVE");
            user.setFavoriteIdsJson("[]");
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public User getProfile(int userId) {
        return userDAO.findById(userId);
    }

    public void changePassword(int userId, String currentPassword, String newPassword) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null) throw new IllegalArgumentException("Không tìm thấy người dùng");
            if (!PasswordUtil.check(currentPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
            }
            if (!isStrongPassword(newPassword)) {
                throw new IllegalArgumentException("Mật khẩu mới phải từ 8 ký tự, có ít nhất 1 chữ và 1 số");
            }
            user.setPasswordHash(PasswordUtil.hash(newPassword));
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8 || pw.length() > 72) return false;
        boolean hasLetter = pw.matches(".*[a-zA-Z].*");
        boolean hasDigit = pw.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }

    public User updateProfile(int userId, Map<String, Object> data) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null) { em.getTransaction().rollback(); return null; }

            String fullName = (String) data.get("fullName");
            String phone = (String) data.get("phone");
            String email = (String) data.get("email");

            if (fullName != null && !fullName.isEmpty()) {
                user.setFullName(fullName);
            }
            if (phone != null && !phone.isEmpty()) {
                if (!phone.equals(user.getPhone())) {
                    User dup = em.createQuery("SELECT u FROM User u WHERE u.phone = :phone", User.class)
                            .setParameter("phone", phone).setMaxResults(1).getResultStream().findFirst().orElse(null);
                    if (dup != null) { em.getTransaction().rollback(); return null; }
                }
                user.setPhone(phone);
            }
            if (email != null && !email.isEmpty()) {
                if (!email.equals(user.getEmail())) {
                    User dup = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                            .setParameter("email", email).setMaxResults(1).getResultStream().findFirst().orElse(null);
                    if (dup != null) { em.getTransaction().rollback(); return null; }
                }
                user.setEmail(email);
            }
            em.getTransaction().commit();
            return user;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
