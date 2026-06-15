package service;

import dto.UserDTO;
import entity.User;
import exception.ResourceNotFoundException;
import repository.UserRepository;
import utils.ValidationUtil;

public class UserService {
    private final UserRepository userRepository = new UserRepository();

    public UserDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toDTO(user);
    }

    public UserDTO updateProfile(Long userId, UserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (dto.getFullName() != null) {
            ValidationUtil.notBlank(dto.getFullName(), "Họ tên");
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null) {
            ValidationUtil.isEmail(dto.getEmail());
            user.setEmail(dto.getEmail());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        user = userRepository.save(user);
        return toDTO(user);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole().getRoleName());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
