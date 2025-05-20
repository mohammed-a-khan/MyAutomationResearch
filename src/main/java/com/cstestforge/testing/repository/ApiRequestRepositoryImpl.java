package com.cstestforge.testing.repository;

import com.cstestforge.testing.model.ApiRequest;
import com.cstestforge.testing.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File system implementation of the ApiRequestRepository interface
 */
@Repository
public class ApiRequestRepositoryImpl implements ApiRequestRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiRequestRepositoryImpl.class);
    
    private final String dataDirectoryPath;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor with path to data directory
     * 
     * @param dataDirectoryPath Base path to store data files
     */
    public ApiRequestRepositoryImpl(@Value("${app.data.directory:./data}") String dataDirectoryPath) {
        this.dataDirectoryPath = dataDirectoryPath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Ensure directories exist
        try {
            Files.createDirectories(Paths.get(dataDirectoryPath));
            Files.createDirectories(Paths.get(dataDirectoryPath, "api-requests"));
            Files.createDirectories(Paths.get(dataDirectoryPath, "api-responses"));
        } catch (IOException e) {
            logger.error("Failed to create data directories", e);
        }
    }
    
    @Override
    public ApiRequest save(ApiRequest request) {
        if (request.getId() == null || request.getId().isEmpty()) {
            request.setId(UUID.randomUUID().toString());
        }
        
        long now = System.currentTimeMillis();
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        
        saveRequestToFile(request);
        
        return request;
    }
    
    @Override
    public ApiRequest update(ApiRequest request) {
        if (request.getId() == null || request.getId().isEmpty()) {
            throw new IllegalArgumentException("API request ID cannot be null or empty");
        }
        
        // Check if request exists
        Optional<ApiRequest> existingRequest = findById(request.getId());
        if (!existingRequest.isPresent()) {
            throw new IllegalArgumentException("API request with ID " + request.getId() + " not found");
        }
        
        // Preserve creation timestamp
        request.setCreatedAt(existingRequest.get().getCreatedAt());
        request.setUpdatedAt(System.currentTimeMillis());
        
        saveRequestToFile(request);
        
        return request;
    }
    
    @Override
    public Optional<ApiRequest> findById(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        
        Path filePath = Paths.get(dataDirectoryPath, "api-requests", id + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        
        try {
            ApiRequest request = objectMapper.readValue(filePath.toFile(), ApiRequest.class);
            return Optional.of(request);
        } catch (IOException e) {
            logger.error("Error reading API request from {}", filePath, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<ApiRequest> findAll() {
        Path requestsDir = Paths.get(dataDirectoryPath, "api-requests");
        
        try {
            if (!Files.exists(requestsDir)) {
                Files.createDirectories(requestsDir);
                return Collections.emptyList();
            }
            
            try (Stream<Path> paths = Files.list(requestsDir)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(this::readRequestFromFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(Comparator.comparing(ApiRequest::getUpdatedAt).reversed())
                    .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.error("Error listing API requests", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<ApiRequest> findByProjectId(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return Collections.emptyList();
        }
        
        return findAll().stream()
            .filter(request -> projectId.equals(request.getProjectId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        Path filePath = Paths.get(dataDirectoryPath, "api-requests", id + ".json");
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Error deleting API request with ID {}", id, e);
            return false;
        }
    }
    
    @Override
    public ApiResponse saveResponse(ApiResponse response) {
        if (response.getId() == null || response.getId().isEmpty()) {
            response.setId(UUID.randomUUID().toString());
        }
        
        if (response.getTimestamp() == null) {
            response.setTimestamp(LocalDateTime.now());
        }
        
        try {
            Path responsesDir = Paths.get(dataDirectoryPath, "api-responses");
            Path requestResponsesDir = responsesDir.resolve(response.getRequestId());
            Files.createDirectories(requestResponsesDir);
            
            Path filePath = requestResponsesDir.resolve(response.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), response);
            
            return response;
        } catch (IOException e) {
            logger.error("Error saving API response with ID {}", response.getId(), e);
            throw new RuntimeException("Failed to save API response", e);
        }
    }
    
    @Override
    public List<ApiResponse> getResponseHistory(String requestId, int limit) {
        if (requestId == null || requestId.isEmpty()) {
            return Collections.emptyList();
        }
        
        Path responsesDir = Paths.get(dataDirectoryPath, "api-responses", requestId);
        if (!Files.exists(responsesDir)) {
            return Collections.emptyList();
        }
        
        try (Stream<Path> paths = Files.list(responsesDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(this::readResponseFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(ApiResponse::getTimestamp).reversed())
                .limit(limit > 0 ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing API response history for request ID {}", requestId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Save an API request to file
     * 
     * @param request API request to save
     */
    private void saveRequestToFile(ApiRequest request) {
        try {
            Path requestsDir = Paths.get(dataDirectoryPath, "api-requests");
            Files.createDirectories(requestsDir);
            
            Path filePath = requestsDir.resolve(request.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), request);
        } catch (IOException e) {
            logger.error("Error saving API request with ID {}", request.getId(), e);
            throw new RuntimeException("Failed to save API request", e);
        }
    }
    
    /**
     * Read an API request from file
     * 
     * @param path Path to the file
     * @return Optional containing the API request, or empty if the file could not be read
     */
    private Optional<ApiRequest> readRequestFromFile(Path path) {
        try {
            ApiRequest request = objectMapper.readValue(path.toFile(), ApiRequest.class);
            return Optional.of(request);
        } catch (IOException e) {
            logger.error("Error reading API request from {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Read an API response from file
     * 
     * @param path Path to the file
     * @return Optional containing the API response, or empty if the file could not be read
     */
    private Optional<ApiResponse> readResponseFromFile(Path path) {
        try {
            ApiResponse response = objectMapper.readValue(path.toFile(), ApiResponse.class);
            return Optional.of(response);
        } catch (IOException e) {
            logger.error("Error reading API response from {}", path, e);
            return Optional.empty();
        }
    }
}