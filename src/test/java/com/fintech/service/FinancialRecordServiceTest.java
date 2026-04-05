package com.fintech.service;

import com.fintech.dto.request.CreateRecordRequest;
import com.fintech.dto.request.UpdateRecordRequest;
import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.entity.FinancialRecord;
import com.fintech.entity.RecordType;
import com.fintech.entity.Role;
import com.fintech.entity.User;
import com.fintech.exception.ResourceNotFoundException;
import com.fintech.repository.FinancialRecordRepository;
import com.fintech.repository.UserRepository;
import com.fintech.service.impl.FinancialRecordServiceImpl;
import com.fintech.util.RecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialRecordService")
class FinancialRecordServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecordMapper recordMapper;

    @InjectMocks
    private FinancialRecordServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final UUID RECORD_ID  = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@test.com";

    private User adminUser;
    private FinancialRecord sampleRecord;
    private FinancialRecordResponse sampleResponse;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(USER_ID)
                .email(ADMIN_EMAIL)
                .name("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();

        sampleRecord = FinancialRecord.builder()
                .id(RECORD_ID)
                .amount(new BigDecimal("1500.00"))
                .type(RecordType.INCOME)
                .category("Salary")
                .transactionDate(LocalDate.now())
                .createdBy(USER_ID)
                .deleted(false)
                .build();

        sampleResponse = FinancialRecordResponse.builder()
                .id(RECORD_ID)
                .amount(new BigDecimal("1500.00"))
                .type(RecordType.INCOME)
                .category("Salary")
                .transactionDate(LocalDate.now())
                .createdBy(USER_ID)
                .build();
    }


    @Nested
    @DisplayName("createRecord()")
    class CreateRecord {

        @Test
        @DisplayName("should create and return a record when request is valid")
        void shouldCreateRecord_whenRequestIsValid() {

            CreateRecordRequest request = new CreateRecordRequest();
            request.setAmount(new BigDecimal("1500.00"));
            request.setType(RecordType.INCOME);
            request.setCategory("Salary");
            request.setTransactionDate(LocalDate.now());

            given(userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL))
                    .willReturn(Optional.of(adminUser));
            given(recordRepository.save(any(FinancialRecord.class)))
                    .willReturn(sampleRecord);
            given(recordMapper.toResponse(sampleRecord))
                    .willReturn(sampleResponse);

            // Act
            FinancialRecordResponse result = service.createRecord(request, ADMIN_EMAIL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(RECORD_ID);
            assertThat(result.getAmount()).isEqualByComparingTo("1500.00");
            assertThat(result.getType()).isEqualTo(RecordType.INCOME);

            then(recordRepository).should().save(any(FinancialRecord.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when creator user not found")
        void shouldThrow_whenCreatorUserNotFound() {
            // Arrange
            given(userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL))
                    .willReturn(Optional.empty());

            CreateRecordRequest request = new CreateRecordRequest();
            request.setAmount(new BigDecimal("100.00"));
            request.setType(RecordType.EXPENSE);
            request.setCategory("Office");
            request.setTransactionDate(LocalDate.now());

            // Act & Assert
            assertThatThrownBy(() -> service.createRecord(request, ADMIN_EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not found");

            then(recordRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("getRecordById()")
    class GetRecordById {

        @Test
        @DisplayName("should return record when it exists and is not deleted")
        void shouldReturnRecord_whenExists() {
            given(recordRepository.findByIdAndDeletedFalse(RECORD_ID))
                    .willReturn(Optional.of(sampleRecord));
            given(recordMapper.toResponse(sampleRecord))
                    .willReturn(sampleResponse);

            FinancialRecordResponse result = service.getRecordById(RECORD_ID);

            assertThat(result.getId()).isEqualTo(RECORD_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when record not found or deleted")
        void shouldThrow_whenRecordNotFound() {
            given(recordRepository.findByIdAndDeletedFalse(RECORD_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRecordById(RECORD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(RECORD_ID.toString());
        }
    }


    @Nested
    @DisplayName("updateRecord()")
    class UpdateRecord {

        @Test
        @DisplayName("should apply only non-null fields (PATCH semantics)")
        void shouldApplyOnlyNonNullFields() {
            UpdateRecordRequest request = new UpdateRecordRequest();
            request.setCategory("Updated Category");

            given(recordRepository.findByIdAndDeletedFalse(RECORD_ID))
                    .willReturn(Optional.of(sampleRecord));
            given(recordRepository.save(sampleRecord)).willReturn(sampleRecord);
            given(recordMapper.toResponse(sampleRecord)).willReturn(sampleResponse);

            service.updateRecord(RECORD_ID, request);

            assertThat(sampleRecord.getCategory()).isEqualTo("Updated Category");
            assertThat(sampleRecord.getAmount()).isEqualByComparingTo("1500.00"); // unchanged
            assertThat(sampleRecord.getType()).isEqualTo(RecordType.INCOME);      // unchanged
        }
    }

    @Nested
    @DisplayName("deleteRecord()")
    class DeleteRecord {

        @Test
        @DisplayName("should soft-delete by setting deleted=true")
        void shouldSoftDelete() {
            given(recordRepository.findByIdAndDeletedFalse(RECORD_ID))
                    .willReturn(Optional.of(sampleRecord));
            given(recordRepository.save(sampleRecord)).willReturn(sampleRecord);

            service.deleteRecord(RECORD_ID);

            assertThat(sampleRecord.isDeleted()).isTrue();
            then(recordRepository).should().save(sampleRecord);
        }

        @Test
        @DisplayName("should throw when trying to delete an already-deleted record")
        void shouldThrow_whenRecordAlreadyDeleted() {
            given(recordRepository.findByIdAndDeletedFalse(RECORD_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteRecord(RECORD_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
