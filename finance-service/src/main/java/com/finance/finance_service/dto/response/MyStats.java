package com.finance.finance_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyStats {

    private String usn;
    private String name;
    private Double totalIncome;
    private Double totalExpense;
    private Double netBalance;

}
