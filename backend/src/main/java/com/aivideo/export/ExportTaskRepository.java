package com.aivideo.export;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExportTaskRepository extends JpaRepository<ExportTask, UUID> {
    List<ExportTask> findByProjectId(UUID projectId);
}
