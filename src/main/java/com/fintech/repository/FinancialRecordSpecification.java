package com.fintech.repository;

import com.fintech.entity.FinancialRecord;
import com.fintech.entity.RecordType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

/**
 * Specification factory for {@link FinancialRecord} dynamic filtering.
 *
 * <p>Why Specification over derived query methods?
 * <ul>
 *   <li>N optional filter parameters would require 2^N derived method variants.</li>
 *   <li>Specifications compose predicates at runtime, keeping repository clean.</li>
 *   <li>Reusable across different query contexts (list, count, export).</li>
 * </ul>
 */

public final class FinancialRecordSpecification {

    private FinancialRecordSpecification() {
    }

    /**
     * Builds a composite {@link Specification} from optional filter parameters.
     * Any null parameter is silently ignored (i.e., no restriction applied for it).
     */
    public static Specification<FinancialRecord> withFilters(
            RecordType type,
            String category,
            LocalDate dateFrom,
            LocalDate dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted records
            predicates.add(cb.isFalse(root.get("deleted")));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("category")),
                        "%" + category.toLowerCase() + "%"));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}