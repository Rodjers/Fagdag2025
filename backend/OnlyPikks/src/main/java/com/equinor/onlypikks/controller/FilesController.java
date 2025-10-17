package com.equinor.onlypikks.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@CrossOrigin
public class FilesController {

    @GetMapping("/files/{fileId}")
    public ResponseEntity<Void> redirectToFile(
            @PathVariable String fileId,
            @RequestParam(name = "disposition", defaultValue = "inline") String disposition
    ) {
        String safeDisposition = StringUtils.hasText(disposition) ? disposition : "inline";
        String presignedUrl = "https://cdn.example.com/object/%s?disposition=%s&token=%s"
                .formatted(fileId, safeDisposition, UUID.randomUUID());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(presignedUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
