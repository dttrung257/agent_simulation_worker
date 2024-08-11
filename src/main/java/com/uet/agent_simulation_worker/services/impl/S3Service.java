package com.uet.agent_simulation_worker.services.impl;

import com.uet.agent_simulation_worker.services.IS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/*
 * This class is used to interact with AWS S3.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service implements IS3Service {
    private final S3Client s3Client;
    private final S3TransferManager transferManager;
    private final ExecutorService virtualThreadExecutor;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public void uploadFile(String objectKey, String localPath) {
        virtualThreadExecutor.submit(() -> {
            try {
                final var putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

                s3Client.putObject(putObjectRequest, Path.of(localPath));
            } catch (Exception e) {
                log.error("Error uploading file to S3", e);
            }
        });
    }

    @Override
    public void uploadFile(String objectKey, String localPath, ObjectCannedACL acl) {
        virtualThreadExecutor.submit(() -> {
            try {
                final var putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .acl(acl)
                    .build();

                s3Client.putObject(putObjectRequest, Path.of(localPath));
            } catch (Exception e) {
                log.error("Error uploading file to S3", e);
            }
        });
    }

    @Override
    public void uploadDirectory(String localPath, String s3Directory) {
        try {
            final var directoryUpload = transferManager.uploadDirectory(UploadDirectoryRequest.builder()
                .source(Path.of(localPath))
                .bucket(bucketName)
                .s3Prefix(s3Directory)
                .build());

            final var completedDirectoryUpload = directoryUpload.completionFuture().join();

            completedDirectoryUpload.failedTransfers()
                    .forEach(fail -> log.warn("Object [{}] failed to transfer", fail.toString()));
        } catch (Exception e) {
            log.error("Error uploading directory to S3", e);
        }
    }

    /**
     * This method is used to delete objects in specific S3 directory.
     *
     * @param s3Directory String
     */
    public void clear(String s3Directory) {
        try {
            s3Directory = s3Directory.endsWith("/") ? s3Directory : s3Directory + "/";

            final var request = ListObjectsV2Request.builder().bucket(bucketName).prefix(s3Directory).build();
            final var list = s3Client.listObjectsV2Paginator(request);

            for (ListObjectsV2Response response : list) {
                final var objects = response.contents();
                if (objects.isEmpty()) {
                    return;
                }

                final var objectIdentifiers = objects.stream()
                    .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                    .toList();

                final var del = Delete.builder().objects(objectIdentifiers).build();

                final var deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(del)
                    .build();

                s3Client.deleteObjects(deleteObjectsRequest);
            }
        } catch (Exception e) {
            log.error("Error uploading directory to S3", e);
        }
    }

    @Override
    public void clearDirectory(String s3Directory) {
        virtualThreadExecutor.submit(() -> this.clear(s3Directory));
    }
}
