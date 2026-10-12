package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.Motorbike;
import com.motorrent.motorbikerental.enums.MotorbikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MotorbikeRepository extends JpaRepository<Motorbike, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Motorbike m where m.id=:id")
    java.util.Optional<Motorbike> lockById(@Param("id") Long id);

    @Query("SELECT m FROM Motorbike m WHERE " +
           "(:category = 'all' OR m.category.name = :category) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:minPrice IS NULL OR m.pricePerDay >= :minPrice) AND " +
           "(:maxPrice IS NULL OR m.pricePerDay <= :maxPrice) AND " +
           "(:cc IS NULL OR m.cc = :cc) AND " +
           "(:status IS NULL OR m.status = :status)")
    List<Motorbike> filterMotorbikes(@Param("keyword") String keyword,
                                     @Param("category") String category,
                                     @Param("minPrice") Integer minPrice,
                                     @Param("maxPrice") Integer maxPrice,
                                     @Param("cc") Integer cc,
                                     @Param("status") MotorbikeStatus status);
}