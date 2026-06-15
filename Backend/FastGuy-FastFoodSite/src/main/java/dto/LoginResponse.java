package dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String role;
    private String fullName;
    private String avatarUrl;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String role, String fullName, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
