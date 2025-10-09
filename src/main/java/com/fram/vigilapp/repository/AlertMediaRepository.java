package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.AlertMedia;
import com.fram.vigilapp.entity.id.AlertMediaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertMediaRepository extends JpaRepository<AlertMedia, AlertMediaId> {
}
