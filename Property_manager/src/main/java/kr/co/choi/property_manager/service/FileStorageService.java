package kr.co.choi.property_manager.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.File;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootDir;
    private final String uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadDir = uploadDir;
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Files.createDirectories(rootDir);

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";

        int idx = original.lastIndexOf('.');
        if (idx > -1) ext = original.substring(idx);

        String storedName = UUID.randomUUID() + ext;
        Path target = rootDir.resolve(storedName);

        Files.copy(file.getInputStream(),target, StandardCopyOption.REPLACE_EXISTING);

        // 브라우저에서 접근할 URL (아래 WebConfig에서 매핑
        String url = "/uploads/" + storedName;

        return new StoredFile(original, storedName, url);

    }

    public record StoredFile(String originalName, String storedName, String url) {}

    public void deleteByUrl(String url) {
        if (url == null || url.isBlank()) return;

        String filename = url.replaceFirst("^/(upload|uploads)/", "");
        Path path = rootDir.resolve(filename).normalize();

        try {
            boolean deleted = Files.deleteIfExists(path);
            System.out.println("[FILE-DELETE] " + path + " deleted=" + deleted);
        } catch (IOException e) {
            System.out.println("[FILE-DELETE] FAIL " + path + " err=" + e.getMessage());
            e.printStackTrace();
        }
    }

}
