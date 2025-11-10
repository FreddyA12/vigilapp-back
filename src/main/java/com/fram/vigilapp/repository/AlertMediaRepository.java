package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.AlertMedia;
import com.fram.vigilapp.entity.id.AlertMediaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlertMediaRepository extends JpaRepository<AlertMedia, AlertMediaId> {

    @Query("SELECT am FROM AlertMedia am WHERE am.alert.id = :alertId")
    List<AlertMedia> findByAlertId(@Param("alertId") UUID alertId);
}
