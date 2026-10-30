package com.campusfix.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir Path tempDir;

    @Test
    void rejectsDisallowedExtension() throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("image", "script.exe", "application/octet-stream", "abc".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storage.store(file));
    }

    @Test
    void rejectsFakeImageContent() throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("image", "fake.png", "image/png", "not really an image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storage.store(file));
    }

    @Test
    void storesRealPngUnderRandomName() throws Exception {
        FileStorageService storage = new FileStorageService(tempDir.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        MockMultipartFile file = new MockMultipartFile("image", "photo.PNG", "image/png", out.toByteArray());

        String stored = storage.store(file);

        assertTrue(stored.endsWith(".png"));
        assertNotEquals("photo.PNG", stored);
        assertTrue(Files.exists(tempDir.resolve(stored)));
    }
}
