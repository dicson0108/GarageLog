package com.garagelog.repository;

import com.garagelog.exception.StorageException;
import com.garagelog.model.AppData;
import com.garagelog.model.Vehicle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class JsonGarageRepositoryTest {
    private Path tempDir;
    private JsonGarageRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("garagelog-test-");
        repository = new JsonGarageRepository(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {}
            });
        }
    }

    @Test
    void testSaveAndLoad() {
        AppData data = repository.load();
        assertNotNull(data);
        assertEquals(0, data.getVehicles().size());

        Vehicle v = new Vehicle();
        v.setPlateNumber("TEST-123");
        v.setBrand("Honda");
        v.setModel("Civic");
        v.setYear(2020);
        data.getVehicles().add(v);

        repository.save(data);

        AppData reloaded = repository.load();
        assertEquals(1, reloaded.getVehicles().size());
        assertEquals("TEST-123", reloaded.getVehicles().get(0).getPlateNumber());
    }

    @Test
    void testBackupAndRestore() {
        AppData data = repository.load();
        Vehicle v = new Vehicle();
        v.setPlateNumber("ORIGINAL-1");
        data.getVehicles().add(v);
        repository.save(data);

        Path backupPath = repository.createBackup();
        assertNotNull(backupPath);
        assertTrue(Files.exists(backupPath));

        // Mutate
        data.getVehicles().clear();
        repository.save(data);
        assertEquals(0, repository.load().getVehicles().size());

        // Restore
        AppData restored = repository.restoreBackup(backupPath);
        assertEquals(1, restored.getVehicles().size());
        assertEquals("ORIGINAL-1", restored.getVehicles().get(0).getPlateNumber());
    }

    @Test
    void testCorruptFileThrowsStorageException() throws IOException {
        Files.writeString(repository.getDataFilePath(), "INVALID JSON {");
        assertThrows(StorageException.class, () -> repository.load());
    }
}
