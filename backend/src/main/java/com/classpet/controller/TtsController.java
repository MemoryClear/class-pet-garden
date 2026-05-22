package com.classpet.controller;

import com.classpet.service.TtsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    @Autowired
    private TtsService ttsService;

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> speak(
            @RequestParam String text,
            @RequestParam(defaultValue = "zh-CN-XiaoxiaoNeural") String voice) {
        try {
            Path mp3 = ttsService.synthesize(text, voice);
            if (mp3 == null) {
                return ResponseEntity.badRequest().build();
            }
            byte[] data = Files.readAllBytes(mp3);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}