package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.MotorbikeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeImageRepository extends JpaRepository<MotorbikeImage, Long> {
    java.util.List<MotorbikeImage> findByMotorbikeIdOrderBySortOrderAsc(Long id);
}