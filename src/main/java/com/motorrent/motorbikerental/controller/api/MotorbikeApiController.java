package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.dto.common.ApiResponse;
import com.motorrent.motorbikerental.dto.response.MotorbikeDetailResponse;
import com.motorrent.motorbikerental.dto.response.MotorbikeResponse;
import com.motorrent.motorbikerental.service.MotorbikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/motorbikes")
@RequiredArgsConstructor
public class MotorbikeApiController {
    private final MotorbikeService motorbikeService;

    @GetMapping
    public ApiResponse<List<MotorbikeResponse>> getAll() {
        return ApiResponse.success(motorbikeService.getAllMotorbikes());
    }

    @GetMapping("/{id}")
    public ApiResponse<MotorbikeDetailResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(motorbikeService.getMotorbikeDetail(id));
    }
}