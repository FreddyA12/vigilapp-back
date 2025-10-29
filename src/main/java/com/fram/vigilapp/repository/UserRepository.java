package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.User;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    User findByEmail(String email);

    /**
     * Find all users whose configured zone intersects with the given alert point
     * Uses PostGIS ST_Intersects for geospatial query
     */
    @Query(value = "SELECT DISTINCT u.* FROM users u " +
            "INNER JOIN user_zones uz ON u.id = uz.user_id " +
            "WHERE ST_Intersects(uz.geometry, ST_SetSRID(:alertPoint, 4326)::geography) " +
            "AND u.status = 'ACTIVE'",
            nativeQuery = true)
    List<User> findUsersInZone(@Param("alertPoint") Point alertPoint);

    /**
     * Find all active users
     */
    List<User> findByStatus(String status);
}
