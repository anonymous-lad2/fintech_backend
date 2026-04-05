package com.fintech.service;


import com.fintech.dto.response.DashboardSummaryResponse;
import com.fintech.entity.RecordType;
import com.fintech.repository.FinancialRecordRepository;
import com.fintech.service.impl.DashboardServiceImpl;
import com.fintech.util.RecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService")
class DashboardServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private RecordMapper recordMapper;

    @InjectMocks
    private DashboardServiceImpl service;

    @Test
    @DisplayName("getSummary() — should compute correct net balance from income and expenses")
    void getSummary_shouldComputeNetBalance() {
        given(recordRepository.sumByType(RecordType.INCOME))
                .willReturn(new BigDecimal("5000.00"));
        given(recordRepository.sumByType(RecordType.EXPENSE))
                .willReturn(new BigDecimal("3000.00"));
        given(recordRepository.sumByCategory(RecordType.INCOME))
                .willReturn(Collections.emptyList());
        given(recordRepository.sumByCategory(RecordType.EXPENSE))
                .willReturn(Collections.emptyList());
        given(recordRepository.findTop10ByDeletedFalseOrderByTransactionDateDescCreatedAtDesc())
                .willReturn(List.of());

        DashboardSummaryResponse summary = service.getSummary();

        assertThat(summary.getTotalIncome()).isEqualByComparingTo("5000.00");
        assertThat(summary.getTotalExpense()).isEqualByComparingTo("3000.00");
        assertThat(summary.getNetBalance()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("getSummary() — should return zero net balance when income equals expenses")
    void getSummary_shouldReturnZeroNetBalance_whenBalanced() {
        given(recordRepository.sumByType(RecordType.INCOME))
                .willReturn(new BigDecimal("1000.00"));
        given(recordRepository.sumByType(RecordType.EXPENSE))
                .willReturn(new BigDecimal("1000.00"));
        given(recordRepository.sumByCategory(RecordType.INCOME)).willReturn(List.of());
        given(recordRepository.sumByCategory(RecordType.EXPENSE)).willReturn(List.of());
        given(recordRepository.findTop10ByDeletedFalseOrderByTransactionDateDescCreatedAtDesc())
                .willReturn(List.of());

        DashboardSummaryResponse summary = service.getSummary();

        assertThat(summary.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getSummary() — should handle negative net balance (expenses > income)")
    void getSummary_shouldHandleNegativeBalance() {
        given(recordRepository.sumByType(RecordType.INCOME))
                .willReturn(new BigDecimal("500.00"));
        given(recordRepository.sumByType(RecordType.EXPENSE))
                .willReturn(new BigDecimal("1200.00"));
        given(recordRepository.sumByCategory(RecordType.INCOME)).willReturn(List.of());
        given(recordRepository.sumByCategory(RecordType.EXPENSE)).willReturn(List.of());
        given(recordRepository.findTop10ByDeletedFalseOrderByTransactionDateDescCreatedAtDesc())
                .willReturn(List.of());

        DashboardSummaryResponse summary = service.getSummary();

        assertThat(summary.getNetBalance()).isEqualByComparingTo("-700.00");
        assertThat(summary.getNetBalance()).isNegative();
    }
}
