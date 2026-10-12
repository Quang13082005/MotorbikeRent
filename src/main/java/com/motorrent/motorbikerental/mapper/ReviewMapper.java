package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.request.ReviewRequest;
import com.motorrent.motorbikerental.dto.response.ReviewResponse;
import com.motorrent.motorbikerental.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(source = "customer.firstName", target = "customerName")
    ReviewResponse toResponse(Review review);
    void updateEntity(@MappingTarget Review review, ReviewRequest request);
}