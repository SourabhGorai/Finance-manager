package com.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkOtpVerifyRequestDto {

    @NotEmpty
    private List<OtpItem> requests;

    @Data
    public static class OtpItem {
        @NotBlank
        private String email;

        @NotBlank
        private String otp;
    }
}
