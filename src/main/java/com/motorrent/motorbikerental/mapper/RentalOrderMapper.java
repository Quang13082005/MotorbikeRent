package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import com.motorrent.motorbikerental.dto.response.RentalOrderResponse;
import com.motorrent.motorbikerental.dto.response.RentalDetailResponse;
import com.motorrent.motorbikerental.entity.RentalOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {UserMapper.class, MotorbikeMapper.class, PaymentMapper.class})
public interface RentalOrderMapper {
    @Mapping(source = "customer.firstName", target = "customerName")
    @Mapping(source = "customer.phone", target = "customerPhone")
    @Mapping(source = "customer.avatar", target = "customerAvatar")
    @Mapping(source = "motorbike.name", target = "motorbikeName")
    RentalOrderResponse toResponse(RentalOrder order);

    @Mapping(source = "customer.firstName", target = "customerName")
    @Mapping(source = "motorbike.name", target = "motorbikeName")
    RentalDetailResponse toDetailResponse(RentalOrder order);

    void updateEntity(@MappingTarget RentalOrder order, RentalOrderRequest request);
}