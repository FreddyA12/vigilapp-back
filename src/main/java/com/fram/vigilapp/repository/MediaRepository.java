package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {
}
