package com.garagelog.repository;

import com.garagelog.model.AppData;

import java.nio.file.Path;
import java.util.List;

public interface GarageRepository {
    AppData load();

    void save(AppData data);

    Path createBackup();

    List<Path> listBackups();

    AppData restoreBackup(Path backupPath);

    Path getDataFilePath();
}
