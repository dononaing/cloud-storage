package me.cema.cloud_storage.dto.resourse;

import lombok.Data;

@Data
public class Resource {
    private final String path;
    private final String name;

    public static Resource getResource(String path, boolean isDirectory) {
        StringBuilder sb = new StringBuilder(path);
        sb = isDirectory ? sb.deleteCharAt(path.length() - 1) : sb;
        int lastIndexOfSlashPlusOne = sb.lastIndexOf("/") + 1;
        String parent = lastIndexOfSlashPlusOne == 0 ? "/" : sb.substring(0, lastIndexOfSlashPlusOne);
        String resourceName = sb.substring(lastIndexOfSlashPlusOne);
        return new Resource(parent, resourceName);
    }
}

