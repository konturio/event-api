package org.springframework.aot;

/**
 * Test-only stub of Spring's {@code AotDetector}.
 * It forces the test runtime to ignore generated AOT artifacts.
 */
public final class AotDetector {
    private AotDetector() {}

    /**
     * Always return {@code false} so tests use reflection-based configuration.
     *
     * @return whether generated AOT artifacts should be used
     */
    public static boolean useGeneratedArtifacts() {
        return false;
    }
}
