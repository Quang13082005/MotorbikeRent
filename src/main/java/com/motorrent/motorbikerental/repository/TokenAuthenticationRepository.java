package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.TokenAuthentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TokenAuthenticationRepository extends JpaRepository<TokenAuthentication, Long> {
    Optional<TokenAuthentication> findByTokenValue(String tokenValue);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TokenAuthentication t set t.isUsed=false where t.user.id=:id")
    void revokeAll(@org.springframework.data.repository.query.Param("id") String id);
}