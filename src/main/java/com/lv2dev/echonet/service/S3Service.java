package com.lv2dev.echonet.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
public class S3Service {

    private final AmazonS3 s3Client;

    @Autowired
    public S3Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    // S3 버킷 이름
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String path, String keyName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileExtension = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf('.') + 1);

        String fullKeyName = path + "/" + keyName + "." + fileExtension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        s3Client.putObject(new PutObjectRequest(bucketName, fullKeyName, file.getInputStream(), metadata));

        return s3Client.getUrl(bucketName, fullKeyName).toString();
    }

    public byte[] getFile(String keyName) throws IOException {
        S3Object s3object = s3Client.getObject(new GetObjectRequest(bucketName, keyName));
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int len;
        byte[] buffer = new byte[1024];
        while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toByteArray();
    }

    public void deleteFile(String fileUrl) {
        // URL에서 객체의 키 추출
        String keyName = extractKeyFromUrl(fileUrl);
        s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
    }

    private String extractKeyFromUrl(String fileUrl) {
        // URL에서 S3 객체의 키를 추출하는 로직 구현
        // 예: "https://s3.amazonaws.com/bucket-name/" 부분을 제거
        // 이 부분은 실제 S3 URL 구조에 맞게 수정해야 함
        return fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
    }
}
