package me.cema.cloud_storage.service;

import com.google.common.collect.Lists;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.resourse.MyItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;
    private final static String BUCKET_NAME = "user-files";

    public List<MyItem> listObjects(String key, int maxKeys, boolean isRecursive) {
        Iterable<Result<io.minio.messages.Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .prefix(key)
                        .maxKeys(maxKeys)
                        .recursive(isRecursive)
                        .build());
        if (!results.iterator().hasNext()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "resource not found");
        }
        return Lists.newArrayList(results)
                .stream()
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while storing objects to delete");
                    }
                })
                .map(item ->
                        new MyItem(
                                item.size(),
                                item.objectName()
                        ))
                .toList();
    }

    public List<MyItem> listObjectsRecursive(String key) {
        return listObjects(key, 1000, true);
    }

    public List<MyItem> listObjectsFindFirst(String key) {
        return listObjects(key, 1, false);
    }

    public MyItem statObject(String key) {
        StatObjectResponse statObjectResponse = executeWithHandling(() ->
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(key)
                        .build()));
        return new MyItem(
                statObjectResponse.size(),
                statObjectResponse.object()
        );
    }

    public void removeObject(String key) {
        executeWithHandling(() -> {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(key)
                    .build());
            return null;
        });
    }

    public void removeObjects(List<DeleteObject> keysToDelete) {
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(BUCKET_NAME)
                .objects(keysToDelete)
                .build());
        if (results.iterator().hasNext()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while deleting an objects");
        }
    }

    public void putObjectWithStreamAndContentType(String key, InputStream stream, long size, long partSize, String contentType) {
        executeWithHandling(() ->
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(key)
                        .stream(stream, size, partSize)
                        .contentType(contentType)
                        .build()));
    }

    public void putStubObject(String key) {
        executeWithHandling(() ->
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(key)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build()));
    }

    public void copyObject(String fromKey, String toKey) {
        executeWithHandling(() ->
                minioClient.copyObject(CopyObjectArgs.builder().
                        bucket(BUCKET_NAME)
                        .object(toKey)
                        .source(CopySource.builder()
                                .bucket(BUCKET_NAME)
                                .object(fromKey)
                                .build())
                        .build()));
    }

    public byte[] getObject(String key) {
        GetObjectResponse getObjectResponse = executeWithHandling(() ->
                minioClient.getObject(GetObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(key)
                        .build()));
        try {
            return getObjectResponse.readAllBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while reading minio response");
        }
    }

    private interface MinioOperation<T> {
        T execute() throws Exception;
    }

    private <T> T executeWithHandling(MinioOperation<T> operation) {
        try {
            return operation.execute();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "resource not found");
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "minio error: " + e.errorResponse().message(), e);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "minio exception", e);
        }
    }
}
