package com.userservice.service;


import com.userservice.dto.UserCreateDto;
import com.userservice.dto.UserDto;
import com.userservice.dto.UserUpdateDto;

import com.userservice.model.User;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;

@Component
public class UserMapper {

    public static UserDto toDto(User u) {
        if (u == null) return null;
        return UserDto.builder()
                .usn(u.getUsn())
                .name(u.getName())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole().toString())
                .verified(u.isVerified())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public static List<UserDto> toResponseList(List<User> users){
        if(users == null || users.isEmpty()) return List.of();

        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    public static User toEntity(UserCreateDto dto) {
        if (dto == null) return null;
        return User.builder()
                .username(dto.getUsername())
                .name(sanitizeName(dto.getName()))
                .password(dto.getPassword())
                .email(dto.getEmail())
                .role(dto.getRole())
                .build();
    }

    public static void updateEntityFromDto(UserUpdateDto dto, User user) {
        if (dto == null || user == null) return;
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getPassword() != null) user.setPassword(dto.getPassword());
    }

    public static String sanitizeName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        // 1. Trim and normalize unicode (é → e)
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // 2. Remove all non-alphabetic characters except space
        normalized = normalized.replaceAll("[^a-zA-Z ]", "");

        // 3. Replace multiple spaces with single space
        normalized = normalized.replaceAll("\\s+", " ");

        // 4. Convert to proper case (optional but recommended)
        normalized = toTitleCase(normalized);

        return normalized;
    }

    private static String toTitleCase(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }

        return result.toString().trim();
    }
}
