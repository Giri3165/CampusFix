package com.campusfix.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Stores complaint images on disk after validating extension, size and real image content. */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path getRoot() { return root; }

    /** @return the generated file name (not the full path) */
    public String store(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller.");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(original);
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only JPG, PNG or GIF images are allowed.");
        }
        try (InputStream in = file.getInputStream()) {
            if (ImageIO.read(in) == null) {
                throw new IllegalArgumentException("The uploaded file is not a valid image.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String storedName = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the image", e);
        }
        return storedName;
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) return;
        try {
            Path target = root.resolve(storedName).normalize();
            if (target.startsWith(root)) Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best effort clean-up
        }
    }
}
