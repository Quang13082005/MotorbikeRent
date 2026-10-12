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

    private static String categoryKey(String value){String key=com.motorrent.motorbikerental.entity.TravelPlace.normalize(value).trim().replaceAll("[\\s_]+","-");return "xe-tay-ga".equals(key)?"xe-ga":key;}
    public List<MotorbikeResponse> getAllMotorbikes() {
        return motorbikeRepository.findAll().stream()
                .map(motorbikeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<MotorbikeResponse> filterMotorbikes(String keyword, String category, String priceFilter, String ccFilter, String statusFilter, String sortBy) {
        Integer minPrice = null;
        Integer maxPrice = null;
        if (priceFilter != null) {
            switch (priceFilter) {
                case "under120":
                    maxPrice = 119999;
                    break;
                case "120to180":
                    minPrice = 120000;
                    maxPrice = 180000;
                    break;
                case "above180":
                    minPrice = 180001;
                    break;
            }
        }

        Integer cc = null;
        if (ccFilter != null && !ccFilter.equals("all")) {
            try {
                cc = Integer.parseInt(ccFilter);
            } catch (NumberFormatException ignored) {}
        }

        com.motorrent.motorbikerental.enums.MotorbikeStatus status = null;
        if ("available".equals(statusFilter)) {
            status = com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE;
        }

        List<Motorbike> motorbikes = motorbikeRepository.filterMotorbikes(keyword, "all", minPrice, maxPrice, cc, status);

        if(category!=null && !"all".equals(category)) motorbikes.removeIf(m->m.getCategory()==null || !categoryKey(m.getCategory().getName()).equals(categoryKey(category)));
        // Sorting
        if (sortBy != null) {
            switch (sortBy) {
                case "popular":
                    motorbikes.sort(java.util.Comparator.comparing(Motorbike::getRating,java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
                    break;
                case "price-asc":
                    motorbikes.sort(java.util.Comparator.comparing(Motorbike::getPricePerDay));
                    break;
                case "price-desc":
                    motorbikes.sort(java.util.Comparator.comparing(Motorbike::getPricePerDay).reversed());
                    break;
                case "newest":
                    motorbikes.sort(java.util.Comparator.comparing(Motorbike::getYear).reversed());
                    break;
            }
        }

        return motorbikes.stream()
                .map(motorbikeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public MotorbikeDetailResponse getMotorbikeDetail(Long id) {
        Motorbike motorbike = motorbikeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND, "Không tìm thấy xe"));
        var result=motorbikeMapper.toDetailResponse(motorbike);
        result.setDepositAmount(PricingService.securityDeposit(motorbike));
        result.setAllImages(motorbike.getImages()==null?java.util.List.of():motorbike.getImages().stream().map(com.motorrent.motorbikerental.entity.MotorbikeImage::getImageUrl).toList());
        return result;
    }
}