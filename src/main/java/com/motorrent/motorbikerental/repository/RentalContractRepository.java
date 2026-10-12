package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
    java.util.Optional<RentalContract> findByRentalOrderId(Long id);
}