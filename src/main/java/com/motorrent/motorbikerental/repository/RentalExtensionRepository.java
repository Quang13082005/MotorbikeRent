package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.RentalExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalExtensionRepository extends JpaRepository<RentalExtension, Long> {
}