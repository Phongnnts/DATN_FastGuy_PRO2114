package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "WorkShift")
public class WorkShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private int shiftId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "shift_date")
    private LocalDate shiftDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "shift_code")
    private String shiftCode;

    @Column(name = "check_in_source")
    private String checkInSource;

    @Column(name = "check_out_source")
    private String checkOutSource;

    @Column(name = "staff_role_snapshot")
    private String staffRoleSnapshot;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @Column(name = "status")
    private String status;

    @Column(name = "attendance_status")
    private String attendanceStatus;

    @Column(name = "approved_minutes")
    private Integer approvedMinutes;

    @Column(name = "approved_overtime_minutes")
    private Integer approvedOvertimeMinutes;

    @Column(name = "attendance_note")
    private String attendanceNote;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }
    public String getCheckInSource() { return checkInSource; }
    public void setCheckInSource(String checkInSource) { this.checkInSource = checkInSource; }
    public String getCheckOutSource() { return checkOutSource; }
    public void setCheckOutSource(String checkOutSource) { this.checkOutSource = checkOutSource; }
    public String getStaffRoleSnapshot() { return staffRoleSnapshot; }
    public void setStaffRoleSnapshot(String staffRoleSnapshot) { this.staffRoleSnapshot = staffRoleSnapshot; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public Integer getApprovedMinutes() { return approvedMinutes; }
    public void setApprovedMinutes(Integer approvedMinutes) { this.approvedMinutes = approvedMinutes; }
    public Integer getApprovedOvertimeMinutes() { return approvedOvertimeMinutes; }
    public void setApprovedOvertimeMinutes(Integer approvedOvertimeMinutes) { this.approvedOvertimeMinutes = approvedOvertimeMinutes; }
    public String getAttendanceNote() { return attendanceNote; }
    public void setAttendanceNote(String attendanceNote) { this.attendanceNote = attendanceNote; }
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
