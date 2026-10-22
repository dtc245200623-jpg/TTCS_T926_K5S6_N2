package com.example.auth.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * Tự động tìm và nạp các biến môi trường từ file .env vào System Properties
 * Giúp các thành viên trong nhóm tự do cấu hình thông tin Database riêng
 * mà không ghi đè cấu hình của nhau hoặc làm lộ thông tin mật lên GitHub.
 */
public class DotenvLoader {

    public static void load() {
        File[] candidateFiles = new File[] {
            new File(".env"),
            new File("../.env"),
            new File("BE/.env")
        };

        for (File file : candidateFiles) {
            if (file.exists() && file.isFile()) {
                System.out.println(">>> [DotenvLoader] Đang nạp cấu hình môi trường từ: " + file.getAbsolutePath());
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();

                            // Bỏ comment inline nếu có (ví dụ: DB_SERVER=... # Địa chỉ IP)
                            int hashIdx = value.indexOf(" #");
                            if (hashIdx >= 0) {
                                value = value.substring(0, hashIdx).trim();
                            }

                            // Bỏ dấu ngoặc kép / nháy đơn bao bọc giá trị
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                if (value.length() >= 2) {
                                    value = value.substring(1, value.length() - 1);
                                }
                            }

                            // Chỉ nạp nếu chưa được set trong System Properties hoặc System Env
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                                count++;
                            }
                        }
                    }
                    System.out.println(">>> [DotenvLoader] Nạp thành công " + count + " biến môi trường từ .env");
                } catch (Exception e) {
                    System.err.println(">>> [DotenvLoader] Cảnh báo lỗi khi đọc file .env: " + e.getMessage());
                }
                return;
            }
        }
        System.out.println(">>> [DotenvLoader] Không tìm thấy file .env, hệ thống sẽ dùng giá trị mặc định trong application.yml hoặc biến môi trường hệ thống.");
    }

    public static void main(String[] args) {
        System.out.println("Kiểm tra nạp biến môi trường từ .env...");
        load();
        System.out.println("DB_SERVER: " + System.getProperty("DB_SERVER"));
        System.out.println("DB_NAME: " + System.getProperty("DB_NAME"));
        System.out.println("DB_USER: " + System.getProperty("DB_USER"));
        System.out.println("DB_PASSWORD: " + (System.getProperty("DB_PASSWORD") != null ? "******" : "null"));
    }
}
