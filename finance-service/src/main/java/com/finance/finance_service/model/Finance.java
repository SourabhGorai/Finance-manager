package com.finance.finance_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financeId;

    @NotBlank(message = "Unique serial number is Required")
    private String usn;

    @NotNull(message = "Amount is required")
    @Min(value = 0)
    private Double amount;

    @NotNull(message = "INCOME/EXPENSE is required")
    @Enumerated(EnumType.STRING)
    private Type type;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    private Category category;

    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean isDeleted = false;

    public void delete() {
        this.isDeleted = true;
    }

}
