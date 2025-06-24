package org.springframework.aot;

public final class AotDetector {
    private AotDetector() {}

    public static boolean useGeneratedArtifacts() {
        return false;
    }
}
