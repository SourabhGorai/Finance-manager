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
public class FinanceUpdateReq {

    private Long financeId;
    private String usn;
    private double amount;
    private Type type;
    private Category category;
    private String note;

}
