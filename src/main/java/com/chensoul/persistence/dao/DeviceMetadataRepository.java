package com.chensoul.persistence.dao;

import com.chensoul.persistence.model.DeviceMetadata;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceMetadataRepository extends JpaRepository<DeviceMetadata, Long> {

    List<DeviceMetadata> findByUserId(Long userId);
}
