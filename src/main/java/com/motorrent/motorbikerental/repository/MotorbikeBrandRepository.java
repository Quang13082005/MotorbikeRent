package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.MotorbikeBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeBrandRepository extends JpaRepository<MotorbikeBrand, Long> {
}