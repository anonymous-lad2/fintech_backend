package com.fintech.service;

import com.fintech.dto.response.DashboardSummaryResponse;
import com.fintech.dto.response.MonthlyTrendResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardSummaryResponse getSummary();

    List<MonthlyTrendResponse> getMonthlyTrends(LocalDate from, LocalDate to);
}
