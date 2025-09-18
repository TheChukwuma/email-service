package com.octopus.email_service.repository;

import com.octopus.email_service.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    
    Optional<Template> findByName(String name);
    
    @Query("SELECT t FROM Template t WHERE t.name = :name AND t.isActive = true")
    Optional<Template> findActiveByName(@Param("name") String name);
    
    @Query("SELECT t FROM Template t WHERE t.isActive = true")
    List<Template> findAllActive();
    
    @Query("SELECT t FROM Template t WHERE t.createdBy.id = :userId AND t.isActive = true")
    List<Template> findActiveByCreatedBy(@Param("userId") Long userId);
    
    boolean existsByName(String name);
}
