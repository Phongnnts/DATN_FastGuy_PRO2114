package service;

import dto.AddressDTO;
import entity.Address;
import entity.DeliveryZone;
import entity.User;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import repository.AddressRepository;
import repository.UserRepository;
import utils.ValidationUtil;

import java.util.List;
import java.util.stream.Collectors;

public class AddressService {
    private final AddressRepository addressRepository = new AddressRepository();
    private final UserRepository userRepository = new UserRepository();
    private final DeliveryZoneService deliveryZoneService = new DeliveryZoneService();

    public List<AddressDTO> getByUserId(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AddressDTO create(Long userId, AddressDTO dto) {
        validate(dto);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        DeliveryZone zone = deliveryZoneService.findById(dto.getZoneId());

        Address address = new Address();
        address.setUser(user);
        address.setZone(zone);
        applyDTO(address, dto);

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.clearDefault(userId);
        }

        address = addressRepository.save(address);
        return toDTO(address);
    }

    public AddressDTO update(Long userId, Long addressId, AddressDTO dto) {
        validate(dto);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        if (dto.getZoneId() != null) {
            DeliveryZone zone = deliveryZoneService.findById(dto.getZoneId());
            address.setZone(zone);
        }
        applyDTO(address, dto);

        if (dto.getIsDefault() != null && dto.getIsDefault()) {
            addressRepository.clearDefault(userId);
        }

        address = addressRepository.save(address);
        return toDTO(address);
    }

    public void delete(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        addressRepository.delete(address);
    }

    private void validate(AddressDTO dto) {
        ValidationUtil.notBlank(dto.getRecipientName(), "Tên người nhận");
        ValidationUtil.notBlank(dto.getPhone(), "Số điện thoại");
        ValidationUtil.isPhone(dto.getPhone());
        ValidationUtil.notBlank(dto.getStreet(), "Địa chỉ");
        ValidationUtil.notNull(dto.getZoneId(), "Khu vực giao hàng");
        ValidationUtil.notBlank(dto.getCity(), "Thành phố");
    }

    private void applyDTO(Address address, AddressDTO dto) {
        if (dto.getRecipientName() != null) address.setRecipientName(dto.getRecipientName());
        if (dto.getPhone() != null) address.setPhone(dto.getPhone());
        if (dto.getStreet() != null) address.setStreet(dto.getStreet());
        if (dto.getWard() != null) address.setWard(dto.getWard());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getIsDefault() != null) address.setIsDefault(dto.getIsDefault());
    }

    private AddressDTO toDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setAddressId(address.getAddressId());
        dto.setRecipientName(address.getRecipientName());
        dto.setPhone(address.getPhone());
        dto.setStreet(address.getStreet());
        dto.setWard(address.getWard());
        dto.setZoneId(address.getZone().getZoneId());
        dto.setZoneName(address.getZone().getDistrictName());
        dto.setShippingFee(address.getZone().getShippingFee());
        dto.setCity(address.getCity());
        dto.setIsDefault(address.getIsDefault());
        return dto;
    }
}
