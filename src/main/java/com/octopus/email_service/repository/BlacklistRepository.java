package com.octopus.email_service.repository;

import com.octopus.email_service.entity.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    
    Optional<Blacklist> findByEmailAddress(String emailAddress);
    
    boolean existsByEmailAddress(String emailAddress);
    
    @Query("SELECT b FROM Blacklist b WHERE LOWER(b.emailAddress) = LOWER(:emailAddress)")
    Optional<Blacklist> findByEmailAddressIgnoreCase(@Param("emailAddress") String emailAddress);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Blacklist b WHERE LOWER(b.emailAddress) = LOWER(:emailAddress)")
    boolean existsByEmailAddressIgnoreCase(@Param("emailAddress") String emailAddress);
}
