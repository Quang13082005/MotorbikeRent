package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.TokenAuthentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TokenAuthenticationRepository extends JpaRepository<TokenAuthentication, Long> {
    Optional<TokenAuthentication> findByTokenValue(String tokenValue);
}