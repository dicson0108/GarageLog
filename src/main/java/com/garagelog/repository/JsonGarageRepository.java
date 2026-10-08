package com.garagelog.repository;

import com.garagelog.exception.StorageException;
import com.garagelog.model.AppData;
import com.garagelog.util.JsonUtil;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonGarageRepository implements GarageRepository {
    private static final String DEFAULT_FILE_NAME = "garagelog.json";
    private static final String BACKUP_DIR_NAME = "backups";
    private static final int MAX_BACKUPS = 10;
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path rootDirectory;
    private final Path dataFilePath;
    private final Path backupDirectory;

    public JsonGarageRepository() {
        this(Paths.get(System.getProperty("user.home"), ".garagelog"));
    }

    public JsonGarageRepository(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.dataFilePath = rootDirectory.resolve(DEFAULT_FILE_NAME);
        this.backupDirectory = rootDirectory.resolve(BACKUP_DIR_NAME);
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(rootDirectory);
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize storage directories: " + rootDirectory, e);
        }
    }

    @Override
    public Path getDataFilePath() {
        return dataFilePath;
    }

    @Override
    public AppData load() {
        if (!Files.exists(dataFilePath)) {
            AppData initial = new AppData();
            save(initial);
            return initial;
        }

        try {
            String json = Files.readString(dataFilePath, StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                return new AppData();
            }
            AppData appData = JsonUtil.getGson().fromJson(json, AppData.class);
            if (appData == null) {
                return new AppData();
            }
            migrateIfNeeded(appData);
            return appData;
        } catch (JsonSyntaxException e) {
            throw new StorageException("Corrupt data file at " + dataFilePath + ". Please restore from backup.", e);
        } catch (IOException e) {
            throw new StorageException("Failed to read data file: " + dataFilePath, e);
        }
    }

    private void migrateIfNeeded(AppData appData) {
        // Current schema is 1. If older schemas exist in future, migrate step-by-step.
        if (appData.getSchemaVersion() < 1) {
            appData.setSchemaVersion(1);
        }
    }

    @Override
    public synchronized void save(AppData data) {
        if (data == null) {
            throw new IllegalArgumentException("Cannot save null AppData.");
        }

        Path tempFile = rootDirectory.resolve(DEFAULT_FILE_NAME + ".tmp");
        try {
            String json = JsonUtil.getGson().toJson(data);
            Files.writeString(tempFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

            try {
                Files.move(tempFile, dataFilePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, dataFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
            throw new StorageException("Failed to write data atomically to " + dataFilePath, e);
        }
    }

    @Override
    public synchronized Path createBackup() {
        if (!Files.exists(dataFilePath)) {
            return null;
        }

        String timestamp = LocalDateTime.now().format(BACKUP_FORMATTER);
        Path targetBackup = backupDirectory.resolve("garagelog-backup-" + timestamp + ".json");
        try {
            Files.copy(dataFilePath, targetBackup, StandardCopyOption.REPLACE_EXISTING);
            pruneBackups();
            return targetBackup;
        } catch (IOException e) {
            throw new StorageException("Failed to create backup at " + targetBackup, e);
        }
    }

    private void pruneBackups() {
        try (Stream<Path> stream = Files.list(backupDirectory)) {
            List<Path> backups = stream
                    .filter(p -> p.getFileName().toString().startsWith("garagelog-backup-") && p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::getCreationTimeSafely).reversed())
                    .toList();

            if (backups.size() > MAX_BACKUPS) {
                for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                    try {
                        Files.deleteIfExists(backups.get(i));
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            // Non-fatal warning
        }
    }

    private long getCreationTimeSafely(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public List<Path> listBackups() {
        try (Stream<Path> stream = Files.list(backupDirectory)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("garagelog-backup-") && p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::getCreationTimeSafely).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to list backups in " + backupDirectory, e);
        }
    }

    @Override
    public synchronized AppData restoreBackup(Path backupPath) {
        if (!Files.exists(backupPath)) {
            throw new StorageException("Backup file does not exist: " + backupPath);
        }

        try {
            String json = Files.readString(backupPath, StandardCharsets.UTF_8);
            AppData restored = JsonUtil.getGson().fromJson(json, AppData.class);
            if (restored == null) {
                throw new StorageException("Backup contains invalid data: " + backupPath);
            }
            save(restored);
            return restored;
        } catch (JsonSyntaxException e) {
            throw new StorageException("Backup file is corrupt: " + backupPath, e);
        } catch (IOException e) {
            throw new StorageException("Failed to read backup file: " + backupPath, e);
        }
    }
}
