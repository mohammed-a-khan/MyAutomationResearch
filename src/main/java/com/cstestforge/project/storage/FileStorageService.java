package com.cstestforge.project.storage;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Service for managing file-based storage operations.
 */
public interface FileStorageService {

    /**
     * Save an object to a JSON file
     * 
     * @param <T> Type of object to save
     * @param path Path to save the file (relative to storage root)
     * @param data Data to save
     * @return True if saved successfully
     */
    <T> boolean saveToJson(String path, T data);

    /**
     * Read an object from a JSON file
     * 
     * @param <T> Type of object to read
     * @param path Path to the file (relative to storage root)
     * @param type Class of object to read
     * @return The object read from the file or null if not found
     */
    <T> T readFromJson(String path, Class<T> type);

    /**
     * Read a list of objects from a JSON file
     * 
     * @param <T> Type of objects in the list
     * @param path Path to the file (relative to storage root)
     * @param type Class of objects in the list
     * @return List of objects or empty list if not found
     */
    <T> List<T> readListFromJson(String path, Class<T> type);

    /**
     * Read a map from a JSON file
     * 
     * @param <K> Type of keys in the map
     * @param <V> Type of values in the map
     * @param path Path to the file (relative to storage root)
     * @param keyType Class of keys
     * @param valueType Class of values
     * @return Map of objects or empty map if not found
     */
    <K, V> Map<K, V> readMapFromJson(String path, Class<K> keyType, Class<V> valueType);

    <K, V> void updateMapInJsonFile(String path, BiFunction<Map<K, V>, Object, Map<K, V>> updateFunction,
                                    Class<K> keyType, Class<V> valueType, Object param);

    /**
     * Delete a file
     * 
     * @param path Path to the file (relative to storage root)
     * @return True if deleted successfully
     */
    boolean deleteFile(String path);

    /**
     * Delete a directory and all its contents recursively
     * 
     * @param path Path to the directory (relative to storage root)
     * @return True if deleted successfully
     */
    boolean deleteDirectory(String path);

    /**
     * Check if a file exists
     * 
     * @param path Path to the file (relative to storage root)
     * @return True if file exists
     */
    boolean fileExists(String path);

    /**
     * Create directory if it doesn't exist
     * 
     * @param path Path to the directory (relative to storage root)
     * @return True if directory exists or was created
     */
    boolean createDirectoryIfNotExists(String path);
    
    /**
     * Create directory. Creates parent directories if they don't exist.
     * 
     * @param path Path to the directory (relative to storage root)
     * @return True if directory was created successfully
     */
    boolean createDirectory(String path);

    /**
     * Lists all files in a directory that match a predicate
     * 
     * @param directoryPath Path to the directory (relative to storage root)
     * @param predicate Filter for files
     * @return List of file paths
     */
    List<String> listFiles(String directoryPath, Predicate<String> predicate);

    /**
     * Get the absolute path from a relative path
     * 
     * @param relativePath Relative path (from storage root)
     * @return Absolute path
     */
    String getAbsolutePath(String relativePath);

    /**
     * Lock a file for exclusive access
     * 
     * @param path Path to the file (relative to storage root)
     * @return Lock object or null if unable to lock
     */
    FileLock lockFile(String path);

    /**
     * Lock multiple files for atomic operations
     * 
     * @param paths Paths to lock (relative to storage root)
     * @return Combined lock object or null if unable to lock all files
     */
    FileLock lockFiles(List<String> paths);
} 