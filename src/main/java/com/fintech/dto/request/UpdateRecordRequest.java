package com.fintech.dto.request;

import com.fintech.entity.RecordType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateRecordRequest {

    @DecimalMin(value = "0.0001", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount exceeds allowed precision")
    private BigDecimal amount;

    private RecordType type;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
