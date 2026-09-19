package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.dto.request.MotorbikeRequest;
import com.motorrent.motorbikerental.dto.response.MotorbikeDetailResponse;
import com.motorrent.motorbikerental.dto.response.MotorbikeResponse;
import com.motorrent.motorbikerental.entity.Motorbike;
import com.motorrent.motorbikerental.exception.AppException;
import com.motorrent.motorbikerental.exception.ErrorCode;
import com.motorrent.motorbikerental.mapper.MotorbikeMapper;
import com.motorrent.motorbikerental.repository.MotorbikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MotorbikeService {
    private final MotorbikeRepository motorbikeRepository;
    private final MotorbikeMapper motorbikeMapper;

    public List<MotorbikeResponse> getAllMotorbikes() {
        return motorbikeRepository.findAll().stream()
                .map(motorbikeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public MotorbikeDetailResponse getMotorbikeDetail(Long id) {
        Motorbike motorbike = motorbikeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND, "Không tìm thấy xe"));
        return motorbikeMapper.toDetailResponse(motorbike);
    }
}