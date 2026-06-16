package service;

import entity.WorkShift;
import exception.ResourceNotFoundException;
import repository.WorkShiftRepository;

import java.util.List;

public class WorkShiftService {
    private final WorkShiftRepository workShiftRepository = new WorkShiftRepository();

    public List<WorkShift> getAll() {
        return workShiftRepository.findAll();
    }

    public WorkShift getById(Long id) {
        return workShiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkShift", id));
    }

    public WorkShift create(WorkShift shift) {
        return workShiftRepository.save(shift);
    }

    public WorkShift update(Long id, WorkShift updated) {
        WorkShift shift = getById(id);
        shift.setShiftName(updated.getShiftName());
        shift.setStartTime(updated.getStartTime());
        shift.setEndTime(updated.getEndTime());
        shift.setRoleType(updated.getRoleType());
        return workShiftRepository.save(shift);
    }

    public void delete(Long id) {
        workShiftRepository.deleteById(id);
    }
}
