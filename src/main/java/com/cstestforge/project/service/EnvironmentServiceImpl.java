package com.cstestforge.project.service;

import com.cstestforge.project.model.Environment;
import com.cstestforge.project.model.EnvironmentVariable;
import com.cstestforge.project.storage.FileLock;
import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.project.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the EnvironmentService using file-based storage.
 */
@Service
public class EnvironmentServiceImpl implements EnvironmentService {

    private final FileStorageService fileStorageService;
    private final EncryptionUtil encryptionUtil;

    @Autowired
    public EnvironmentServiceImpl(FileStorageService fileStorageService, EncryptionUtil encryptionUtil) {
        this.fileStorageService = fileStorageService;
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public List<Environment> findAllByProjectId(String projectId) {
        String envIndexPath = String.format("projects/%s/environments/_index.json", projectId);
        
        if (!fileStorageService.fileExists(envIndexPath)) {
            return new ArrayList<>();
        }
        
        Map<String, String> envIndex = fileStorageService.readMapFromJson(
                envIndexPath, String.class, String.class);
        
        List<Environment> environments = new ArrayList<>();
        
        for (String envId : envIndex.keySet()) {
            String envPath = String.format("projects/%s/environments/%s.json", projectId, envId);
            Environment env = fileStorageService.readFromJson(envPath, Environment.class);
            
            if (env != null) {
                // Decrypt secret variables
                decryptSecretVariables(env);
                environments.add(env);
            }
        }
        
        return environments;
    }

    @Override
    public Optional<Environment> findById(String projectId, String id) {
        String envPath = String.format("projects/%s/environments/%s.json", projectId, id);
        
        if (!fileStorageService.fileExists(envPath)) {
            return Optional.empty();
        }
        
        Environment env = fileStorageService.readFromJson(envPath, Environment.class);
        
        if (env != null) {
            // Decrypt secret variables
            decryptSecretVariables(env);
        }
        
        return Optional.ofNullable(env);
    }

    @Override
    public Environment create(String projectId, Environment environment) {
        // Ensure project exists
        String projectPath = String.format("projects/%s/project.json", projectId);
        if (!fileStorageService.fileExists(projectPath)) {
            throw new IllegalArgumentException("Project not found with ID: " + projectId);
        }
        
        // Ensure environment ID is set
        if (environment.getId() == null || environment.getId().isEmpty()) {
            environment.setId(java.util.UUID.randomUUID().toString());
        }
        
        // Encrypt secret variables
        encryptSecretVariables(environment);
        
        // Save environment
        String envPath = String.format("projects/%s/environments/%s.json", projectId, environment.getId());
        fileStorageService.saveToJson(envPath, environment);
        
        // Update environment index
        String envIndexPath = String.format("projects/%s/environments/_index.json", projectId);
        
        try (FileLock lock = fileStorageService.lockFile(envIndexPath)) {
            if (lock != null) {
                Map<String, String> envIndex = fileStorageService.readMapFromJson(
                        envIndexPath, String.class, String.class);
                
                if (envIndex == null) {
                    envIndex = new HashMap<>();
                }
                
                envIndex.put(environment.getId(), environment.getName());
                fileStorageService.saveToJson(envIndexPath, envIndex);
            }
        }
        
        // If this is the first environment or marked as default, update project settings
        List<Environment> existingEnvs = findAllByProjectId(projectId);
        if (existingEnvs.size() == 1 || environment.isDefault()) {
            setAsDefault(projectId, environment.getId());
        }
        
        // Decrypt for return
        decryptSecretVariables(environment);
        
        return environment;
    }

    @Override
    public Environment update(String projectId, String id, Environment environment) {
        // Verify environment exists
        String envPath = String.format("projects/%s/environments/%s.json", projectId, id);
        
        if (!fileStorageService.fileExists(envPath)) {
            throw new IllegalArgumentException("Environment not found with ID: " + id);
        }
        
        // Load existing environment
        Environment existingEnv = fileStorageService.readFromJson(envPath, Environment.class);
        
        // Update fields
        existingEnv.setName(environment.getName());
        existingEnv.setUrl(environment.getUrl());
        existingEnv.setDescription(environment.getDescription());
        
        // Handle variables separately to maintain encryption
        if (environment.getVariables() != null) {
            existingEnv.setVariables(environment.getVariables());
            encryptSecretVariables(existingEnv);
        }
        
        // Save updated environment
        fileStorageService.saveToJson(envPath, existingEnv);
        
        // Update environment index if name changed
        String envIndexPath = String.format("projects/%s/environments/_index.json", projectId);
        
        try (FileLock lock = fileStorageService.lockFile(envIndexPath)) {
            if (lock != null) {
                Map<String, String> envIndex = fileStorageService.readMapFromJson(
                        envIndexPath, String.class, String.class);
                
                envIndex.put(id, existingEnv.getName());
                fileStorageService.saveToJson(envIndexPath, envIndex);
            }
        }
        
        // If marked as default, update project settings
        if (environment.isDefault()) {
            setAsDefault(projectId, id);
        }
        
        // Decrypt for return
        decryptSecretVariables(existingEnv);
        
        return existingEnv;
    }

    @Override
    public boolean delete(String projectId, String id) {
        String envPath = String.format("projects/%s/environments/%s.json", projectId, id);
        
        if (!fileStorageService.fileExists(envPath)) {
            return false;
        }
        
        // Load environment to check if it's default
        Environment env = fileStorageService.readFromJson(envPath, Environment.class);
        boolean wasDefault = env != null && env.isDefault();
        
        // Remove from environment index
        String envIndexPath = String.format("projects/%s/environments/_index.json", projectId);
        
        try (FileLock lock = fileStorageService.lockFile(envIndexPath)) {
            if (lock != null) {
                Map<String, String> envIndex = fileStorageService.readMapFromJson(
                        envIndexPath, String.class, String.class);
                
                envIndex.remove(id);
                fileStorageService.saveToJson(envIndexPath, envIndex);
            }
        }
        
        // Delete environment file
        boolean deleted = fileStorageService.deleteFile(envPath);
        
        // If this was the default environment, set a new default if possible
        if (wasDefault) {
            List<Environment> remainingEnvs = findAllByProjectId(projectId);
            if (!remainingEnvs.isEmpty()) {
                setAsDefault(projectId, remainingEnvs.get(0).getId());
            }
        }
        
        return deleted;
    }

    @Override
    public Environment setAsDefault(String projectId, String id) {
        // Verify environment exists
        String envPath = String.format("projects/%s/environments/%s.json", projectId, id);
        
        if (!fileStorageService.fileExists(envPath)) {
            throw new IllegalArgumentException("Environment not found with ID: " + id);
        }
        
        // Get all environments
        List<Environment> environments = findAllByProjectId(projectId);
        
        // Update default status
        for (Environment env : environments) {
            boolean shouldBeDefault = env.getId().equals(id);
            
            if (env.isDefault() != shouldBeDefault) {
                env.setDefault(shouldBeDefault);
                
                // Re-encrypt secret variables
                encryptSecretVariables(env);
                
                // Save environment
                String currentEnvPath = String.format("projects/%s/environments/%s.json", projectId, env.getId());
                fileStorageService.saveToJson(currentEnvPath, env);
            }
        }
        
        // Update project settings
        String settingsPath = String.format("projects/%s/settings.json", projectId);
        
        if (fileStorageService.fileExists(settingsPath)) {
            try (FileLock lock = fileStorageService.lockFile(settingsPath)) {
                if (lock != null) {
                    Map<String, Object> settings = fileStorageService.readFromJson(settingsPath, Map.class);
                    
                    if (settings == null) {
                        settings = new HashMap<>();
                    }
                    
                    settings.put("defaultEnvironment", id);
                    fileStorageService.saveToJson(settingsPath, settings);
                }
            }
        }
        
        // Return the environment that was set as default
        return findById(projectId, id).orElse(null);
    }

    @Override
    public Environment addVariable(String projectId, String environmentId, EnvironmentVariable variable) {
        // Verify environment exists
        Optional<Environment> envOpt = findById(projectId, environmentId);
        
        if (!envOpt.isPresent()) {
            throw new IllegalArgumentException("Environment not found with ID: " + environmentId);
        }
        
        Environment env = envOpt.get();
        
        // Add variable
        env.addVariable(variable);
        
        // Encrypt and save
        encryptSecretVariables(env);
        String envPath = String.format("projects/%s/environments/%s.json", projectId, environmentId);
        fileStorageService.saveToJson(envPath, env);
        
        // Decrypt for return
        decryptSecretVariables(env);
        
        return env;
    }

    @Override
    public Environment updateVariable(String projectId, String environmentId, String variableId, EnvironmentVariable variable) {
        // Verify environment exists
        Optional<Environment> envOpt = findById(projectId, environmentId);
        
        if (!envOpt.isPresent()) {
            throw new IllegalArgumentException("Environment not found with ID: " + environmentId);
        }
        
        Environment env = envOpt.get();
        
        // Find and update variable
        EnvironmentVariable existingVar = env.findVariable(variableId);
        
        if (existingVar == null) {
            throw new IllegalArgumentException("Variable not found with ID: " + variableId);
        }
        
        existingVar.setName(variable.getName());
        existingVar.setValue(variable.getValue());
        existingVar.setSecret(variable.isSecret());
        
        // Encrypt and save
        encryptSecretVariables(env);
        String envPath = String.format("projects/%s/environments/%s.json", projectId, environmentId);
        fileStorageService.saveToJson(envPath, env);
        
        // Decrypt for return
        decryptSecretVariables(env);
        
        return env;
    }

    @Override
    public Environment deleteVariable(String projectId, String environmentId, String variableId) {
        // Verify environment exists
        Optional<Environment> envOpt = findById(projectId, environmentId);
        
        if (!envOpt.isPresent()) {
            throw new IllegalArgumentException("Environment not found with ID: " + environmentId);
        }
        
        Environment env = envOpt.get();
        
        // Remove variable
        boolean removed = env.removeVariable(variableId);
        
        if (!removed) {
            throw new IllegalArgumentException("Variable not found with ID: " + variableId);
        }
        
        // Encrypt and save
        encryptSecretVariables(env);
        String envPath = String.format("projects/%s/environments/%s.json", projectId, environmentId);
        fileStorageService.saveToJson(envPath, env);
        
        // Decrypt for return
        decryptSecretVariables(env);
        
        return env;
    }
    
    /**
     * Encrypt secret variables before saving
     * 
     * @param environment Environment with variables to encrypt
     */
    private void encryptSecretVariables(Environment environment) {
        if (environment.getVariables() == null) {
            return;
        }
        
        for (EnvironmentVariable var : environment.getVariables()) {
            if (var.isSecret() && var.getValue() != null) {
                var.setValue(encryptionUtil.encrypt(var.getValue()));
            }
        }
    }
    
    /**
     * Decrypt secret variables after loading
     * 
     * @param environment Environment with variables to decrypt
     */
    private void decryptSecretVariables(Environment environment) {
        if (environment.getVariables() == null) {
            return;
        }
        
        for (EnvironmentVariable var : environment.getVariables()) {
            if (var.isSecret() && var.getValue() != null) {
                var.setValue(encryptionUtil.decrypt(var.getValue()));
            }
        }
    }
} 