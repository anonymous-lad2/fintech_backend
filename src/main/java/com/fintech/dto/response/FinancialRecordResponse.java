package com.fintech.dto.response;

import com.fintech.entity.RecordType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class FinancialRecordResponse {

    private UUID id;
    private BigDecimal amount;
    private RecordType type;
    private String category;
    private LocalDate transactionDate;
    private String notes;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
