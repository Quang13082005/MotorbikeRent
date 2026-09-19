package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.Motorbike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeRepository extends JpaRepository<Motorbike, Long> {
}