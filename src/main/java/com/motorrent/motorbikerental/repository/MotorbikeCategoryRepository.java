package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.MotorbikeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeCategoryRepository extends JpaRepository<MotorbikeCategory, Long> {
}