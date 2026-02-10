package org.icc.pecesatierra.utils;

import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.icc.pecesatierra.exceptions.InvalidImageFormatException;
import org.icc.pecesatierra.exceptions.ServerErrorException;
import org.icc.pecesatierra.web.services.CloudinaryService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class PictureUtils {

    private CloudinaryService cloudinaryService;

    public Map<String, String> validateAndSavePicture(MultipartFile file) {
        Map<String, String> pictureData = new HashMap<>();

        try {
            Tika tika = new Tika();

            if (file != null) {

                if (!tika.detect(file.getBytes()).startsWith("image/"))
                    throw new InvalidImageFormatException("Formato de imagen invalido.");

                String originalName = file.getOriginalFilename();

                if (originalName == null || !originalName.contains("."))
                    throw new InvalidImageFormatException("Formato de imagen invalido.");

                if (originalName.contains("svg"))
                    throw new InvalidImageFormatException("No se permiten imágenes con extension svg.");

            pictureData = cloudinaryService.uploadMemberPhoto(file);

            }

        } catch (IOException e) {
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde");
        }

        return pictureData;

    }

    public void delete(String publicId){
        try{
            cloudinaryService.deletePhoto(publicId);
        }catch (IOException e){
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde");
        }
    }

}
