package com.cstestforge.ado.repository;

import com.cstestforge.ado.model.AdoConnection;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ADO connections.
 * Connections are stored at "ado/connections/"
 */
public interface AdoConnectionRepository {

    /**
     * Find all ADO connections
     *
     * @return List of all connections
     */
    List<AdoConnection> findAll();
    
    /**
     * Find a connection by its ID
     *
     * @param id Connection ID
     * @return Optional containing the connection if found
     */
    Optional<AdoConnection> findById(String id);
    
    /**
     * Create a new connection
     *
     * @param connection Connection to create
     * @return Created connection with ID
     */
    AdoConnection save(AdoConnection connection);
    
    /**
     * Update an existing connection
     *
     * @param id Connection ID
     * @param connection Updated connection data
     * @return Updated connection
     */
    AdoConnection update(String id, AdoConnection connection);
    
    /**
     * Delete a connection
     *
     * @param id Connection ID
     * @return true if deleted successfully
     */
    boolean delete(String id);
    
    /**
     * Check if a connection exists
     * 
     * @param id Connection ID
     * @return true if the connection exists
     */
    boolean exists(String id);
} 