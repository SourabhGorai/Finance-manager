package com.userservice.dto;

import com.userservice.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto implements Serializable {
    private String usn;
    private String username;
    private String name;
    private String email;
    private String role;
    private boolean verified;
    private LocalDateTime createdAt;
}
