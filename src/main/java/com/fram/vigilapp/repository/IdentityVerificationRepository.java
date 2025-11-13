package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, UUID> {
}
