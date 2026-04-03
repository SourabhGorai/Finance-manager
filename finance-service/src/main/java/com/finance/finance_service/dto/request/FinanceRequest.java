package com.finance.finance_service.dto.request;

import com.finance.finance_service.model.Category;
import com.finance.finance_service.model.Type;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinanceRequest {

    @NotNull
    private double amount;

    @NotNull
    private Type type;

    @NotNull
    private Category category;

    private String note;
}
