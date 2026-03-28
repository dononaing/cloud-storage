package me.cema.cloud_storage.dto.resourse;

import lombok.Data;

@Data
public class ResourceResponse {
    private final String path;
    private final String name;
    private final Long size;
    private final ResourceType type;
}
