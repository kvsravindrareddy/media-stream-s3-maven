package com.veera;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ContentManagementService {

    @Autowired
    private AmazonS3 amazonS3Client;

    /*public void uploadToS3(String bucketName, String key, Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();
            long contentLength = resource.contentLength();
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType("audio/wav")
                    .build();
            amazonS3Client.putObject(bucketName, key, resource.getInputStream(), metadata);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentLength(contentLength)
                    .build();
            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            // Handle the response if needed
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public String uploadFile(String bucketName, String key, Resource resource) {

        String uploadedFileName = "";
        try {
            InputStream inputStream = resource.getInputStream();
            // Create object metadata
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType("audio/wav");
            amazonS3Client.putObject(bucketName, key, inputStream, objectMetadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return uploadedFileName;
    }
}