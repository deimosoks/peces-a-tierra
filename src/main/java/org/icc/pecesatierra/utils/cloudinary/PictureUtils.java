package org.icc.pecesatierra.utils.cloudinary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.icc.pecesatierra.exceptions.images.InvalidImageFormatException;
import org.icc.pecesatierra.exceptions.server.ServerErrorException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PictureUtils {

    private final CloudinaryService cloudinaryService;

    public Map<String, String> validateAndSavePicture(MultipartFile file, String folderLocation) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageFormatException("El archivo está vacío o no existe.");
        }

        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());

            boolean isValidFormat = mimeType.equals("image/jpeg") ||
                    mimeType.equals("image/png") ||
                    mimeType.equals("image/webp");

            if (!isValidFormat) {
                log.warn("Intento de subida de formato no permitido: {}", mimeType);
                throw new InvalidImageFormatException("Formato de imagen no permitido. Solo se acepta JPG, PNG y WEBP.");
            }

            return cloudinaryService.uploadPhoto(file, folderLocation);

        } catch (IOException e) {
            log.error("Error al procesar el archivo: {}", e.getMessage());
            throw new ServerErrorException();
        }
    }

    public void delete(String publicId) {
        try {
            cloudinaryService.deletePhoto(publicId);
        } catch (IOException e) {
            log.error("""
                            Error al eliminar imagen {}.
                            Error: {}
                            StackTrace: {}
                            """
                    , publicId
                    , e.getMessage()
                    , e.getStackTrace());
            throw new ServerErrorException();
        }
    }

}
