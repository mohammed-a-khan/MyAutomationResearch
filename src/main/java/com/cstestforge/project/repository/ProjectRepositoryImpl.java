package com.cstestforge.project.repository;

import com.cstestforge.project.model.Project;
import com.cstestforge.project.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ProjectRepository
 */
@Repository
public class ProjectRepositoryImpl implements ProjectRepository {
    
    private static final String PROJECTS_PATH = "projects";
    private static final String PROJECT_FILE = "project.json";
    
    private final FileStorageService storageService;
    
    @Autowired
    public ProjectRepositoryImpl(FileStorageService storageService) {
        this.storageService = storageService;
        
        // Ensure projects directory exists
        storageService.createDirectory(PROJECTS_PATH);
    }
    
    @Override
    public Optional<Project> findById(String id) {
        String projectPath = PROJECTS_PATH + "/" + id + "/" + PROJECT_FILE;
        if (!storageService.fileExists(projectPath)) {
            return Optional.empty();
        }
        
        Project project = storageService.readFromJson(projectPath, Project.class);
        return Optional.ofNullable(project);
    }
    
    @Override
    public List<Project> findAll() {
        List<Project> projects = new ArrayList<>();
        
        // List all project directories
        List<String> projectDirs = storageService.listFiles(PROJECTS_PATH, name -> true);
        
        // Load each project
        for (String projectDir : projectDirs) {
            String projectPath = PROJECTS_PATH + "/" + projectDir + "/" + PROJECT_FILE;
            if (storageService.fileExists(projectPath)) {
                Project project = storageService.readFromJson(projectPath, Project.class);
                if (project != null) {
                    projects.add(project);
                }
            }
        }
        
        return projects;
    }
    
    @Override
    public Project create(Project project) {
        // Ensure ID is set
        if (project.getId() == null || project.getId().isEmpty()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        
        // Ensure timestamps are set
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(LocalDateTime.now());
        }
        if (project.getUpdatedAt() == null) {
            project.setUpdatedAt(LocalDateTime.now());
        }
        
        // Create directory for project
        String projectDir = PROJECTS_PATH + "/" + project.getId();
        storageService.createDirectory(projectDir);
        
        // Save project
        String projectPath = projectDir + "/" + PROJECT_FILE;
        storageService.saveToJson(projectPath, project);
        
        return project;
    }
    
    @Override
    public Project update(String id, Project project) {
        // Check if project exists
        Optional<Project> existingProjectOpt = findById(id);
        if (!existingProjectOpt.isPresent()) {
            throw new IllegalArgumentException("Project not found: " + id);
        }
        
        Project existingProject = existingProjectOpt.get();
        
        // Preserve created timestamp and ID
        project.setId(id);
        project.setCreatedAt(existingProject.getCreatedAt());
        
        // Update timestamp
        project.setUpdatedAt(LocalDateTime.now());
        
        // Save project
        String projectPath = PROJECTS_PATH + "/" + id + "/" + PROJECT_FILE;
        storageService.saveToJson(projectPath, project);
        
        return project;
    }
    
    @Override
    public boolean delete(String id) {
        // Check if project exists
        if (!findById(id).isPresent()) {
            return false;
        }
        
        // Delete project directory
        String projectDir = PROJECTS_PATH + "/" + id;
        return storageService.deleteDirectory(projectDir);
    }
    
    @Override
    public List<String> getAllProjectIds() {
        // List all project directories
        List<String> projectDirs = storageService.listFiles(PROJECTS_PATH, name -> true);
        
        // Filter to only include directories that contain a project.json file
        return projectDirs.stream()
                .filter(dir -> storageService.fileExists(PROJECTS_PATH + "/" + dir + "/" + PROJECT_FILE))
                .collect(Collectors.toList());
    }
} 