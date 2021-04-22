package io.kontur.eventapi.stormsnoaa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;

@Component
public class StormsNoaaImportService {

    @Value("${stormsNoaa.host}")
    private String URL;

    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    private static final String separator = System.getProperty("file.separator");

    public void downloadFile(String filename, String tmpFilePath) throws Exception {
        File file = new File(tmpFilePath);
        URLConnection conn = new URI(URL + filename).toURL().openConnection();
        transferData(conn, file);
    }

    public String getFilePath(String filename) {
        return tmpDir + separator + filename;
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);
        file.delete();
    }

    private static void transferData(URLConnection conn, File file) throws IOException {
        try (InputStream inputStream = conn.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
            byte[] buffer = new byte[1024];
            int bytesCount;
            while ((bytesCount = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesCount);
            }
        }
    }
}
