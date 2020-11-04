package io.kontur.eventapi;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class TestUtil {

    public static String readMessageFromFile(Object obj, String file) throws IOException {
        return IOUtils.toString(obj.getClass().getResourceAsStream(file), "UTF-8");
    }
}
