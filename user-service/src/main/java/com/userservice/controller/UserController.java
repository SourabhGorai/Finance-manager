package com.userservice.controller;


import com.userservice.dto.ApiResponse;
import com.userservice.dto.UserCreateDto;
import com.userservice.dto.UserDto;
import com.userservice.dto.UserUpdateDto;
import com.userservice.model.Role;
import com.userservice.model.User;
import com.userservice.security.JwtUtil;
import com.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        log.info("Request received to get all users");
        List<UserDto> resp = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetch %d records", resp.size()),
                resp
        ));
    }


    @GetMapping("/getAll/paged")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Request received to get all users (paginated) - page={}, size={}", page, size);
        return ResponseEntity.ok(ApiResponse.success(
                "fetched successfully",
                userService.getAllUsersPaginated(page, size)
        ));
    }


    @GetMapping("/{usn}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable String usn) {

        log.info("Request received to get user by ID");

        UserDto dto = userService.getUserByUsn(usn);
        log.info("{}", dto);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(
                "Fetched data successfully",
                dto
        ));

    }

    @PostMapping("/getUsers")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(@RequestBody List<String> usns) {

        log.info("Request received to fetch user data for usns: {}", usns);

        List<UserDto> users = userService.getUsersForUsns(usns);

        if (users == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Fetch %d records", users.size()),
                users
        ));

    }


    @PutMapping("/{usn}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String usn,
            @RequestBody UserUpdateDto dto
    ) {

        log.info("Request received to update user by Usn: {}", usn);

        try {
            UserDto updated = userService.updateUser(usn, dto);
            return ResponseEntity.ok(ApiResponse.success(
                    "User updated successfully",
                    updated
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to update user details"));
        }

    }

    @DeleteMapping("/soft/{usn}")
    public ResponseEntity<ApiResponse<?>> deleteUser(
            @PathVariable String usn,
            HttpServletRequest req
    ) {
        try {
            String token = req.getHeader("Authorization").substring(7);
            String requesterUsn = jwtUtil.extractUsn(token);
            String requesterRole = jwtUtil.extractRole(token);

            if (!requesterUsn.equals(usn) && !requesterRole.equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            userService.softDeleteUser(usn);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/hard/{usn}")
    public ResponseEntity<ApiResponse<?>> permanentlyDeleteUser(
            @PathVariable String usn,
            HttpServletRequest req
    ) {
        try {
            String token = req.getHeader("Authorization").substring(7);
            String requesterUsn = jwtUtil.extractUsn(token);
            String requesterRole = jwtUtil.extractRole(token);

            if (!requesterUsn.equals(usn) && !requesterRole.equals("ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            userService.deleteUser(usn);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/validate/{usn}")
    public Boolean validateUser(@PathVariable String usn) {
        try {
            return userService.validate(usn);
        } catch (NotFoundException e) {
            log.info("User with PRN {} does not exist", usn);
            return false;
        }
    }

    @PutMapping("/changeRole/{usn}/{role}")
    public ResponseEntity<ApiResponse<UserDto>> changeRole(
            @PathVariable String usn,
            @PathVariable Role role
    ) {
        log.debug("Request received to change role of prn {}", usn);
        UserDto resp = userService.changeRole(usn, role);
        return ResponseEntity.ok(ApiResponse.success(
                "Role changed successfully",
                resp
        ));
    }

    @PutMapping("/changeEmail/{usn}/{email}")
    public ResponseEntity<ApiResponse<UserDto>> changeEmail(
            @PathVariable String usn,
            @PathVariable String email
    ) {
        log.info("Request received to change email for prn: {}", usn);
        UserDto resp = userService.changeEmail(usn, email);
        return ResponseEntity.ok(ApiResponse.success(
                "Email changed successfully",
                resp
        ));
    }



}
