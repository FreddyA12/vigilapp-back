package com.fram.vigilapp.service;

import com.fram.vigilapp.dto.SaveUserZoneDto;
import com.fram.vigilapp.dto.UserZoneDto;
import com.fram.vigilapp.entity.User;

import java.util.UUID;

public interface UserZoneService {
    UserZoneDto createOrUpdateUserZone(User user, SaveUserZoneDto saveUserZoneDto);
    UserZoneDto getUserZone(UUID userId);
    void deleteUserZone(UUID userId);
}
