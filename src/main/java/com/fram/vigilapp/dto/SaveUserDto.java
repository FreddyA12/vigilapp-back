package com.fram.vigilapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SaveUserDto {
    @NotBlank(message = "El nombre es obligatorio")
    String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    String lastName;

    @Email(message = "El correo no es válido")
    @NotBlank(message = "El correo es obligatorio")
    String email;

    @NotBlank(message = "La contraseña es obligatoria")
    String password;

    MultipartFile fotoCedula;

    MultipartFile selfie;
}
