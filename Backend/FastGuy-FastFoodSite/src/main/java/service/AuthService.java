package service;

import dto.ChangePasswordRequest;
import dto.LoginRequest;
import dto.LoginResponse;
import dto.RegisterRequest;
import entity.Role;
import entity.User;
import exception.BadRequestException;
import exception.DuplicateResourceException;
import exception.ResourceNotFoundException;
import exception.UnauthorizedException;
import jakarta.persistence.EntityManager;
import repository.RoleRepository;
import repository.UserRepository;
import utils.JwtUtil;
import utils.PasswordUtil;
import utils.ValidationUtil;

public class AuthService {
    private final UserRepository userRepository = new UserRepository();
    private final RoleRepository roleRepository = new RoleRepository();

    public LoginResponse register(RegisterRequest req) {
        ValidationUtil.notBlank(req.getFullName(), "Họ tên");
        ValidationUtil.notBlank(req.getPhone(), "Số điện thoại");
        ValidationUtil.notBlank(req.getPassword(), "Mật khẩu");
        ValidationUtil.isPhone(req.getPhone());
        ValidationUtil.isEmail(req.getEmail());
        ValidationUtil.minLength(req.getPassword(), 6, "Mật khẩu");

        if (userRepository.existsByPhone(req.getPhone())) {
            throw new DuplicateResourceException("Số điện thoại đã được đăng ký");
        }

        Role userRole = roleRepository.findByName("USER");
        if (userRole == null) {
            throw new ResourceNotFoundException("Role", "USER");
        }

        User user = new User(userRole, req.getPhone(),
                PasswordUtil.hash(req.getPassword()), req.getFullName());
        user.setEmail(req.getEmail());
        user = userRepository.save(user);

        String token = JwtUtil.generateToken(user.getUserId(), "USER");
        return new LoginResponse(token, user.getUserId(), "USER",
                user.getFullName(), user.getAvatarUrl());
    }

    public LoginResponse login(LoginRequest req) {
        ValidationUtil.notBlank(req.getLogin(), "Tên đăng nhập");
        ValidationUtil.notBlank(req.getPassword(), "Mật khẩu");

        String login = req.getLogin();
        User user = login.contains("@")
                ? userRepository.findByEmail(login).orElse(null)
                : userRepository.findByPhone(login).orElse(null);

        if (user == null) {
            throw new UnauthorizedException("Số điện thoại/email hoặc mật khẩu không đúng");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }

        if (!PasswordUtil.verify(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Số điện thoại/email hoặc mật khẩu không đúng");
        }

        String roleName = user.getRole().getRoleName();
        String token = JwtUtil.generateToken(user.getUserId(), roleName);
        return new LoginResponse(token, user.getUserId(), roleName,
                user.getFullName(), user.getAvatarUrl());
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        ValidationUtil.notBlank(req.getOldPassword(), "Mật khẩu cũ");
        ValidationUtil.notBlank(req.getNewPassword(), "Mật khẩu mới");
        ValidationUtil.minLength(req.getNewPassword(), 6, "Mật khẩu mới");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!PasswordUtil.verify(req.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        user.setPasswordHash(PasswordUtil.hash(req.getNewPassword()));
        userRepository.save(user);
    }
}
