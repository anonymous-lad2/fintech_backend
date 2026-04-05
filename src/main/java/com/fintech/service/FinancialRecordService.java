package com.fintech.service;

import com.fintech.dto.request.CreateRecordRequest;
import com.fintech.dto.request.UpdateRecordRequest;
import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.entity.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface FinancialRecordService {
    FinancialRecordResponse createRecord(CreateRecordRequest request, String createdByEmail);

    FinancialRecordResponse getRecordById(UUID id);

    Page<FinancialRecordResponse> getRecords(RecordType type, String category, LocalDate dateFrom, LocalDate dateTo,
                                             Pageable pageable);

    FinancialRecordResponse updateRecord(UUID id, UpdateRecordRequest request);

    void deleteRecord(UUID id);
}
