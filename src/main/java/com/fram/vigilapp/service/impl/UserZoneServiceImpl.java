package com.fram.vigilapp.service.impl;

import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;
import com.fram.vigilapp.entity.UserZone;
import com.fram.vigilapp.repository.UserZoneRepository;
import com.fram.vigilapp.service.UserZoneService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserZoneServiceImpl implements UserZoneService {

    private final UserZoneRepository userZoneRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Override
    @Transactional
    public UserZoneDto createOrUpdateUserZone(User user, SaveUserZoneDto saveUserZoneDto) {
        UserZone userZone = userZoneRepository.findByUser(user).orElse(null);

        Point centerPoint = geometryFactory.createPoint(
                new Coordinate(saveUserZoneDto.getCenterLongitude(), saveUserZoneDto.getCenterLatitude())
        );
        centerPoint.setSRID(4326);

        Polygon zonePolygon = createCirclePolygon(
                saveUserZoneDto.getCenterLatitude(),
                saveUserZoneDto.getCenterLongitude(),
                saveUserZoneDto.getRadiusM()
        );

        if (userZone == null) {
            userZone = UserZone.builder()
                    .user(user)
                    .geometry(zonePolygon)
                    .radiusM(saveUserZoneDto.getRadiusM())
                    .build();
        } else {
            userZone.setGeometry(zonePolygon);
            userZone.setRadiusM(saveUserZoneDto.getRadiusM());
        }

        userZone = userZoneRepository.save(userZone);

        return mapToDto(userZone, saveUserZoneDto.getCenterLatitude(), saveUserZoneDto.getCenterLongitude());
    }

    @Override
    @Transactional(readOnly = true)
    public UserZoneDto getUserZone(UUID userId) {
        UserZone userZone = userZoneRepository.findByUserId(userId)
                .orElse(null);

        if (userZone == null) {
            return null;
        }

        Coordinate centroid = userZone.getGeometry().getCentroid().getCoordinate();

        return mapToDto(userZone, centroid.y, centroid.x);
    }

    @Override
    @Transactional
    public void deleteUserZone(UUID userId) {
        userZoneRepository.findByUserId(userId).ifPresent(userZoneRepository::delete);
    }

    private Polygon createCirclePolygon(Double latitude, Double longitude, Integer radiusM) {
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(32);
        shapeFactory.setCentre(new Coordinate(longitude, latitude));

        double radiusDegrees = radiusM / 111320.0;
        shapeFactory.setSize(radiusDegrees * 2);

        Polygon polygon = shapeFactory.createCircle();
        polygon.setSRID(4326);

        return polygon;
    }

    private UserZoneDto mapToDto(UserZone userZone, Double centerLat, Double centerLon) {
        return UserZoneDto.builder()
                .id(userZone.getId())
                .userId(userZone.getUser().getId())
                .centerLatitude(centerLat)
                .centerLongitude(centerLon)
                .radiusM(userZone.getRadiusM())
                .build();
    }
}
