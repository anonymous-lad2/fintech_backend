package com.fintech.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "financial_records", indexes = {
        @Index(name = "idx_records_type", columnList = "type"),
        @Index(name = "idx_records_category", columnList = "category"),
        @Index(name = "idx_records_date", columnList = "transactionDate"),
        @Index(name = "idx_records_deleted", columnList = "deleted")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Amount stored with up to 19 digits and 4 decimal places to handle currencies with sub-cent precision
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordType type;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(length = 500)
    private String notes;

    // soft-delete flag: deleted records can be included for admin audit purpose
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
