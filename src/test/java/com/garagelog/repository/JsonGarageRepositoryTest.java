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
import java.nio.file.attribute.FileTime;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        v.setPurchaseDate(LocalDate.of(2020, 2, 29));
        v.setPurchasePrice(new BigDecimal("12345.67"));
        data.getVehicles().add(v);

        repository.save(data);

        AppData reloaded = repository.load();
        assertEquals(1, reloaded.getVehicles().size());
        assertEquals("TEST-123", reloaded.getVehicles().get(0).getPlateNumber());
        assertEquals(v.getPurchaseDate(), reloaded.getVehicles().get(0).getPurchaseDate());
        assertEquals(v.getPurchasePrice(), reloaded.getVehicles().get(0).getPurchasePrice());
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
    void testBackupRetentionKeepsNewestTenAndLeavesOtherFiles() throws IOException {
        repository.load();
        Path backups = tempDir.resolve("backups");
        Path unrelated = Files.writeString(backups.resolve("notes.json"), "keep");
        for (int i = 0; i < 12; i++) {
            Path backup = Files.writeString(backups.resolve("garagelog-backup-" + i + ".json"), "{}");
            Files.setLastModifiedTime(backup, FileTime.fromMillis(1000L + i));
        }

        Path newest = repository.createBackup();
        assertEquals(10, repository.listBackups().size());
        assertEquals(newest, repository.listBackups().getFirst());
        assertTrue(Files.exists(unrelated));
        assertFalse(Files.exists(backups.resolve("garagelog-backup-2.json")));
        assertTrue(Files.exists(backups.resolve("garagelog-backup-3.json")));
        assertTrue(Files.exists(backups.resolve("garagelog-backup-11.json")));
    }

    @Test
    void testCorruptFileThrowsStorageException() throws IOException {
        Files.writeString(repository.getDataFilePath(), "INVALID JSON {");
        assertThrows(StorageException.class, () -> repository.load());
    }
}
