package com.fintech.controller;

import com.fintech.dto.request.CreateRecordRequest;
import com.fintech.dto.request.UpdateRecordRequest;
import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.entity.RecordType;
import com.fintech.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Tag(name = "Financial Records", description = "CRUD and filtering for financial entries")
@SecurityRequirement(name = "bearerAuth")
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @GetMapping
    @Operation(summary = "List records with operational filtering and pagination")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<Page<FinancialRecordResponse>> getRecords(
            @RequestParam(required = false)RecordType type,
            @RequestParam(required = false)String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate dateTo,
            @PageableDefault(size = 20, sort = "transactionDate")Pageable pageable
    ) {
        return ResponseEntity.ok(recordService.getRecords(type, category, dateFrom, dateTo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single financial record by ID")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    public ResponseEntity<FinancialRecordResponse> getRecordById(@PathVariable UUID id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @PostMapping
    @Operation(summary = "Create a financial record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinancialRecordResponse> createRecord(
            @Valid @RequestBody CreateRecordRequest request,
            Authentication authentication) {

        String createdByEmail = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(request, createdByEmail));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a financial record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinancialRecordResponse> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a financial record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRecord(@PathVariable UUID id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
