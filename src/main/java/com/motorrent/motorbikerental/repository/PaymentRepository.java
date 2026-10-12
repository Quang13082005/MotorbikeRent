package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByRentalOrderIdOrderByCreatedAtDesc(Long id);
    java.util.Optional<Payment> findByProviderCode(Long code);
    @org.springframework.data.jpa.repository.Query("select p.rentalOrder.id from Payment p where p.providerCode=:code")
    java.util.Optional<Long> orderIdForCode(@org.springframework.data.repository.query.Param("code") Long code);
    @org.springframework.data.jpa.repository.Query("select p.sourceType from Payment p where p.providerCode=:code")
    java.util.Optional<String> sourceTypeForCode(@org.springframework.data.repository.query.Param("code") Long code);
    @org.springframework.data.jpa.repository.Query("select p.sourceId from Payment p where p.providerCode=:code")
    java.util.Optional<Long> sourceIdForCode(@org.springframework.data.repository.query.Param("code") Long code);
    @org.springframework.data.jpa.repository.Query("select p.rentalOrder.id from Payment p where p.id=:id")
    java.util.Optional<Long> orderIdForPayment(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.providerCode=:code")
    java.util.Optional<Payment> lockByProviderCode(@org.springframework.data.repository.query.Param("code") Long code);
    java.util.Optional<Payment> findByIdempotencyKey(String key);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.rentalOrder.id=:id order by p.id")
    java.util.List<Payment> lockForOrder(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.id=:id")
    java.util.Optional<Payment> lockById(@org.springframework.data.repository.query.Param("id") Long id);

}
