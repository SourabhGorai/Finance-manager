package com.finance.finance_service.mapper;

import com.finance.finance_service.dto.response.FinanceResponse;
import com.finance.finance_service.model.Finance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FinanceMapper {

    public static FinanceResponse toResponse(Finance finance, String name){

        if(finance==null) return null;

        return FinanceResponse.builder()
                .financeId(finance.getFinanceId())
                .usn(finance.getUsn())
                .name(name)
                .type(finance.getType().toString())
                .category(finance.getCategory().toString())
                .note(finance.getNote())
                .isDeleted(finance.isDeleted())
                .build();

    }

    public static List<FinanceResponse> toResponseList(
            Map<Finance, String> map
    ) {

        if (map == null || map.isEmpty()) {
            return List.of();
        }

        return map.entrySet()
                .stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();

    }

    public static List<FinanceResponse> toResponseList(List<Finance> finances, String name) {

        if (finances == null || finances.isEmpty()) {
            return List.of();
        }

        return finances.stream()
                .map(finance -> toResponse(finance, name))
                .toList();

    }

}
