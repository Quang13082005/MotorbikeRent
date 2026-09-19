package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.response.PaymentResponse;
import com.motorrent.motorbikerental.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}