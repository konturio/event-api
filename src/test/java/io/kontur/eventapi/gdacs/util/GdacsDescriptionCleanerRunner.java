package io.kontur.eventapi.gdacs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GdacsDescriptionCleanerRunner {
    public static void main(String[] args) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            System.out.println("before\tafter");
            while ((line = br.readLine()) != null) {
                String cleaned = GdacsDescriptionCleaner.clean(line);
                System.out.println(line + "\t" + cleaned);
            }
        }
    }
}
