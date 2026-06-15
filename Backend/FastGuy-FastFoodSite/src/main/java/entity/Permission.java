package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "permission_name", nullable = false, unique = true, length = 100)
    private String permissionName;

    @Column(name = "description", length = 255)
    private String description;

    public Permission() {}

    public Permission(String permissionName, String description) {
        this.permissionName = permissionName;
        this.description = description;
    }

    public Integer getPermissionId() { return permissionId; }
    public void setPermissionId(Integer permissionId) { this.permissionId = permissionId; }
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
