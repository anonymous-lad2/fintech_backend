package com.fintech.service.impl;

import com.fintech.dto.request.CreateRecordRequest;
import com.fintech.dto.request.UpdateRecordRequest;
import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.entity.FinancialRecord;
import com.fintech.entity.RecordType;
import com.fintech.exception.ResourceNotFoundException;
import com.fintech.repository.FinancialRecordRepository;
import com.fintech.repository.FinancialRecordSpecification;
import com.fintech.repository.UserRepository;
import com.fintech.service.FinancialRecordService;
import com.fintech.util.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final RecordMapper recordMapper;


    @Override
    public FinancialRecordResponse createRecord(CreateRecordRequest request, String createdByEmail) {
        UUID creatorId = userRepository.findByEmailAndActiveTrue(createdByEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
                .getId();

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .transactionDate(request.getTransactionDate())
                .notes(request.getNotes())
                .createdBy(creatorId)
                .deleted(false)
                .build();

        FinancialRecord savedRecord = recordRepository.save(record);
        log.info("Created {} record of {} in category '{}' by user {}", savedRecord.getType(), savedRecord.getAmount(),
                savedRecord.getCategory(), createdByEmail);

        return recordMapper.toResponse(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(UUID id) {
        return recordMapper.toResponse(
                recordRepository.findByIdAndDeletedFalse(id)
                        .orElseThrow(() -> ResourceNotFoundException.forRecord(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FinancialRecordResponse> getRecords(RecordType type, String category, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {

        var spec = FinancialRecordSpecification.withFilters(type, category, dateFrom, dateTo);
        return recordRepository.findAll(spec, pageable)
                .map(recordMapper::toResponse);
    }

    @Override
    public FinancialRecordResponse updateRecord(UUID id, UpdateRecordRequest request) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ResourceNotFoundException.forRecord(id));

        if(request.getAmount() != null) record.setAmount(request.getAmount());
        if(request.getType() != null) record.setType(record.getType());
        if(request.getCategory() != null) record.setCategory(request.getCategory());
        if(request.getTransactionDate() != null) record.setTransactionDate(request.getTransactionDate());
        if(request.getNotes() != null) record.setNotes(record.getNotes());

        return recordMapper.toResponse(recordRepository.save(record));
    }

    @Override
    public void deleteRecord(UUID id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> ResourceNotFoundException.forRecord(id));

        record.setDeleted(true);
        recordRepository.save(record);
        log.info("Soft-deleted financial record {}", id);
    }
}
