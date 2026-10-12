package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
 java.util.Optional<Promotion> findByCode(String code);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select p from Promotion p where p.code=:code")
 java.util.Optional<Promotion> lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}