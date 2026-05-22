package com.classpet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TtsService {

    @Value("${tts.cache-dir:/app/data/tts-cache}")
    private String cacheDir;

    @Value("${tts.voice:zh-CN-XiaoxiaoNeural}")
    private String defaultVoice;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public Path synthesize(String text, String voice) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) return null;
        if (voice == null || voice.isBlank()) voice = defaultVoice;

        String key = md5(text + "|" + voice) + ".mp3";
        Path cachePath = Paths.get(cacheDir, key);
        if (Files.exists(cachePath)) return cachePath;

        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            if (Files.exists(cachePath)) return cachePath;
            Files.createDirectories(cachePath);
            Path tempPath = Files.createTempFile(cachePath.getParent(), "tts_", ".mp3");
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "edge-tts", "--voice", voice, "--text", text, "--write-media", tempPath.toString()
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String output = new String(proc.getInputStream().readAllBytes());
                int code = proc.waitFor();
                if (code != 0) {
                    Files.deleteIfExists(tempPath);
                    throw new IOException("edge-tts failed (code " + code + "): " + output);
                }
                Files.move(tempPath, cachePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | InterruptedException e) {
                Files.deleteIfExists(tempPath);
                throw e;
            } finally {
                locks.remove(key);
            }
        }
        return cachePath;
    }

    private String md5(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
