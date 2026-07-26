package dev.barboud.taglens.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class ImageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${b2.bucket}")
    private String bucket;

    public ImageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // Upload — returns the key to save in your database
    public String uploadImage(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storageKey = "images/" + UUID.randomUUID() + extension;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(storageKey)
                        .contentType(file.getContentType()).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return storageKey;
    }

//    // Presigned URL — expires after given minutes
//    public String getImageLink(String key, int minutes) {
//        return s3Presigner.presignGetObject(
//                GetObjectPresignRequest.builder()
//                        .signatureDuration(Duration.ofMinutes(minutes))
//                        .getObjectRequest(r -> r.bucket(bucket).key(key))
//                        .build()
//        ).url().toString();
//    }
//
//    // Delete
//    public void deleteImage(String key) {
//        s3Client.deleteObject(
//                DeleteObjectRequest.builder()
//                        .bucket(bucket).key(key).build()
//        );
//    }

}
