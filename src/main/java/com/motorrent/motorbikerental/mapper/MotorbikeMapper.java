package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.request.MotorbikeRequest;
import com.motorrent.motorbikerental.dto.response.MotorbikeResponse;
import com.motorrent.motorbikerental.dto.response.MotorbikeDetailResponse;
import com.motorrent.motorbikerental.entity.Motorbike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MotorbikeMapper {
    @Mapping(source = "brand.name", target = "brand")
    @Mapping(source = "category.name", target = "category")
    MotorbikeResponse toResponse(Motorbike motorbike);

    @Mapping(source = "brand.name", target = "brand")
    @Mapping(source = "category.name", target = "category")
    @Mapping(source = "station.name", target = "station")
    MotorbikeDetailResponse toDetailResponse(Motorbike motorbike);

    void updateEntity(@MappingTarget Motorbike motorbike, MotorbikeRequest request);
}