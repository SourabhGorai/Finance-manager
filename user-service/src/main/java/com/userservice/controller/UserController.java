package com.userservice.controller;


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
    public ResponseEntity<List<UserDto>> getAllUsers() {
        log.info("Request received to get all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/getAll/paged")
    public ResponseEntity<Page<UserDto>> getAllUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Request received to get all users (paginated) - page={}, size={}", page, size);
        return ResponseEntity.ok(userService.getAllUsersPaginated(page, size));
    }


    @GetMapping("/{usn}")
    public ResponseEntity<UserDto> getUser(@PathVariable String usn) {

        log.info("Request received to get user by ID");

        UserDto dto = userService.getUserByUsn(usn);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);

    }

    @PostMapping("/getUsers")
    public ResponseEntity<List<UserDto>> getUsers(@RequestBody List<String> usns) {

        log.info("Request received to fetch user data for usns: {}", usns);

        List<UserDto> users = userService.getUsersForUsns(usns);

        if(users == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(users);

    }


    @PutMapping("/{usn}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String usn,
            @RequestBody UserUpdateDto dto
    ) {

        log.info("Request received to update user by Usn: {}", usn);

        try {
            UserDto updated = userService.updateUser(usn, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }

    }

    @DeleteMapping("/soft/{usn}")
    public ResponseEntity<?> deleteUser(
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
    public ResponseEntity<?> permanentlyDeleteUser(
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
    public Boolean validateUser(@PathVariable String usn){
        try{
            return userService.validate(usn);
        }catch(NotFoundException e){
            log.info("User with PRN {} does not exist", usn);
            return false;
        }
    }

    @PutMapping("/changeRole/{usn}/{role}")
    public ResponseEntity<UserDto> changeRole(
            @PathVariable String usn,
            @PathVariable Role role
    ) {
        log.debug("Request received to change role of prn {}", usn);
        UserDto resp = userService.changeRole(usn, role);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/changeEmail/{usn}/{email}")
    public ResponseEntity<UserDto> changeEmail(
            @PathVariable String usn,
            @PathVariable String email
    ) {
        log.info("Request received to change email for prn: {}", usn);
        UserDto resp = userService.changeEmail(usn, email);
        return ResponseEntity.ok(resp);
    }

}
