package io.kontur.eventapi.staticdata.reader;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StaticFileReader {

    public List<String> findAllFilenames(String folder) {
        URL url = getClass().getClassLoader().getResource(folder);
        if (url != null) {
            File directory = new File(url.getPath());
            File[] files = directory.listFiles();
            if (files != null) {
                return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public String readFile(String filePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IOException("File not found: " + filePath);
        }
        return new String(inputStream.readAllBytes());
    }
}
