package com.nhom611.jobsvc.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class ProposalFileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public ProposalFileStorageService(
            MinioClient minioClient,
            @Value("${storage.minio.bucket:proposal-files}") String bucketName
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    public void upload(String objectKey, MultipartFile file, String proposalId) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                            .build()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload proposal attachment for proposalId=" + proposalId, ex);
        }
    }

    public StoredFile download(String objectKey, String fileName, String contentType) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectKey).build()
        )) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            inputStream.transferTo(buffer);
            return new StoredFile(buffer.toByteArray(), fileName, contentType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to download proposal attachment objectKey=" + objectKey, ex);
        }
    }

    public record StoredFile(byte[] content, String fileName, String contentType) {
    }
}