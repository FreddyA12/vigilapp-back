package com.fram.vigilapp.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    UUID id;
    String firstName;
    String lastName;
    String email;
}
