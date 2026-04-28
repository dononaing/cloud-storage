package me.cema.cloud_storage.controller;

import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.resourse.ResourceResponse;
import me.cema.cloud_storage.model.user.User;
import me.cema.cloud_storage.service.ResourceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResourceResponse get(@RequestParam String path,
                                @AuthenticationPrincipal User user) {
        return resourceService.get(path, user.getId());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path,
                       @AuthenticationPrincipal User user) {
        resourceService.delete(path, user.getId());
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String path,
                                                          @AuthenticationPrincipal User user) {
        Path convertedPath = Path.of(path);
        String filename = path.endsWith("/") ? convertedPath.getFileName() + ".zip" : convertedPath.getFileName().toString();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(resourceService.download(path, user.getId()));
    }

    @GetMapping("/move")
    public ResourceResponse move(@RequestParam String from,
                                 @RequestParam String to,
                                 @AuthenticationPrincipal User user) {
        return resourceService.move(from, to, user.getId());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Iterable<ResourceResponse> upload(@RequestParam String path,
                                             @RequestBody MultipartFile[] files,
                                             @AuthenticationPrincipal User user) {
        return resourceService.upload(path, files, user.getId());
    }

    @GetMapping("/directory")
    public Iterable<ResourceResponse> getDirectoryContent(@RequestParam String path,
                                                          @AuthenticationPrincipal User user) {
        return resourceService.getDirectoryContent(path, user.getId());
    }

    @GetMapping("/search")
    public Iterable<ResourceResponse> search(@RequestParam String query,
                                             @AuthenticationPrincipal User user) {
        return resourceService.search(query, user.getId());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/directory")
    public ResourceResponse uploadEmptyDirectory(@RequestParam String path,
                                                 @AuthenticationPrincipal User user) {
        return resourceService.uploadEmptyDirectory(path, user.getId());
    }
}

