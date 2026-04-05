package com.fintech.repository;

import com.fintech.entity.FinancialRecord;
import com.fintech.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, UUID>, JpaSpecificationExecutor<FinancialRecord> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(UUID id);

    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

    /**
     * Sums all amounts for a given record type (INCOME or EXPENSE),
     * excluding soft-deleted records.
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
            "WHERE r.type = :type AND r.deleted = false")
    BigDecimal sumByType(@Param("type") RecordType type);

    /**
     * Returns per-category totals for a given record type.
     * Result is projected as Object[] {category, total}.
     */
    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
            "WHERE r.type = :type AND r.deleted = false " +
            "GROUP BY r.category ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumByCategory(@Param("type") RecordType type);

    /**
     * Monthly aggregation: returns {year, month, type, total} tuples
     * within a date range — used for trend charts.
     */
    @Query("SELECT YEAR(r.transactionDate), MONTH(r.transactionDate), r.type, SUM(r.amount) " +
            "FROM FinancialRecord r " +
            "WHERE r.transactionDate BETWEEN :from AND :to AND r.deleted = false " +
            "GROUP BY YEAR(r.transactionDate), MONTH(r.transactionDate), r.type " +
            "ORDER BY YEAR(r.transactionDate), MONTH(r.transactionDate)")
    List<Object[]> monthlyTrends(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Recent activity: latest N non-deleted records.
     */
    List<FinancialRecord> findTop10ByDeletedFalseOrderByTransactionDateDescCreatedAtDesc();
}
