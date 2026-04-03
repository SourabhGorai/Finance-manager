package com.userservice.service;

import com.userservice.dto.UserCreateDto;
import com.userservice.dto.UserDto;
import com.userservice.dto.UserUpdateDto;
import com.userservice.model.Role;
import com.userservice.model.User;
import com.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
//    private final UserMapper mapper;
    private final OtpService otpService;


    public UserDto registerUser(UserCreateDto dto) {

        log.info("Attempting to register new user");

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = UserMapper.toEntity(dto);
        if (user.getRole() == null) {
            user.setRole(Role.VIEWER);
        } else if (user.getRole() == Role.ADMIN){
            throw new RuntimeException("Contact admin to create new ADMIN user.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerified(false);
        User saved = userRepository.save(user);

        otpService.generateAndSendOtpForUser(saved);

        return UserMapper.toDto(saved);
    }

    public UserDto registerAdmin(UserCreateDto dto, String requesterRole) {
        log.info("Attempting to register new ADMIN");

        if (!requesterRole.equals("ADMIN")) {
            log.warn("Unauthorized attempt to create ADMIN by role: {}", requesterRole);
            throw new ServiceException("You are not authorized to create an ADMIN");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = UserMapper.toEntity(dto);
        if (user.getRole() == null) {
            user.setRole(Role.VIEWER);
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setVerified(false);
        User saved = userRepository.save(user);

        otpService.generateAndSendOtpForUser(saved);
        return UserMapper.toDto(saved);
    }


    public List<UserDto> getAllUsers() {

        log.info("Attempting to fetch all users");

        log.debug("Fetching all users from database (cache miss)");
        return userRepository.findAll().stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<UserDto> getAllUsersPaginated(int page, int size) {
        log.info("Fetching paginated users - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .map(UserMapper::toDto);
    }


    public UserDto getUserByUsn(String usn) {

        log.info("Attempting to fetch user details with usn: {}", usn);

        log.debug("Fetching user by PRN: {} from database (cache miss)", usn);
//        log.info("{}", userRepository.findByPrn(prn));
        return userRepository.findByUsn(usn)
                .map(UserMapper::toDto)
                .orElse(null);
    }


    public UserDto updateUser(String usn, UserUpdateDto dto) {

        log.info("Attempting to update user with usn: {}", usn);

        User user = userRepository.findByUsn(usn)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserMapper.updateEntityFromDto(dto, user);
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        User saved = userRepository.save(user);
        log.debug("Updated user {} and evicted related caches", usn);
        return UserMapper.toDto(saved);
    }

    @Transactional
    public void softDeleteUser(String usn) {
        log.info("Starting deletion process for user with PRN: {}", usn);

        // Verify user exists first
        User user = userRepository.findByUsn(usn)
                .orElseThrow(() -> new RuntimeException("User not found with PRN: " + usn));

//        // Step 2: Delete from profile service
//        log.info("Step 2/3: Deleting profile for user {}", prn);
//        profileManagementServiceClient.permanentlyDeleteProfile(prn);

        // Step 3: Delete from user service
        log.info("Step 3/3: Deleting user {} from user database", usn);
        user.setActive(false);
        userRepository.save(user);

        log.info("Successfully completed deletion of user with USN: {}", usn);
    }

    @Transactional
    public void deleteUser(String usn) {
        log.info("Starting hard deletion process for user with PRN: {}", usn);

        // Verify user exists first
        User user = userRepository.findByUsn(usn)
                .orElseThrow(() -> new RuntimeException("User not found with PRN: " + usn));

//        // Step 2: Delete from profile service
//        log.info("Step 2/3: Deleting profile for user {}", prn);
//        profileManagementServiceClient.permanentlyDeleteProfile(prn);

        // Step 3: Delete from user service
        log.info("Step 3/3: Deleting user {} from user database", usn);
        userRepository.delete(user);

        log.info("Successfully completed deletion of user with USN: {}", usn);
    }


    public boolean validateCredentials(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .map(u -> u.isVerified()
                        && passwordEncoder
                        .matches(rawPassword, u.getPassword()))
                .orElse(false);
    }


    public UserDto findByUsername(String username) {
        log.debug("Fetching user by username: {} from database (cache miss)", username);
        return userRepository.findByUsername(username)
                .map(UserMapper::toDto)
                .orElse(null);
    }


    public boolean validate(String usn) {
        log.debug("Validating user existence for USN: {} (cache miss)", usn);
        return userRepository.existsByUsn(usn);
    }


    public UserDto changeRole(String usn, Role role) {

        log.debug("Attempting to change role of prn: {}", usn);

        User user = userRepository.findByUsn(usn).orElseThrow();

        user.setRole(role);
        userRepository.save(user);

        return userRepository.findByUsn(usn)
                .map(UserMapper::toDto)
                .orElse(null);
    }


    @Transactional
    public UserDto changeEmail(String usn, String email) {

        log.info("Attempting to change email for PRN: {}", usn);

        User user = userRepository.findByUsn(usn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmail().equalsIgnoreCase(email)) {
            log.info("Same email provided. No change required.");
            return UserMapper.toDto(user);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        user.setEmail(email);

        user.setVerified(false);

        user.setOtp(null);
        user.setOtpExpiry(null);

        User savedUser = userRepository.save(user);

        otpService.generateAndSendOtpForUser(savedUser);

        log.info("Email updated and verification OTP sent for USN: {}", usn);

        return UserMapper.toDto(savedUser);
    }

    public List<UserDto> getUsersForUsns(List<String> usns) {

        log.info("Attempting to fetch users with usns: {}", usns);

        List<User> users = userRepository.findByUsnIn(usns);

        if(users == null){
            log.info("Failed to fetch data");
            throw new ServiceException("failed to fetch user data");
        }

        return UserMapper.toResponseList(users);

    }
}