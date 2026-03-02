package org.icc.pecesatierra.utils.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

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

        log.info("Se subió la foto {} a los servicios de Cloudinary.", result.get(result.get("url")));

        return result;
    }

    public void deletePhoto(String publicId) throws IOException {
        log.info("Se elimino la imagen {}", publicId);
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
