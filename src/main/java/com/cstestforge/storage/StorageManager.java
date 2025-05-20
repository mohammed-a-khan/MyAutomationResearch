package com.cstestforge.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced storage manager with caching, versioning, and transaction support.
 */
@Service
public class StorageManager implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    
    private static final String VERSION_FIELD = "_version";
    private static final String TIMESTAMP_FIELD = "_lastModified";
    private static final String HISTORY_DIR = "_history";
    
    @Value("${cstestforge.storage.base-dir:./storage}")
    private String baseStorageDirectory;
    
    @Value("${cstestforge.storage.cache.max-size:1000}")
    private int maxCacheSize;
    
    @Value("${cstestforge.storage.cache.ttl-minutes:15}")
    private int cacheTtlMinutes;
    
    @Value("${cstestforge.storage.lock.timeout-seconds:30}")
    private int lockTimeoutSeconds;
    
    @Value("${cstestforge.storage.versioning.enabled:true}")
    private boolean versioningEnabled;
    
    @Value("${cstestforge.storage.versioning.max-versions:10}")
    private int maxVersions;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // LRU cache with time-based eviction
    private final Map<String, CacheEntry<?>> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, CacheEntry<?>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<?>> eldest) {
                    return size() > maxCacheSize;
                }
            });
    
    private final ScheduledExecutorService cacheCleanupService = 
            Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Initialize the storage system. Called after properties are set.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // Create storage directory if it doesn't exist
        File storageDir = new File(baseStorageDirectory);
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                logger.error("Failed to create storage directory: {}", baseStorageDirectory);
                throw new RuntimeException("Failed to initialize storage system");
            }
        }
        
        // Schedule cache cleanup
        cacheCleanupService.scheduleAtFixedRate(
                this::cleanupCache, 
                cacheTtlMinutes, 
                cacheTtlMinutes, 
                TimeUnit.MINUTES);
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupCache() {
        try {
            logger.debug("Starting cache cleanup");
            Instant now = Instant.now();
            
            synchronized (cache) {
                Iterator<Map.Entry<String, CacheEntry<?>>> iterator = cache.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, CacheEntry<?>> entry = iterator.next();
                    if (entry.getValue().isExpired(now)) {
                        iterator.remove();
                    }
                }
            }
            
            logger.debug("Cache cleanup complete. Current size: {}", cache.size());
        } catch (Exception e) {
            logger.error("Error during cache cleanup", e);
        }
    }
    
    /**
     * Get the absolute path for a relative path
     * 
     * @param relativePath Relative path from storage root
     * @return Absolute path
     */
    public String getAbsolutePath(String relativePath) {
        return Paths.get(baseStorageDirectory, relativePath).toString();
    }
    
    /**
     * Read an object from storage with caching
     * 
     * @param <T> Type of object
     * @param path Relative path
     * @param type Class of the object
     * @return The object or null if not found
     */
    public <T> T read(String path, Class<T> type) {
        return read(path, type, null);
    }
    
    /**
     * Read an object with optional transformation function
     * 
     * @param <T> Type of object
     * @param path Relative path
     * @param type Class of the object
     * @param transformFunction Optional function to transform the object
     * @return The object or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T read(String path, Class<T> type, Function<T, T> transformFunction) {
        String cacheKey = path + "#" + type.getName();
        
        // Try to get from cache
        CacheEntry<T> cacheEntry = (CacheEntry<T>) cache.get(cacheKey);
        if (cacheEntry != null && !cacheEntry.isExpired(Instant.now())) {
            logger.debug("Cache hit for: {}", path);
            return cacheEntry.getData();
        }
        
        // Not in cache, read from file
        File file = new File(getAbsolutePath(path));
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        try {
            T data = objectMapper.readValue(file, type);
            
            // Apply transformation if provided
            if (transformFunction != null) {
                data = transformFunction.apply(data);
            }
            
            // Update cache
            cache.put(cacheKey, new CacheEntry<>(data));
            
            return data;
        } catch (IOException e) {
            logger.error("Error reading file: {}", path, e);
            return null;
        }
    }
    
    /**
     * Write an object to storage
     * 
     * @param <T> Type of object
     * @param path Relative path
     * @param data Data to write
     * @return True if successful
     */
    public <T> boolean write(String path, T data) {
        try {
            File file = new File(getAbsolutePath(path));
            File parentDir = file.getParentFile();
            
            // Create parent directory if needed
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Add versioning info if enabled
            if (versioningEnabled && data instanceof Map) {
                ObjectNode node = objectMapper.valueToTree(data);
                node.put(VERSION_FIELD, UUID.randomUUID().toString());
                node.put(TIMESTAMP_FIELD, Instant.now().toString());
                data = (T) objectMapper.treeToValue(node, data.getClass());
            }
            
            // Create a backup before writing if the file exists and versioning is enabled
            if (versioningEnabled && file.exists()) {
                createVersionBackup(path);
            }
            
            // Write to file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            
            // Update cache
            String cacheKey = path + "#" + data.getClass().getName();
            cache.put(cacheKey, new CacheEntry<>(data));
            
            return true;
        } catch (IOException e) {
            logger.error("Error writing to file: {}", path, e);
            return false;
        }
    }
    
    /**
     * Create a version backup of a file
     * 
     * @param path Path to the file to backup
     */
    private void createVersionBackup(String path) throws IOException {
        File sourceFile = new File(getAbsolutePath(path));
        if (!sourceFile.exists()) {
            return;
        }
        
        // Read the current version information
        JsonNode rootNode = objectMapper.readTree(sourceFile);
        String version = rootNode.has(VERSION_FIELD) ? 
                rootNode.get(VERSION_FIELD).asText() : 
                UUID.randomUUID().toString();
        
        // Create history directory if it doesn't exist
        Path historyDir = Paths.get(sourceFile.getParent(), HISTORY_DIR);
        if (!Files.exists(historyDir)) {
            Files.createDirectories(historyDir);
        }
        
        // Create versioned backup
        String timestamp = Instant.now().toString().replace(":", "-");
        String fileName = sourceFile.getName();
        String backupFileName = fileName.substring(0, fileName.lastIndexOf('.')) + 
                "_" + version + "_" + timestamp + ".json";
        
        Path backupPath = historyDir.resolve(backupFileName);
        Files.copy(sourceFile.toPath(), backupPath);
        
        // Clean up old versions if needed
        cleanupOldVersions(historyDir.toFile(), fileName.substring(0, fileName.lastIndexOf('.')));
    }
    
    /**
     * Clean up old versions, keeping only the most recent ones
     * 
     * @param historyDir Directory containing version history
     * @param filePrefix Prefix of the file name
     */
    private void cleanupOldVersions(File historyDir, String filePrefix) {
        if (!historyDir.exists() || !historyDir.isDirectory()) {
            return;
        }
        
        File[] versionFiles = historyDir.listFiles((dir, name) -> 
                name.startsWith(filePrefix) && name.endsWith(".json"));
        
        if (versionFiles != null && versionFiles.length > maxVersions) {
            // Sort by last modified time (oldest first)
            Arrays.sort(versionFiles, Comparator.comparingLong(File::lastModified));
            
            // Delete oldest files beyond the max limit
            for (int i = 0; i < versionFiles.length - maxVersions; i++) {
                versionFiles[i].delete();
            }
        }
    }
    
    /**
     * Delete a file or directory
     * 
     * @param path Relative path
     * @return True if deleted successfully
     */
    public boolean delete(String path) {
        File file = new File(getAbsolutePath(path));
        if (!file.exists()) {
            return true; // Already doesn't exist
        }
        
        // Clear from cache
        synchronized (cache) {
            cache.entrySet().removeIf(entry -> entry.getKey().startsWith(path));
        }
        
        // Delete the file or directory
        if (file.isDirectory()) {
            return deleteDirectory(file);
        } else {
            return file.delete();
        }
    }
    
    /**
     * Recursively delete a directory
     * 
     * @param directory Directory to delete
     * @return True if deleted successfully
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * Check if a file exists
     * 
     * @param path Relative path
     * @return True if file exists
     */
    public boolean exists(String path) {
        return new File(getAbsolutePath(path)).exists();
    }
    
    /**
     * Create a directory if it doesn't exist
     * 
     * @param path Relative path
     * @return True if directory exists or was created
     */
    public boolean createDirectory(String path) {
        File dir = new File(getAbsolutePath(path));
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
    
    /**
     * List files in a directory
     * 
     * @param directoryPath Relative directory path
     * @param filter Optional filter predicate
     * @return List of file names
     */
    public List<String> listFiles(String directoryPath, java.util.function.Predicate<String> filter) {
        File dir = new File(getAbsolutePath(directoryPath));
        
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(files)
                .filter(File::isFile)
                .map(File::getName)
                .filter(filter != null ? filter : name -> true)
                .collect(Collectors.toList());
    }
    
    /**
     * Acquire a lock on a file
     * 
     * @param path Relative path
     * @return Lock object
     */
    public EnhancedFileLock lock(String path) {
        File file = new File(getAbsolutePath(path));
        EnhancedFileLock lock = new EnhancedFileLock(file, TimeUnit.SECONDS.toMillis(lockTimeoutSeconds));
        
        try {
            if (lock.acquire()) {
                return lock;
            } else {
                throw new RuntimeException("Failed to acquire lock on: " + path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error acquiring lock on: " + path, e);
        }
    }
    
    /**
     * Execute a function within a transaction (lock multiple files)
     * 
     * @param <T> Return type of the function
     * @param paths Paths to lock
     * @param function Function to execute
     * @return Result of the function
     */
    public <T> T executeInTransaction(List<String> paths, Function<Void, T> function) {
        // Sort paths to prevent deadlocks
        List<String> sortedPaths = new ArrayList<>(paths);
        Collections.sort(sortedPaths);
        
        // Acquire locks in order
        List<EnhancedFileLock> locks = new ArrayList<>();
        
        try {
            for (String path : sortedPaths) {
                EnhancedFileLock lock = lock(path);
                locks.add(lock);
            }
            
            // Execute function
            return function.apply(null);
        } finally {
            // Release locks in reverse order
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).release();
            }
        }
    }
    
    /**
     * Cache entry with expiration
     */
    private static class CacheEntry<T> {
        private final T data;
        private final Instant expirationTime;
        
        public CacheEntry(T data) {
            this.data = data;
            this.expirationTime = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(15)); // Default 15 minutes
        }
        
        public T getData() {
            return data;
        }
        
        public boolean isExpired(Instant now) {
            return now.isAfter(expirationTime);
        }
    }
} 