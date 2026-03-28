package me.cema.cloud_storage.services;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.resourse.ResourceResponse;
import me.cema.cloud_storage.dto.resourse.ResourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final MinioClient minioClient;
    private static final String BUCKET_NAME = "user-files";

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
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "minio error: " + e.errorResponse().message());
            }
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "minio exception");
        }
    }

    public ResourceResponse get(String path, Long id) {
        validatePath(path);

        boolean isDirectory = isDirectory(path);
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String key = keyOf(normalizedPath, id);

        if (isDirectory) {
            Iterable<Result<Item>> results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder()
                                    .bucket(BUCKET_NAME)
                                    .prefix(key)
                                    .maxKeys(1)
                                    .build());
            if (results.iterator().hasNext()) {
                Resource resource = getResource(path, isDirectory);
                return new ResourceResponse(
                        resource.path,
                        resource.name,
                        null,
                        ResourceType.DIRECTORY
                );
            }
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "resource not found");
        } else {
            StatObjectResponse statObjectResponse = executeWithHandling(() ->
                    minioClient.statObject(StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(key)
                            .build())
            );
            Resource file = getResource(path, isDirectory);
            return new ResourceResponse(
                    file.path,
                    file.name,
                    statObjectResponse.size(),
                    ResourceType.FILE
            );
        }
    }

    public void delete(String path, Long id) {
        validatePath(path);
        String key = keyOf(path, id);
        executeWithHandling(() -> {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(key)
                    .build());
            return null;
        });
    }

    public StreamingResponseBody download(String path, Long id, boolean isDirectory) {
        validatePath(path);
        String key = keyOf(path, id);
        if (!isDirectory) {
            return writeFileToStream(key);
        } else {
            return writeDirectoryToZip(key);
        }
    }

    private StreamingResponseBody writeFileToStream(String key) {
        return outputStream -> {
            GetObjectResponse getObjectResponse = executeWithHandling(() ->
                    minioClient.getObject(GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(key)
                            .build()));
            outputStream.write(getObjectResponse.readAllBytes());
        };
    }

    private StreamingResponseBody writeDirectoryToZip(String key) {
        return outputStream ->
                executeWithHandling(
                        () -> {
                            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                                    .bucket(BUCKET_NAME)
                                    .prefix(key)
                                    .recursive(false)
                                    .build());
                            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                            for (Result<Item> item : results) {
                                String name = item.get().objectName();
                                GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                                        .bucket(BUCKET_NAME)
                                        .object(name)
                                        .build());
                                zipOutputStream.putNextEntry(new ZipEntry(name.replaceFirst(key, "")));
                                zipOutputStream.write(response.readAllBytes());
                            }
                            zipOutputStream.close();
                            return null;
                        }
                );
    }

    public ResourceResponse move(String from, String to, Long id) {
        validatePath(from);
        validatePath(to);
        if (from.equals(to) || (from.charAt(from.length() - 1) != to.charAt(to.length() - 1))) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "\"from\" and \"to\" parameters are not valid");
        }
        String fromKey = keyOf(from, id);
        String toKey = keyOf(to, id);
        boolean isDirectory = to.endsWith("/");

        if (isDirectory) {
            moveDirectory(fromKey, toKey);
            Resource directory = getResource(to, isDirectory);
            return new ResourceResponse(
                    directory.path,
                    directory.name,
                    null,
                    ResourceType.DIRECTORY
            );
        } else {
            StatObjectResponse statObjectResponse = moveFile(fromKey, toKey);
            Resource file = getResource(to, isDirectory);
            return new ResourceResponse(
                    file.path,
                    file.name,
                    statObjectResponse.size(),
                    ResourceType.FILE
            );
        }
    }

    private void moveDirectory(String fromKey, String toKey) {
        Iterable<Result<Item>> fromResults = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(BUCKET_NAME)
                .prefix(fromKey)
                .recursive(true).
                build()
        );
        if (!fromResults.iterator().hasNext()) {
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND, "resource by \"from\" do not exist");
        }

        Iterable<Result<Item>> toResults = minioClient.listObjects(ListObjectsArgs.builder().
                bucket(BUCKET_NAME)
                .prefix(toKey)
                .recursive(true)
                .build());
        if (toResults.iterator().hasNext()) {
            throw new HttpClientErrorException(HttpStatus.CONFLICT, "resource by \"to\" already exists");
        }

        for (Result<Item> item : fromResults) {
            String key;
            try {
                key = item.get().objectName();
            } catch (Exception e) {
                throw new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            String finalKey = key;
            executeWithHandling(
                    () -> {
                        String newKey = toKey + key.substring(fromKey.length());
                        minioClient.copyObject(CopyObjectArgs.builder()
                                .bucket(BUCKET_NAME)
                                .object(newKey)
                                .source(CopySource.builder()
                                        .bucket(BUCKET_NAME)
                                        .object(finalKey).build()
                                ).build());
                        return null;
                    }
            );
        }
        executeWithHandling(() -> {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(fromKey)
                    .build());
            return null;
        });
    }

    private StatObjectResponse moveFile(String fromKey, String toKey) {
        executeWithHandling(
                () -> minioClient.statObject(StatObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(fromKey)
                        .build())
        );
        try {
            executeWithHandling(
                    () -> minioClient.statObject(StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(toKey)
                            .build())
            );
        } catch (HttpClientErrorException ignored) {
            executeWithHandling(
                    () -> minioClient.copyObject(CopyObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(toKey)
                            .source(CopySource.builder()
                                    .bucket(BUCKET_NAME)
                                    .object(fromKey).build()
                            ).build())
            );
            executeWithHandling(() -> {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(fromKey)
                        .build());
                return null;
            });

            StatObjectResponse statObjectResponse = executeWithHandling(
                    () -> minioClient.statObject(StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(toKey)
                            .build())
            );
            return statObjectResponse;
        }
        throw new HttpClientErrorException(HttpStatus.CONFLICT, "resource by \"to\" already exists");
    }

    @RequiredArgsConstructor
    static class Resource {
        private final String path;
        private final String name;
    }

    private static Resource getResource(String path, boolean isDirectory) {
        StringBuilder sb = new StringBuilder(path);
        if (isDirectory) {
            sb.deleteCharAt(path.length() - 1);
        }
        int lastIndexOfSlashPlusOne = sb.lastIndexOf("/") + 1;
        String parent = sb.substring(0, lastIndexOfSlashPlusOne) != null ? sb.substring(0, lastIndexOfSlashPlusOne) : "/";
        String resourceName = sb.substring(lastIndexOfSlashPlusOne);
        return new Resource(parent, resourceName);
    }

    private static String keyOf(String path, Long id) {
        return "user-" + id + "-files/" + path;
    }

    private static void validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "path must not be empty");
        }
    }

    private static boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}


