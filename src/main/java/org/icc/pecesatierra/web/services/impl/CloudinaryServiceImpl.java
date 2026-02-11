package org.icc.pecesatierra.web.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.web.services.CloudinaryService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public Map<String, String> uploadPhoto(MultipartFile file, String folderLocation) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folderLocation,
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true
                )
        );

        Map<String, String> result = new HashMap<>();
        result.put("url", uploadResult.get("secure_url").toString());
        result.put("publicId", uploadResult.get("public_id").toString());
        return result;
    }

    @Override
    public void deletePhoto(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
