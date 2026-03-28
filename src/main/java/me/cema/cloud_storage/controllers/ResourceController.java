package me.cema.cloud_storage.controllers;

import lombok.RequiredArgsConstructor;
import me.cema.cloud_storage.dto.resourse.ResourceResponse;
import me.cema.cloud_storage.models.user.User;
import me.cema.cloud_storage.services.ResourceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResourceResponse get(@RequestParam String path /*@AuthenticationPrincipal User user*/) {
        return resourceService.get(path, 58L);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path, @AuthenticationPrincipal User user) {
        resourceService.delete(path, user.getId());
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String path
            /*@AuthenticationPrincipal User user*/) {
        boolean isDirectory = path.endsWith("/");
        Path convertedPath = Path.of(path);
        String filename = path.endsWith("/") ? convertedPath.getFileName() + ".zip" : convertedPath.getFileName().toString();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(resourceService.download(path, 58L, isDirectory));
    }

    @GetMapping("/move")
    public ResourceResponse move(@RequestParam String from, @RequestParam String to /*@AuthenticationPrincipal User user*/) {
        //TODO
        return null;
    }

}

