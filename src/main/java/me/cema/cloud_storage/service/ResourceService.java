package me.cema.cloud_storage.service;

import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.resourse.MyItem;
import me.cema.cloud_storage.dto.resourse.Resource;
import me.cema.cloud_storage.dto.resourse.ResourceResponse;
import me.cema.cloud_storage.dto.resourse.ResourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static me.cema.cloud_storage.dto.resourse.Resource.getResource;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final MinioService minioService;

    public ResourceResponse get(String path, Long id) {
        path = validatePath(path);
        String key = keyOf(path, id);

        if (isDirectory(path)) {
            minioService.listObjectsFindFirst(key);
            Resource directory = getResource(path, true);
            return new ResourceResponse(
                    directory.getPath(),
                    directory.getName(),
                    null,
                    ResourceType.DIRECTORY
            );
        } else {
            MyItem item = minioService.statObject(key);
            Resource file = getResource(path, false);
            return new ResourceResponse(
                    file.getPath(),
                    file.getName(),
                    item.getSize(),
                    ResourceType.FILE
            );
        }
    }

    public List<ResourceResponse> upload(String directoryName, List<MultipartFile> files, Long id) {
        directoryName = validatePath(directoryName);
        if (!directoryName.endsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path should end with \"/\"");
        }
        if (!directoryExist(keyOf(directoryName, id))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "directory does not exist: " + directoryName);
        }
        List<ResourceResponse> result = new ArrayList<>();
        List<DeleteObject> uploadedObjects = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = directoryName + file.getOriginalFilename();
            String key = keyOf(path, id);
            long size = file.getSize();
            if (fileExist(key)) {
                minioService.removeObjects(uploadedObjects);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "file already exist: " + file.getOriginalFilename());
            }
            try {
                InputStream inputStream = file.getInputStream();
                minioService.putObjectWithStreamAndContentType(key, inputStream, size, -1, file.getContentType());
            } catch (Exception e) {
                minioService.removeObjects(uploadedObjects);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while procession the file: " + file.getOriginalFilename());
            }
            uploadedObjects.add(new DeleteObject(key));
            Resource resource = getResource(path, false);
            result.add(new ResourceResponse(
                    resource.getPath(),
                    resource.getName(),
                    size,
                    ResourceType.FILE));
        }
        return result;
    }

    public void delete(String path, Long id) {
        path = path.startsWith("/") ? path : "/" + validatePath(path);
        String key = keyOf(path, id);
        String parent;
        if (isDirectory(key)) {
            path = path.substring(0, path.length()-1);
            parent = path.substring(0, path.lastIndexOf("/") + 1);
            List<MyItem> itemsToRemove = minioService.listObjectsRecursive(key);
            List<DeleteObject> keysToRemove = itemsToRemove
                    .stream()
                    .map(item -> new DeleteObject(item.getName()))
                    .toList();
            minioService.removeObjects(keysToRemove);
        } else {
            parent = path.substring(0, path.lastIndexOf("/") + 1);
            minioService.removeObject(key);
        }
        try {
            minioService.putStubObject(keyOf(parent, id));
        } catch (ResponseStatusException ignored) {

        }
    }

    public StreamingResponseBody download(String path, Long id) {
        path = validatePath(path);
        String key = keyOf(path, id);
        if (isDirectory(path)) {
            return writeDirectoryToZip(key);
        } else {
            return writeFileToStream(key);
        }
    }

    private StreamingResponseBody writeFileToStream(String key) {
        return outputStream -> outputStream.write(minioService.getObject(key));
    }

    private StreamingResponseBody writeDirectoryToZip(String key) {
        return outputStream -> {
            List<MyItem> results = minioService.listObjectsRecursive(key);
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (MyItem item : results) {
                    String name = item.getName();
                    zipOutputStream.putNextEntry(new ZipEntry(name.substring(key.length())));
                    zipOutputStream.write(minioService.getObject(name));
                }
            }
        };
    }

    public ResourceResponse move(String from, String to, Long id) {
        from = validatePath(from);
        to = validatePath(to);
        if (from.equals(to) || (from.charAt(from.length() - 1) != to.charAt(to.length() - 1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"from\" and \"to\" parameters are not valid");
        }

        String fromKey = keyOf(from, id);
        String toKey = keyOf(to, id);
        boolean isDirectory = isDirectory(to);
        if (isDirectory) {
            moveDirectory(fromKey, toKey);
            Resource directory = getResource(to, true);
            return new ResourceResponse(
                    directory.getPath(),
                    directory.getName(),
                    null,
                    ResourceType.DIRECTORY
            );
        } else {
            MyItem item = moveFile(fromKey, toKey);
            Resource file = getResource(to, false);
            return new ResourceResponse(
                    file.getPath(),
                    file.getName(),
                    item.getSize(),
                    ResourceType.FILE
            );
        }
    }

    private MyItem moveFile(String fromKey, String toKey) {
        MyItem item = minioService.statObject(fromKey);
        if (fileExist(toKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "file by \"to\" already exist");
        }

        minioService.copyObject(item.getName(), toKey);
        try {
            minioService.removeObject(fromKey);
        } catch (Exception e) {
            minioService.removeObject(toKey);
        }
        return minioService.statObject(toKey);
    }

    private void moveDirectory(String fromKey, String toKey) {
        List<MyItem> fromItems = minioService.listObjectsRecursive(fromKey);
        if (directoryExist(toKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "directory by \"to\" already exist");
        }
        List<DeleteObject> copiedKeys = new ArrayList<>();
        try {
            for (MyItem item : fromItems) {
                String key = item.getName();
                String newKey = toKey + key.substring(fromKey.length());
                minioService.copyObject(key, newKey);
                copiedKeys.add(new DeleteObject(newKey));
            }
        } catch (Exception e) {
            minioService.removeObjects(copiedKeys);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        List<DeleteObject> objectsToDelete = fromItems.stream().map(item -> new DeleteObject(item.getName())).toList();
        minioService.removeObjects(objectsToDelete);
    }

    public List<ResourceResponse> getDirectoryContent(String path, Long id) {
        path = validatePath(path);
        if (!isDirectory(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path should end with /");
        }
        String directoryKey = keyOf(path, id);
        List<MyItem> myItems = minioService.listObjects(keyOf(path, id), 1000, false);
        List<ResourceResponse> result = new ArrayList<>();
        for (MyItem item : myItems) {
            String key = item.getName();
            if (directoryKey.equals(key)) {
                continue;
            }
            boolean isDirectory = isDirectory(key);
            Resource resource = getResource(key.substring(key.indexOf("/")), isDirectory);
            result.add(new ResourceResponse(
                    resource.getPath(),
                    resource.getName(),
                    isDirectory ? null : item.getSize(),
                    isDirectory ? ResourceType.DIRECTORY : ResourceType.FILE
            ));
        }
        return result;
    }

    public List<ResourceResponse> search(String query, Long id) {
        query = validatePath(query);
        List<MyItem> myItems = minioService.listObjectsRecursive(keyOf("", id));
        List<ResourceResponse> response = new ArrayList<>();
        for (MyItem item : myItems) {
            String absoluteKey = item.getName();
            if (isDirectory(absoluteKey)) {
                continue;
            }
            String key = absoluteKey.substring(absoluteKey.indexOf("/"));
            if (key.contains(query)) {
                Resource resource = getResource(key, false);
                response.add(new ResourceResponse(
                        resource.getPath(),
                        resource.getName(),
                        item.getSize(),
                        ResourceType.FILE
                ));
            }
        }
        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resource not found");
        }
        return response;
    }

    public ResourceResponse uploadEmptyDirectory(String path, Long id) {
        path = validatePath(path);
        if (!isDirectory(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path must end with /");
        }
        Resource resource = getResource(path, true);
        String parent = resource.getPath();
        if (!directoryExist(keyOf(parent, id)) && !Objects.equals(parent, "/")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "parent directory does not exist");
        }
        String key = keyOf(path, id);
        if (directoryExist(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "directory already exist");
        }
        minioService.putStubObject(key);
        return new ResourceResponse(
                resource.getPath(),
                resource.getName(),
                null,
                ResourceType.DIRECTORY
        );
    }

    private boolean fileExist(String key) {
        try {
            minioService.statObject(key);
            return true;
        } catch (ResponseStatusException ignored) {
            return false;
        }
    }

    private boolean directoryExist(String key) {
        try {
            minioService.listObjectsFindFirst(key);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String keyOf(String path, Long id) {
        path = path.startsWith("/") ? path.substring(1) : path;
        return "user-" + id + "-files/" + path;
    }

    private static String validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }
        return path.replaceAll("^/+", "/");
    }

    private static boolean isDirectory(String path) {
        return path.endsWith("/");
    }
}


