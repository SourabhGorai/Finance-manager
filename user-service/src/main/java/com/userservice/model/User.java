package com.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users2",
        indexes = {
                @Index(name = "idx_users2_usn",                        columnList = "usn"),
                @Index(name = "idx_users2_username",                   columnList = "username"),
                @Index(name = "idx_users2_email",                      columnList = "email"),
                @Index(name = "idx_users2_role",                       columnList = "role"),
                @Index(name = "idx_users2_is_verified",                columnList = "is_verified")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    @Id
    @Column(nullable = false, unique = true, length = 20)
    private String usn;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ----- HELPERS ----------------------------------------------

    @PrePersist
    public void generateUsn() {
        if (this.usn == null) {
            this.usn = generateCustomUsn();
        }
    }

    private String generateCustomUsn() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        String random = java.util.UUID.randomUUID()
                .toString()
                .replaceAll("-", "")
                .substring(0, 6)
                .toUpperCase();

        return "USR" + date + random; // e.g., USR20260402A1B2C3
    }
}