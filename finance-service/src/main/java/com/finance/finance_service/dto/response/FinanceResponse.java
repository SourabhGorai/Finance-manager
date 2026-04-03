package com.finance.finance_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.finance.finance_service.model.Category;
import com.finance.finance_service.model.Type;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinanceResponse {

    private Long financeId;
    private String usn;
    private String name;
    private Double amount;
    private String type;
    private String category;
    private String note;
    private boolean isDeleted;

}
