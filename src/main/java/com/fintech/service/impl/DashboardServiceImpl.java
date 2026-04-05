package com.fintech.service.impl;

import com.fintech.dto.response.DashboardSummaryResponse;
import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.dto.response.MonthlyTrendResponse;
import com.fintech.entity.RecordType;
import com.fintech.repository.FinancialRecordRepository;
import com.fintech.service.DashboardService;
import com.fintech.util.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final RecordMapper recordMapper;

    @Override
    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpense = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        Map<String, BigDecimal> incomeByCategory = buildCategoryMap(RecordType.INCOME);
        Map<String, BigDecimal> expenseByCategory = buildCategoryMap(RecordType.EXPENSE);

        List<FinancialRecordResponse> recentActivity =
                recordRepository.findTop10ByDeletedFalseOrderByTransactionDateDescCreatedAtDesc()
                        .stream()
                        .map(recordMapper::toResponse)
                        .toList();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .incomeByCategory(incomeByCategory)
                .expenseByCategory(expenseByCategory)
                .recentActivity(recentActivity)
                .build();
    }

    @Override
    public List<MonthlyTrendResponse> getMonthlyTrends(LocalDate from, LocalDate to) {

        List<Object[]> rows = recordRepository.monthlyTrends(from, to);

        Map<String, MonthlyTrendResponse.MonthlyTrendResponseBuilder> builders = new LinkedHashMap<>();

        for(Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            RecordType type = (RecordType) row[2];
            BigDecimal total = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);
            builders.computeIfAbsent(key, k ->
                    MonthlyTrendResponse.builder()
                            .year(year)
                            .month(month)
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpense(BigDecimal.ZERO)
            );

            MonthlyTrendResponse.MonthlyTrendResponseBuilder builder = builders.get(key);

            if(type == RecordType.INCOME) builder.totalIncome(total);
            else builder.totalExpense(total);
        }

        return builders.values().stream()
                .map(b -> {
                    MonthlyTrendResponse r = b.build();
                    r.setNetBalance(r.getTotalIncome().subtract(r.getTotalExpense()));

                    return r;
                })
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> buildCategoryMap(RecordType type) {
        return recordRepository.sumByCategory(type)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1],
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
