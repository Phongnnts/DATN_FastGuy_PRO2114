package entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private int scheduleId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "shift_id")
    private WorkShift shift;

    @Column(name = "work_date")
    private LocalDate workDate;

    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "status")
    private String status;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Schedule() {}

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public WorkShift getShift() { return shift; }
    public void setShift(WorkShift shift) { this.shift = shift; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
