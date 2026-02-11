package org.icc.pecesatierra.web.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface CloudinaryService {
    Map<String, String> uploadPhoto(MultipartFile file, String folderLocation) throws IOException;

    void deletePhoto(String publicId) throws IOException;
}
