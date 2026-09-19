package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.request.PromotionRequest;
import com.motorrent.motorbikerental.dto.response.PromotionResponse;
import com.motorrent.motorbikerental.entity.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PromotionMapper {
    PromotionResponse toResponse(Promotion promotion);
    void updateEntity(@MappingTarget Promotion promotion, PromotionRequest request);
}