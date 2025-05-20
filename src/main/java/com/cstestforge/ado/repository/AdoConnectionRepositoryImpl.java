package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.AdoConnection;
import com.cstestforge.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AdoConnectionRepository using file system storage.
 * Connections are stored at "ado/connections/{connectionId}.json"
 * Connection index is stored at "ado/connections/_index.json"
 */
@Repository
public class AdoConnectionRepositoryImpl implements AdoConnectionRepository {

    private static final Logger logger = LoggerFactory.getLogger(AdoConnectionRepositoryImpl.class);
    private static final String CONNECTIONS_DIR = "ado/connections";
    private static final String CONNECTION_FILE = "ado/connections/%s.json";
    private static final String CONNECTIONS_INDEX = "ado/connections/_index.json";
    
    private final StorageManager storageManager;
    
    @Autowired
    public AdoConnectionRepositoryImpl(StorageManager storageManager) {
        this.storageManager = storageManager;
        // Ensure directory exists
        storageManager.createDirectory(CONNECTIONS_DIR);
        
        // Create empty index if it doesn't exist
        if (!storageManager.exists(CONNECTIONS_INDEX)) {
            storageManager.write(CONNECTIONS_INDEX, new HashMap<String, String>());
        }
    }

    @Override
    public List<AdoConnection> findAll() {
        // Get the connections index
        Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
        if (connectionsIndex == null || connectionsIndex.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Load each connection
        List<AdoConnection> connections = new ArrayList<>();
        for (String connectionId : connectionsIndex.keySet()) {
            String connectionPath = String.format(CONNECTION_FILE, connectionId);
            AdoConnection connection = storageManager.read(connectionPath, AdoConnection.class);
            if (connection != null) {
                connections.add(connection);
            }
        }
        
        // Sort connections alphabetically by name
        connections.sort(Comparator.comparing(AdoConnection::getName));
        
        return connections;
    }

    @Override
    public Optional<AdoConnection> findById(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        
        // Check if connection exists in index
        Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
        if (connectionsIndex == null || !connectionsIndex.containsKey(id)) {
            return Optional.empty();
        }
        
        // Load connection from file
        String connectionPath = String.format(CONNECTION_FILE, id);
        AdoConnection connection = storageManager.read(connectionPath, AdoConnection.class);
        
        return Optional.ofNullable(connection);
    }

    @Override
    public AdoConnection save(AdoConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        
        // Generate ID if not provided
        if (connection.getId() == null || connection.getId().isEmpty()) {
            connection.setId(UUID.randomUUID().toString());
        }
        
        // Set timestamps
        long now = System.currentTimeMillis();
        connection.setCreatedAt(now);
        connection.setUpdatedAt(now);
        
        // Save connection to file
        String connectionPath = String.format(CONNECTION_FILE, connection.getId());
        storageManager.write(connectionPath, connection);
        
        // Update connections index
        Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
        if (connectionsIndex == null) {
            connectionsIndex = new HashMap<>();
        }
        
        connectionsIndex.put(connection.getId(), connection.getName());
        storageManager.write(CONNECTIONS_INDEX, connectionsIndex);
        
        return connection;
    }

    @Override
    public AdoConnection update(String id, AdoConnection connection) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Connection ID cannot be null or empty");
        }
        
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        
        // Check if connection exists
        Optional<AdoConnection> existingConnection = findById(id);
        if (!existingConnection.isPresent()) {
            throw new IllegalArgumentException("Connection not found with ID: " + id);
        }
        
        AdoConnection original = existingConnection.get();
        
        // Update fields but preserve certain values
        connection.setId(id); // Ensure ID doesn't change
        connection.setCreatedAt(original.getCreatedAt()); // Preserve creation time
        connection.setUpdatedAt(System.currentTimeMillis()); // Update timestamp
        
        // Save updated connection
        String connectionPath = String.format(CONNECTION_FILE, id);
        storageManager.write(connectionPath, connection);
        
        // Update index if name changed
        if (!Objects.equals(original.getName(), connection.getName())) {
            Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
            connectionsIndex.put(id, connection.getName());
            storageManager.write(CONNECTIONS_INDEX, connectionsIndex);
        }
        
        return connection;
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        // Check if connection exists
        if (!exists(id)) {
            return false;
        }
        
        // Delete connection file
        String connectionPath = String.format(CONNECTION_FILE, id);
        boolean deleted = storageManager.delete(connectionPath);
        
        // Remove from index
        if (deleted) {
            Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
            connectionsIndex.remove(id);
            storageManager.write(CONNECTIONS_INDEX, connectionsIndex);
        }
        
        return deleted;
    }

    @Override
    public boolean exists(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        Map<String, String> connectionsIndex = storageManager.read(CONNECTIONS_INDEX, Map.class);
        return connectionsIndex != null && connectionsIndex.containsKey(id);
    }
} 