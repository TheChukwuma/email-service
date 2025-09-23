package com.octopus.email_service.repository;

import com.octopus.email_service.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    
    Optional<SystemSetting> findBySettingKey(String settingKey);
    
    @Query("SELECT s.settingValue FROM SystemSetting s WHERE s.settingKey = :key")
    Optional<String> findValueByKey(@Param("key") String key);
    
    @Query(value = "SELECT is_superadmin_setup_completed()", nativeQuery = true)
    Boolean isSuperAdminSetupCompleted();
}
