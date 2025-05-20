package com.cstestforge.project.controller;

import com.cstestforge.project.model.ApiResponse;
import com.cstestforge.project.model.PagedResponse;
import com.cstestforge.project.model.PaginationParams;
import com.cstestforge.project.model.Project;
import com.cstestforge.project.model.ProjectFilter;
import com.cstestforge.project.model.ProjectListResponse;
import com.cstestforge.project.model.ProjectStatus;
import com.cstestforge.project.model.ProjectType;
import com.cstestforge.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for project operations.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Get all projects with filtering, pagination and sorting
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectListResponse>> getAllProjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String[] statuses,
            @RequestParam(required = false) String[] types,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        try {
            // Create filter from request parameters
            ProjectFilter filter = new ProjectFilter();
            filter.setSearch(search);
            filter.setPage(page);
            filter.setSize(size);
            filter.setSortBy(sortBy);
            filter.setSortDirection(sortDirection);

            // Convert string arrays to enums if provided
            if (statuses != null && statuses.length > 0) {
                filter.setStatuses(Arrays.stream(statuses)
                        .map(s -> ProjectStatus.valueOf(s))
                        .collect(Collectors.toList()));
            }

            if (types != null && types.length > 0) {
                filter.setTypes(Arrays.stream(types)
                        .map(t -> ProjectType.valueOf(t))
                        .collect(Collectors.toList()));
            }

            if (tags != null && tags.length > 0) {
                filter.setTags(Arrays.asList(tags));
            }

            PagedResponse<Project> pagedResponse = projectService.findAll(filter);

            // Create the expected response format
            ProjectListResponse response = new ProjectListResponse(
                    pagedResponse.getItems(),
                    new PaginationParams(
                            pagedResponse.getPage(),
                            pagedResponse.getSize(),
                            pagedResponse.getTotalItems(),
                            pagedResponse.getTotalPages()
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving projects", e.getMessage()));
        }
    }

    /**
     * Get a project by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> getProjectById(@PathVariable String id) {
        try {
            return projectService.findById(id)
                    .map(project -> ResponseEntity.ok(ApiResponse.success(project)))
                    .orElse(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Project not found", "No project found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving project", e.getMessage()));
        }
    }

    /**
     * Create a new project
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Project>> createProject(@RequestBody Project project) {
        try {
            System.out.println("Received project creation request:");
            System.out.println("Project name: " + project.getName());
            System.out.println("Project type: " + (project.getType() != null ? project.getType() : "null"));

            // Set defaults if needed
            if (project.getType() == null) {
                System.out.println("Setting default project type: WEB");
                project.setType(ProjectType.WEB);
            }

            if (project.getStatus() == null) {
                System.out.println("Setting default project status: DRAFT");
                project.setStatus(ProjectStatus.DRAFT);
            }

            Project createdProject = projectService.create(project);
            System.out.println("Project created successfully with ID: " + createdProject.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdProject, "Project created successfully"));
        } catch (Exception e) {
            e.printStackTrace();

            // Log full error details
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                System.err.println("Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
            }

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating project", e.getMessage()));
        }
    }

    /**
     * Update an existing project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> updateProject(@PathVariable String id, @RequestBody Project project) {
        try {
            Project updatedProject = projectService.update(id, project);
            return ResponseEntity.ok(ApiResponse.success(updatedProject, "Project updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating project", e.getMessage()));
        }
    }

    /**
     * Delete a project
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable String id) {
        try {
            boolean deleted = projectService.delete(id);

            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null, "Project deleted successfully"));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Project not found", "No project found with ID: " + id));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting project", e.getMessage()));
        }
    }

    /**
     * Get all unique tags
     */
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<Set<String>>> getAllTags() {
        try {
            Set<String> tags = projectService.getAllTags();
            return ResponseEntity.ok(ApiResponse.success(tags));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving tags", e.getMessage()));
        }
    }

    /**
     * Add a tag to a project
     */
    @PostMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Project>> addTag(@PathVariable String id, @PathVariable String tag) {
        try {
            Project updatedProject = projectService.addTag(id, tag);
            return ResponseEntity.ok(ApiResponse.success(updatedProject, "Tag added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error adding tag", e.getMessage()));
        }
    }

    /**
     * Remove a tag from a project
     */
    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<ApiResponse<Project>> removeTag(@PathVariable String id, @PathVariable String tag) {
        try {
            Project updatedProject = projectService.removeTag(id, tag);
            return ResponseEntity.ok(ApiResponse.success(updatedProject, "Tag removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error removing tag", e.getMessage()));
        }
    }
}