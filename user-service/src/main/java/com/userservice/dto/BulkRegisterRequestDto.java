package com.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkRegisterRequestDto {

    @NotEmpty(message = "User list cannot be empty")
    @Valid
    private List<UserCreateDto> users;
}
