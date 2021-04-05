package io.kontur.eventapi.staticdata.reader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticFileReaderTest {

    private final StaticFileReader staticFileReader = new StaticFileReader();
    private final static String FOLDER = "io/kontur/eventapi/staticdata/static/";
    private final static String FILENAME = "[test-provider](2020-07-12T10:37:00).geojson";
    private final static String NOT_EXISTING_FILENAME = "not-existing-file.json";

    @Test
    public void testFindAllFilenames() {
        List<String> filenames = staticFileReader.findAllFilenames(FOLDER);
        assertEquals(1, filenames.size());
        assertEquals(FILENAME, filenames.get(0));
    }

    @Test
    public void testReadFile() throws IOException {
        assertDoesNotThrow(() -> staticFileReader.readFile(FOLDER + FILENAME));
        assertNotNull(staticFileReader.readFile(FOLDER + FILENAME));
        assertThrows(IOException.class, () -> staticFileReader.readFile(FOLDER + NOT_EXISTING_FILENAME));
    }

}