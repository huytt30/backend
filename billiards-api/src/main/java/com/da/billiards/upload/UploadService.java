package com.da.billiards.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.da.billiards.common.ApiException;
import com.da.billiards.config.AppProperties;

@Service
public class UploadService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final Path uploadDir;

    public UploadService(AppProperties properties) throws IOException {
        this.uploadDir = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    /**
     * Lưu ảnh với tên ngẫu nhiên. Định dạng được xác định từ nội dung file (magic bytes),
     * không dựa vào tên hay Content-Type do client gửi lên.
     */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn file ảnh");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("Ảnh tối đa 5MB");
        }
        try {
            String extension = detectExtension(file);
            String filename = UUID.randomUUID() + "." + extension;
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, uploadDir.resolve(filename));
            }
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Không lưu được file", ex);
        }
    }

    private static String detectExtension(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.readNBytes(header, 0, header.length);
        }
        if (read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "png";
        }
        if (read >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return "gif";
        }
        if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        throw ApiException.badRequest("Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc GIF");
    }
}
