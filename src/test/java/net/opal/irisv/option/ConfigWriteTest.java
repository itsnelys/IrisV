package net.opal.irisv.option;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigWriteTest {
    @TempDir Path directory;

    @Test void replacesExistingFileWithoutLeavingTemporaryFiles() throws Exception {
        Path file = directory.resolve("options.json");
        ConfigOptions.writeAtomic(file, "old");
        ConfigOptions.writeAtomic(file, "new");
        assertEquals("new", Files.readString(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
}
