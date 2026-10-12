package com.motorrent.motorbikerental.mapper;
import com.motorrent.motorbikerental.dto.request.UserUpdateRequest;
import com.motorrent.motorbikerental.dto.response.UserResponse;
import com.motorrent.motorbikerental.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
    void updateEntity(@MappingTarget User user, UserUpdateRequest request);
}