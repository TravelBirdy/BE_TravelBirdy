package com.travelbird.file.repository;

import com.travelbird.file.domain.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    @Modifying
    @Query("delete from FileAsset f where f.user.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
