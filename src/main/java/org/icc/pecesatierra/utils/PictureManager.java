package org.icc.pecesatierra.utils;

import org.apache.tika.Tika;
import org.icc.pecesatierra.exceptions.InvalidImageFormatException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class PictureManager {

    private final Path rootLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public String validatePicture(MultipartFile file) {
        String newFileName = "";

        try {
            Tika tika = new Tika();

            if (file != null) {

                if (!tika.detect(file.getBytes()).startsWith("image/"))
                    throw new InvalidImageFormatException("Invalid image format");

                String originalName = file.getOriginalFilename();

                if (originalName == null || !originalName.contains("."))
                    throw new InvalidImageFormatException("Invalid image format.");

                newFileName = UUID.randomUUID() + originalName.substring(originalName.lastIndexOf("."));

                save(file, newFileName);

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return newFileName;

    }

    public void save(MultipartFile file, String newFileName) {
        try {
            Path destinationFile = rootLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), destinationFile);
        } catch (IOException e) {

        }
    }

    public void delete(String url) {
        Path path = Path.of(url.substring(1));
        System.out.println(path.toAbsolutePath());;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {

        }
    }

}
