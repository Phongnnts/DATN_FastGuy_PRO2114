package service;

import entity.Schedule;
import entity.User;
import entity.WorkShift;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import repository.ScheduleRepository;
import repository.UserRepository;
import repository.WorkShiftRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ScheduleService {
    private final ScheduleRepository scheduleRepository = new ScheduleRepository();
    private final UserRepository userRepository = new UserRepository();
    private final WorkShiftRepository workShiftRepository = new WorkShiftRepository();

    public List<Schedule> getByDate(LocalDate date) {
        return scheduleRepository.findByDate(date);
    }

    public Schedule create(Long userId, Long shiftId, LocalDate workDate, Long assignedById, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        WorkShift shift = workShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkShift", shiftId));
        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new ResourceNotFoundException("User", assignedById));

        Schedule schedule = new Schedule();
        schedule.setUser(user);
        schedule.setShift(shift);
        schedule.setWorkDate(workDate);
        schedule.setAssignedBy(assignedBy);
        schedule.setNote(note);
        schedule.setStatus("PENDING");
        return scheduleRepository.save(schedule);
    }

    public Schedule update(Long scheduleId, Long shiftId, String note) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", scheduleId));
        if (shiftId != null) {
            WorkShift shift = workShiftRepository.findById(shiftId)
                    .orElseThrow(() -> new ResourceNotFoundException("WorkShift", shiftId));
            schedule.setShift(shift);
        }
        if (note != null) schedule.setNote(note);
        return scheduleRepository.save(schedule);
    }

    public Schedule checkIn(Long userId) {
        Schedule schedule = getTodaySchedule(userId);
        if (!"PENDING".equals(schedule.getStatus())) {
            throw new BadRequestException("Bạn đã check-in rồi");
        }
        schedule.setStatus("CHECKED_IN");
        schedule.setCheckedInAt(LocalDateTime.now());
        return scheduleRepository.save(schedule);
    }

    public Schedule checkOut(Long userId) {
        Schedule schedule = getTodaySchedule(userId);
        if (!"CHECKED_IN".equals(schedule.getStatus())) {
            throw new BadRequestException("Bạn chưa check-in hoặc đã check-out rồi");
        }
        schedule.setStatus("CHECKED_OUT");
        schedule.setCheckedOutAt(LocalDateTime.now());
        return scheduleRepository.save(schedule);
    }

    public Schedule getTodaySchedule(Long userId) {
        return scheduleRepository.findByUserAndDate(userId, LocalDate.now())
                .orElseThrow(() -> new BadRequestException("Hôm nay bạn không có ca làm việc"));
    }
}
