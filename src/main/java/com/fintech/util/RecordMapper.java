package com.fintech.util;

import com.fintech.dto.response.FinancialRecordResponse;
import com.fintech.entity.FinancialRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecordMapper {
    FinancialRecordResponse toResponse(FinancialRecord record);
}
