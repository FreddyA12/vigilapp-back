package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserZoneRepository extends JpaRepository<UserZone, UUID> {
    Optional<UserZone> findByUser(User user);
    Optional<UserZone> findByUserId(UUID userId);
}
