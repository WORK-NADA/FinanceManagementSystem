package FinanceManangementSystem.demo.Service.Implementations;

import FinanceManangementSystem.demo.Enums.UserRole;
import FinanceManangementSystem.demo.Exceptions.DuplicateResourceException;
import FinanceManangementSystem.demo.Exceptions.ResourceNotFoundException;
import FinanceManangementSystem.demo.Model.User;
import FinanceManangementSystem.demo.Model.UserAddress;
import FinanceManangementSystem.demo.Repository.UserRepository;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUpdateUserDTO;
import FinanceManangementSystem.demo.Payloads.RequestDTO.RequestUserDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserAddressDTO;
import FinanceManangementSystem.demo.Payloads.ResponseDTO.ResponseUserDTO;
import FinanceManangementSystem.demo.Service.AdminServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AdminService implements AdminServiceInterface {

    private final UserRepository userRepo;

    private final ModelMapper modelMapper;

    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    @Override
    public ResponseUserDTO registration(RequestUserDTO dto) {
        log.info("SERVICE - request came in registration...");

        Optional<String> name = userRepo.findByEmailOrContact(dto.getEmail(),dto.getMobileNumber());

        if(name.isPresent()){
            throw new DuplicateResourceException("User already exists...");
        }

        User user = modelMapper.map(dto,User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (dto.getUserAddress() != null) {
            UserAddress address = modelMapper.map(dto.getUserAddress(), UserAddress.class);

            address.setUser(user);

            user.setAddress(address);
        }

        user = userRepo.save(user);

        log.info("SERVICE - registered successfully...");

        ResponseUserDTO response = modelMapper.map(user,ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseUserDTO> listAllUsers() {
        log.info("SERVICE - request came in listAllUsers...");

        // Filter explicitly by UserRole.CLIENT to exclude ADMIN accounts from the clients list
        List<User> users = userRepo.findByRoleOrderByCreatedAtDesc(UserRole.CLIENT);

        log.info("SERVICE - clients fetched successfully, count: {}", users.size());

        return users.stream()
                .map(user -> {
                    ResponseUserDTO dto = modelMapper.map(user, ResponseUserDTO.class);
                    if (user.getAddress() != null) {
                        dto.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseUserDTO getUserByPublicId(UUID publicId) {
        log.info("SERVICE - request came in getUserByPublicId for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        return response;
    }

    @Transactional
    @Override
    public ResponseUserDTO updateUser(UUID publicId, RequestUpdateUserDTO dto) {
        log.info("SERVICE - request came in updateUser for publicId: {}", publicId);

        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));

        // Check for email or mobile conflict with other users
        Optional<String> duplicate = userRepo.findByEmailOrContactAndNotPublicId(dto.getEmail(), dto.getMobileNumber(), publicId);
        if (duplicate.isPresent()) {
            throw new DuplicateResourceException("Another user with this email or mobile number already exists.");
        }

        user.setOwnerName(dto.getOwnerName());
        user.setEmail(dto.getEmail());
        user.setMobileNumber(dto.getMobileNumber());

        if (dto.getUserAddress() != null) {
            UserAddress address = user.getAddress();
            if (address == null) {
                address = new UserAddress();
                address.setUser(user);
                user.setAddress(address);
            }
            address.setHouseNo(dto.getUserAddress().getHouseNo());
            address.setSocietyName(dto.getUserAddress().getSocietyName());
            address.setArea(dto.getUserAddress().getArea());
            address.setCity(dto.getUserAddress().getCity());
            address.setPincode(dto.getUserAddress().getPincode());
            address.setState(dto.getUserAddress().getState());
            address.setCountry(dto.getUserAddress().getCountry() != null && !dto.getUserAddress().getCountry().isBlank()
                    ? dto.getUserAddress().getCountry() : "India");
        }

        user = userRepo.save(user);

        log.info("SERVICE - user updated successfully...");

        ResponseUserDTO response = modelMapper.map(user, ResponseUserDTO.class);
        if (user.getAddress() != null) {
            response.setUserAddress(modelMapper.map(user.getAddress(), ResponseUserAddressDTO.class));
        }
        return response;
    }

    @Transactional
    @Override
    public void deactivateUser(UUID publicId) {
        log.info("SERVICE - request came in deactivateUser for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        user.setEnabled(false);
        userRepo.save(user);
        log.info("SERVICE - user deactivated successfully...");
    }

    @Transactional
    @Override
    public void reactivateUser(UUID publicId) {
        log.info("SERVICE - request came in reactivateUser for publicId: {}", publicId);
        User user = userRepo.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with publicId: " + publicId));
        user.setEnabled(true);
        userRepo.save(user);
        log.info("SERVICE - user reactivated successfully...");
    }
}
