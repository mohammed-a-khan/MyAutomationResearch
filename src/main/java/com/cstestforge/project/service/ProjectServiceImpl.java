package com.cstestforge.project.service;

import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.Project;
import com.cstestforge.project.model.ProjectFilter;
import com.cstestforge.project.model.ProjectStatus;
import com.cstestforge.project.model.ProjectType;
import com.cstestforge.project.storage.FileLock;
import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.project.exception.ResourceNotFoundException;
import com.cstestforge.project.exception.ConcurrencyException;
import com.cstestforge.project.exception.StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Implementation of the ProjectService using file-based storage.
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final FileStorageService fileStorageService;
    private static final String PROJECTS_INDEX_PATH = "projects/_index.json";
    private static final String TAGS_INDEX_PATH = "tags/_index.json";

    @Autowired
    public ProjectServiceImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        // Initialize indexes if they don't exist
        initIndexes();
    }

    /**
     * Initialize the index files if they don't exist
     */
    private void initIndexes() {
        // Create projects index if it doesn't exist
        if (!fileStorageService.fileExists(PROJECTS_INDEX_PATH)) {
            fileStorageService.saveToJson(PROJECTS_INDEX_PATH, new HashMap<String, String>());
        }

        // Create tags index if it doesn't exist
        if (!fileStorageService.fileExists(TAGS_INDEX_PATH)) {
            fileStorageService.saveToJson(TAGS_INDEX_PATH, new HashSet<String>());
        }
    }

    @Override
    public PagedResponse<Project> findAll(ProjectFilter filter) {
        // Get all project IDs from the index
        Map<String, String> projectIndex = fileStorageService.readMapFromJson(
                PROJECTS_INDEX_PATH, String.class, String.class);

        // Load all projects
        List<Project> allProjects = new ArrayList<>();
        for (String projectId : projectIndex.keySet()) {
            String projectPath = String.format("projects/%s/project.json", projectId);
            Project project = fileStorageService.readFromJson(projectPath, Project.class);
            if (project != null) {
                allProjects.add(project);
            }
        }

        // Apply filtering
        List<Project> filteredProjects = allProjects.stream()
                .filter(p -> applyFilter(p, filter))
                .collect(Collectors.toList());

        // Apply sorting
        sortProjects(filteredProjects, filter.getSortBy(), filter.getSortDirection());

        // Apply pagination
        int total = filteredProjects.size();
        int offset = filter.getOffset();
        int limit = filter.getSize();

        List<Project> pagedProjects = filteredProjects.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return new PagedResponse<>(pagedProjects, filter.getPage(), filter.getSize(), total);
    }

    @Override
    public Optional<Project> findById(String id) {
        String projectPath = String.format("projects/%s/project.json", id);
        if (!fileStorageService.fileExists(projectPath)) {
            return Optional.empty();
        }

        Project project = fileStorageService.readFromJson(projectPath, Project.class);
        return Optional.ofNullable(project);
    }

    @Override
    public Project create(Project project) {
        // Ensure ID is set
        if (project.getId() == null || project.getId().isEmpty()) {
            project.setId(UUID.randomUUID().toString());
        }

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        // Set default status if not set
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.DRAFT);
        }

        // Set default type if not set
        if (project.getType() == null) {
            project.setType(ProjectType.WEB);
        }

        // Save the project
        String projectPath = String.format("projects/%s/project.json", project.getId());
        String projectDir = String.format("projects/%s", project.getId());
        String envDir = String.format("projects/%s/environments", project.getId());

        // Create directories
        fileStorageService.createDirectoryIfNotExists(projectDir);
        fileStorageService.createDirectoryIfNotExists(envDir);

        // Update the project index - Using atomic update method
        updateProjectIndexAtomically(project.getId(), project.getName());

        // Update tags index
        updateTagsIndex(project.getTags());

        // Save the project
        fileStorageService.saveToJson(projectPath, project);

        // Create environments index
        String envsIndexPath = String.format("projects/%s/environments/_index.json", project.getId());
        fileStorageService.saveToJson(envsIndexPath, new HashMap<String, String>());

        return project;
    }

    @Override
    public Project update(String id, Project project) {
        String projectPath = String.format("projects/%s/project.json", id);

        if (!fileStorageService.fileExists(projectPath)) {
            throw new ResourceNotFoundException("Project", "id", id);
        }

        // Load the existing project
        Project existingProject = fileStorageService.readFromJson(projectPath, Project.class);

        // Update fields
        existingProject.setName(project.getName());
        existingProject.setDescription(project.getDescription());
        existingProject.setType(project.getType());
        existingProject.setStatus(project.getStatus());
        existingProject.setBaseUrl(project.getBaseUrl());
        existingProject.setRepositoryUrl(project.getRepositoryUrl());
        existingProject.setUpdatedBy(project.getUpdatedBy());
        existingProject.setUpdatedAt(LocalDateTime.now());

        // Update tags - save old tags to remove from index if needed
        Set<String> oldTags = new HashSet<>(existingProject.getTags());
        existingProject.setTags(project.getTags());

        // Update project tags in the index
        updateTagsIndex(existingProject.getTags());
        removeUnusedTags(oldTags);

        // Update project name in the index - Using atomic update method
        updateProjectIndexAtomically(id, existingProject.getName());

        // Save the updated project
        fileStorageService.saveToJson(projectPath, existingProject);

        return existingProject;
    }

    @Override
    public boolean delete(String id) {
        String projectPath = String.format("projects/%s/project.json", id);
        String projectDir = String.format("projects/%s", id);

        if (!fileStorageService.fileExists(projectPath)) {
            throw new ResourceNotFoundException("Project", "id", id);
        }

        // Load the project to get tags
        Project project = fileStorageService.readFromJson(projectPath, Project.class);
        if (project != null) {
            // Remove project tags from the index
            removeUnusedTags(project.getTags());
        }

        // Remove from project index - Using atomic update method
        removeProjectFromIndexAtomically(id);

        // Delete project directory recursively
        return fileStorageService.deleteDirectory(projectDir);
    }

    @Override
    public Set<String> getAllTags() {
        return fileStorageService.readFromJson(TAGS_INDEX_PATH, Set.class);
    }

    @Override
    public Project addTag(String projectId, String tag) {
        String projectPath = String.format("projects/%s/project.json", projectId);

        if (!fileStorageService.fileExists(projectPath)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        // Load the project
        Project project = fileStorageService.readFromJson(projectPath, Project.class);
        if (project != null) {
            // Add the tag
            project.addTag(tag);
            project.setUpdatedAt(LocalDateTime.now());

            // Update tags index
            updateTagsIndex(Set.of(tag));

            // Save the project
            fileStorageService.saveToJson(projectPath, project);
        }

        return project;
    }

    @Override
    public Project removeTag(String projectId, String tag) {
        String projectPath = String.format("projects/%s/project.json", projectId);

        if (!fileStorageService.fileExists(projectPath)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }

        // Load the project
        Project project = fileStorageService.readFromJson(projectPath, Project.class);
        if (project != null) {
            // Remove the tag
            project.removeTag(tag);
            project.setUpdatedAt(LocalDateTime.now());

            // Remove from tags index if no longer used
            removeUnusedTags(Set.of(tag));

            // Save the project
            fileStorageService.saveToJson(projectPath, project);
        }

        return project;
    }

    /**
     * Apply filter to a project
     *
     * @param project Project to filter
     * @param filter Filter criteria
     * @return True if project matches filter
     */
    private boolean applyFilter(Project project, ProjectFilter filter) {
        // Search filter
        if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
            String search = filter.getSearch().toLowerCase();
            boolean matchesSearch =
                    (project.getName() != null && project.getName().toLowerCase().contains(search)) ||
                            (project.getDescription() != null && project.getDescription().toLowerCase().contains(search));

            if (!matchesSearch) {
                return false;
            }
        }

        // Status filter
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty() &&
                project.getStatus() != null &&
                !filter.getStatuses().contains(project.getStatus())) {
            return false;
        }

        // Type filter
        if (filter.getTypes() != null && !filter.getTypes().isEmpty() &&
                project.getType() != null &&
                !filter.getTypes().contains(project.getType())) {
            return false;
        }

        // Tags filter
        if (filter.getTags() != null && !filter.getTags().isEmpty()) {
            if (project.getTags() == null || project.getTags().isEmpty()) {
                return false;
            }

            boolean hasMatchingTag = false;
            for (String tag : filter.getTags()) {
                if (project.getTags().contains(tag)) {
                    hasMatchingTag = true;
                    break;
                }
            }

            if (!hasMatchingTag) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sort projects based on sort criteria
     *
     * @param projects List of projects to sort
     * @param sortBy Sort field
     * @param sortDirection Sort direction
     */
    private void sortProjects(List<Project> projects, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "updatedAt";
        }

        boolean ascending = "asc".equalsIgnoreCase(sortDirection);

        Comparator<Project> comparator = null;

        switch (sortBy) {
            case "name":
                comparator = Comparator.comparing(Project::getName, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "createdAt":
                comparator = Comparator.comparing(Project::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "lastRunDate":
                comparator = Comparator.comparing(Project::getLastRunDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "successRate":
                comparator = Comparator.comparing(Project::getSuccessRate);
                break;
            case "updatedAt":
            default:
                comparator = Comparator.comparing(Project::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        projects.sort(comparator);
    }

    /**
     * Update the tags index with new tags
     *
     * @param tags Tags to add to the index
     */
    private void updateTagsIndex(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        // Use atomic update method for tags index
        atomicFileUpdate(TAGS_INDEX_PATH, (fileData) -> {
            Set<String> existingTags;

            if (fileData == null || fileData.isEmpty()) {
                existingTags = new HashSet<>();
            } else {
                try {
                    // Parse existing tags from JSON
                    existingTags = fileStorageService.readFromJson(TAGS_INDEX_PATH, Set.class);
                    if (existingTags == null) {
                        existingTags = new HashSet<>();
                    }
                } catch (Exception e) {
                    // If parsing fails, create a new set
                    existingTags = new HashSet<>();
                }
            }

            // Add new tags
            existingTags.addAll(tags);

            // Convert back to JSON
            return existingTags;
        });
    }

    /**
     * Remove tags from the index if they are no longer used by any project
     *
     * @param tagsToCheck Tags to check and potentially remove
     */
    private void removeUnusedTags(Set<String> tagsToCheck) {
        if (tagsToCheck == null || tagsToCheck.isEmpty()) {
            return;
        }

        // Get all projects to check if tags are still in use
        Map<String, String> projectIndex = fileStorageService.readMapFromJson(
                PROJECTS_INDEX_PATH, String.class, String.class);

        // Build set of all tags in use
        Set<String> allUsedTags = new HashSet<>();
        for (String projectId : projectIndex.keySet()) {
            String projectPath = String.format("projects/%s/project.json", projectId);
            Project project = fileStorageService.readFromJson(projectPath, Project.class);
            if (project != null && project.getTags() != null) {
                allUsedTags.addAll(project.getTags());
            }
        }

        // Find tags that are no longer used
        Set<String> tagsToRemove = tagsToCheck.stream()
                .filter(tag -> !allUsedTags.contains(tag))
                .collect(Collectors.toSet());

        if (tagsToRemove.isEmpty()) {
            return;
        }

        // Use atomic update method for tags index
        atomicFileUpdate(TAGS_INDEX_PATH, (fileData) -> {
            Set<String> existingTags;

            try {
                // Parse existing tags from JSON
                existingTags = fileStorageService.readFromJson(TAGS_INDEX_PATH, Set.class);
                if (existingTags == null) {
                    return new HashSet<>();
                }
            } catch (Exception e) {
                // If parsing fails, return empty set
                return new HashSet<>();
            }

            // Remove unused tags
            existingTags.removeAll(tagsToRemove);

            return existingTags;
        });
    }

    /**
     * Atomically update the project index with a project name
     *
     * @param projectId Project ID
     * @param projectName Project name
     */
    private void updateProjectIndexAtomically(String projectId, String projectName) {
        atomicFileUpdate(PROJECTS_INDEX_PATH, (fileData) -> {
            Map<String, String> projectIndex;

            if (fileData == null || fileData.isEmpty()) {
                projectIndex = new HashMap<>();
            } else {
                try {
                    // Parse existing project index from JSON
                    projectIndex = fileStorageService.readMapFromJson(
                            PROJECTS_INDEX_PATH, String.class, String.class);
                    if (projectIndex == null) {
                        projectIndex = new HashMap<>();
                    }
                } catch (Exception e) {
                    // If parsing fails, create a new map
                    projectIndex = new HashMap<>();
                }
            }

            // Add or update project in index
            projectIndex.put(projectId, projectName);

            return projectIndex;
        });
    }

    /**
     * Atomically remove a project from the index
     *
     * @param projectId Project ID to remove
     */
    private void removeProjectFromIndexAtomically(String projectId) {
        atomicFileUpdate(PROJECTS_INDEX_PATH, (fileData) -> {
            Map<String, String> projectIndex;

            if (fileData == null || fileData.isEmpty()) {
                return new HashMap<String, String>();
            }

            try {
                // Parse existing project index from JSON
                projectIndex = fileStorageService.readMapFromJson(
                        PROJECTS_INDEX_PATH, String.class, String.class);
                if (projectIndex == null) {
                    return new HashMap<String, String>();
                }
            } catch (Exception e) {
                // If parsing fails, return empty map
                return new HashMap<String, String>();
            }

            // Remove project from index
            projectIndex.remove(projectId);

            return projectIndex;
        });
    }

    /**
     * Perform an atomic update on a file
     *
     * @param filePath Path to the file to update
     * @param updateFunction Function that takes the current file content and returns the updated content
     */
    private <T> void atomicFileUpdate(String filePath, java.util.function.Function<String, T> updateFunction) {
        String absolutePath = fileStorageService.getAbsolutePath(filePath);
        Path path = Paths.get(absolutePath);

        try {
            // Create parent directories if they don't exist
            Files.createDirectories(path.getParent());

            // Create the file if it doesn't exist
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            // Open the file with exclusive lock for read/write
            try (FileChannel channel = FileChannel.open(path,
                    StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                // Lock the file
                try (java.nio.channels.FileLock lock = channel.lock()) {
                    // Read current content
                    String content = readChannelContent(channel);

                    // Apply update function
                    T updatedData = updateFunction.apply(content);

                    // Write updated content back to file
                    writeUpdatedContent(channel, updatedData);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to update file", "update", filePath, e);
        }
    }

    /**
     * Read content from a file channel
     *
     * @param channel File channel to read from
     * @return Content as string
     * @throws IOException If read fails
     */
    private String readChannelContent(FileChannel channel) throws IOException {
        channel.position(0);
        long size = channel.size();

        if (size == 0) {
            return "";
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        channel.read(buffer);
        buffer.flip();

        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    /**
     * Write updated content to a file channel
     *
     * @param channel File channel to write to
     * @param data Data to write
     * @throws IOException If write fails
     */
    private <T> void writeUpdatedContent(FileChannel channel, T data) throws IOException {
        // Convert data to JSON using the same ObjectMapper as FileStorageService
        String json;
        if (data instanceof String) {
            json = (String) data;
        } else {
            // Use a temporary file to serialize data to JSON
            String tempPath = "temp_" + UUID.randomUUID().toString() + ".json";
            fileStorageService.saveToJson(tempPath, data);

            // Read the JSON content
            String tempAbsPath = fileStorageService.getAbsolutePath(tempPath);
            json = Files.readString(Paths.get(tempAbsPath));

            // Delete the temporary file
            Files.deleteIfExists(Paths.get(tempAbsPath));
        }

        // Write content to channel
        ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        channel.position(0);
        channel.write(buffer);
        channel.truncate(buffer.array().length);
    }
}