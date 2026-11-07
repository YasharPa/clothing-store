package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Makes the tests write their log into a temporary folder instead of the real "data" folder.
 * LogService reads the folder from the data.dir property the first time it is used,
 * so call TestEnv.init() before it (in a @BeforeAll method).
 */
final class TestEnv {
    private static Path dir;

    private TestEnv() {
    }

    static synchronized void init() {
        if (dir == null) {
            try {
                dir = Files.createTempDirectory("store-tests");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            System.setProperty("data.dir", dir.toString());
        }
    }
}
