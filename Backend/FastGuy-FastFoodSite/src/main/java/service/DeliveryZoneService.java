package service;

import dto.DeliveryZoneDTO;
import entity.DeliveryZone;
import exception.ResourceNotFoundException;
import repository.DeliveryZoneRepository;

import java.util.List;
import java.util.stream.Collectors;

public class DeliveryZoneService {
    private final DeliveryZoneRepository deliveryZoneRepository = new DeliveryZoneRepository();

    public List<DeliveryZoneDTO> getAllActive() {
        return deliveryZoneRepository.findAllActive().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DeliveryZone findById(Long zoneId) {
        return deliveryZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryZone", zoneId));
    }

    private DeliveryZoneDTO toDTO(DeliveryZone zone) {
        DeliveryZoneDTO dto = new DeliveryZoneDTO();
        dto.setZoneId(zone.getZoneId());
        dto.setDistrictName(zone.getDistrictName());
        dto.setShippingFee(zone.getShippingFee());
        return dto;
    }
}
