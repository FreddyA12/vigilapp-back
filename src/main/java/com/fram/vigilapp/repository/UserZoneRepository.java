package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.UserZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserZoneRepository extends JpaRepository<UserZone, UUID> {
}
