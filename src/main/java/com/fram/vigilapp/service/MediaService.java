package com.fram.vigilapp.service;

import com.fram.vigilapp.entity.Media;
import com.fram.vigilapp.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {

    /**
     * Procesa y guarda un archivo de evidencia para una alerta.
     * Si es una imagen, automáticamente detecta y difumina caras.
     *
     * @param file Archivo a procesar
     * @param user Usuario propietario del archivo
     * @param forBlurAnalysis Si se debe analizar y difuminar caras
     * @return Media entity guardada
     */
    Media processAndSaveMedia(MultipartFile file, User user, boolean forBlurAnalysis);

    /**
     * Procesa múltiples archivos de evidencia.
     *
     * @param files Archivos a procesar
     * @param user Usuario propietario
     * @param forBlurAnalysis Si se debe analizar y difuminar caras
     * @return Lista de Media entities guardadas
     */
    List<Media> processAndSaveMultipleMedia(List<MultipartFile> files, User user, boolean forBlurAnalysis);

    /**
     * Difumina caras en una imagen usando el servicio de Python.
     *
     * @param imageBytes Bytes de la imagen
     * @param filename Nombre del archivo
     * @return Bytes de la imagen procesada con caras difuminadas
     */
    byte[] blurFacesInImage(byte[] imageBytes, String filename);
}
