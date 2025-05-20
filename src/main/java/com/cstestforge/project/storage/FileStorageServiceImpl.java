package com.cstestforge.project.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.cstestforge.project.exception.StorageException;
import com.cstestforge.project.exception.ConcurrencyException;

/**
 * Implementation of the FileStorageService for managing file-based storage operations.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.storage.root:./storage}")
    private String storageRoot;

    private final ObjectMapper objectMapper;

    public FileStorageServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public <T> boolean saveToJson(String path, T data) {
        try {
            // Ensure storage root exists
            File rootDir = new File(storageRoot);
            if (!rootDir.exists()) {
                boolean created = rootDir.mkdirs();
                if (!created) {
                    throw new StorageException("Failed to create storage root directory: " + storageRoot);
                }
            }

            // Ensure parent directory exists
            File file = new File(getAbsolutePath(path));
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    throw new StorageException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            // Create a backup of the existing file if it exists
            if (file.exists()) {
                String backupPath = path + ".bak";
                Files.copy(Paths.get(getAbsolutePath(path)), Paths.get(getAbsolutePath(backupPath)), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Write the data to the file
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, data);
            
            return true;
        } catch (IOException e) {
            throw new StorageException("Failed to save file", "write", path, e);
        }
    }

    @Override
    public <T> T readFromJson(String path, Class<T> type) {
        File file = new File(getAbsolutePath(path));
        if (!file.exists()) {
            return null;
        }

        try {
            return objectMapper.readValue(file, type);
        } catch (IOException e) {
            throw new StorageException("Failed to read file", "read", path, e);
        }
    }

    @Override
    public <T> List<T> readListFromJson(String path, Class<T> type) {
        File file = new File(getAbsolutePath(path));
        if (!file.exists()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(file, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new StorageException("Failed to read list from file", "read", path, e);
        }
    }

    @Override
    public <K, V> Map<K, V> readMapFromJson(String path, Class<K> keyType, Class<V> valueType) {
        File file = new File(getAbsolutePath(path));
        if (!file.exists()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(file, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (IOException e) {
            throw new StorageException("Failed to read map from file", "read", path, e);
        }
    }

    @Override
    public boolean deleteFile(String path) {
        File file = new File(getAbsolutePath(path));
        return !file.exists() || file.delete();
    }

    @Override
    public boolean deleteDirectory(String path) {
        Path directoryPath = Paths.get(getAbsolutePath(path));
        if (!Files.exists(directoryPath)) {
            return true; // Directory doesn't exist, so it's already "deleted"
        }

        try {
            // Walk through the directory tree and delete all files and subdirectories
            Files.walk(directoryPath)
                .sorted((p1, p2) -> -p1.compareTo(p2)) // Reverse order to delete children before parents
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new StorageException("Failed to delete path: " + p);
                    }
                });
            return true;
        } catch (IOException e) {
            throw new StorageException("Failed to delete directory: " + path);
        }
    }

    @Override
    public boolean fileExists(String path) {
        return new File(getAbsolutePath(path)).exists();
    }

    @Override
    public boolean createDirectoryIfNotExists(String path) {
        File dir = new File(getAbsolutePath(path));
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        File dir = new File(getAbsolutePath(path));
        if (dir.exists()) {
            return dir.isDirectory();
        }
        return dir.mkdirs();
    }

    @Override
    public List<String> listFiles(String directoryPath, Predicate<String> predicate) {
        File dir = new File(getAbsolutePath(directoryPath));
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(dir.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(predicate)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to list files", "list", directoryPath, e);
        }
    }

    @Override
    public String getAbsolutePath(String relativePath) {
        return Paths.get(storageRoot, relativePath).toString();
    }

    @Override
    public FileLock lockFile(String path) {
        return lockFiles(Collections.singletonList(path));
    }

    @Override
    public FileLock lockFiles(List<String> paths) {
        List<FileChannel> channels = new ArrayList<>();
        List<java.nio.channels.FileLock> locks = new ArrayList<>();

        try {
            // Open channels and acquire locks for all files
            for (String path : paths) {
                File file = new File(getAbsolutePath(path));
                
                // Create parent directories if needed
                File parentDir = file.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Create file if it doesn't exist
                if (!file.exists()) {
                    file.createNewFile();
                }

                // Open channel and acquire lock
                FileChannel channel = FileChannel.open(
                        file.toPath(),
                        StandardOpenOption.READ,
                        StandardOpenOption.WRITE
                );
                channels.add(channel);
                
                java.nio.channels.FileLock lock = channel.tryLock();
                if (lock == null) {
                    // Failed to acquire lock, so release all previous locks
                    releaseChannelsAndLocks(channels, locks);
                    throw new ConcurrencyException("File is locked by another process: " + path);
                }
                
                locks.add(lock);
            }

            return new FileLock(locks, paths);
        } catch (IOException e) {
            releaseChannelsAndLocks(channels, locks);
            throw new StorageException("Failed to lock files", "lock", String.join(", ", paths), e);
        }
    }

    /**
     * Helper method to release channels and locks in case of failure
     */
    private void releaseChannelsAndLocks(List<FileChannel> channels, List<java.nio.channels.FileLock> locks) {
        // Release locks
        for (java.nio.channels.FileLock lock : locks) {
            try {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
            } catch (IOException e) {
                // Log error but continue releasing other resources
                System.err.println("Error releasing lock: " + e.getMessage());
            }
        }
        
        // Close channels
        for (FileChannel channel : channels) {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException e) {
                // Log error but continue releasing other resources
                System.err.println("Error closing channel: " + e.getMessage());
            }
        }
    }

    /**
     * Read a map from a JSON file using an existing file channel (for use with locks)
     */
    public <K, V> Map<K, V> readMapFromJson(FileChannel channel, Class<K> keyType, Class<V> valueType) throws IOException {
        // Read the file content without closing the channel
        ByteBuffer buffer = ByteBuffer.allocate((int)channel.size());
        channel.position(0); // Reset position to beginning of file
        channel.read(buffer);
        buffer.flip();

        String content = StandardCharsets.UTF_8.decode(buffer).toString();

        // If file is empty or contains only whitespace, return empty map
        if (content.trim().isEmpty() || content.trim().equals("{}")) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(content,
                    objectMapper.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON content: " + e.getMessage(), e);
        }
    }

    /**
     * Write a map to a JSON file using an existing file channel (for use with locks)
     */
    public <K, V> void writeMapToJson(FileChannel channel, Map<K, V> data) throws IOException {
        // Convert data to JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);

        // Write to the channel
        ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        channel.position(0); // Reset position to beginning of file
        channel.write(buffer);
        channel.truncate(buffer.capacity()); // Truncate if new content is shorter
    }

    @Override
    public <K, V> void updateMapInJsonFile(String path, BiFunction<Map<K, V>, Object, Map<K, V>> updateFunction,
                                           Class<K> keyType, Class<V> valueType, Object param) {
        try (FileChannel channel = FileChannel.open(Paths.get(getAbsolutePath(path)),
                StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE)) {
            try (java.nio.channels.FileLock lock = channel.lock()) {
                // Read current map
                Map<K, V> currentMap = readMapFromChannel(channel, keyType, valueType);

                // Apply update function to get new map
                Map<K, V> updatedMap = updateFunction.apply(currentMap, param);

                // Write updated map
                writeMapToChannel(channel, updatedMap);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to update map in file", "update", path, e);
        }
    }

    // Private helper methods
    private <K, V> Map<K, V> readMapFromChannel(FileChannel channel, Class<K> keyType, Class<V> valueType) throws IOException {
        // Read file content
        ByteBuffer buffer = ByteBuffer.allocate((int)channel.size());
        channel.position(0);
        channel.read(buffer);
        buffer.flip();

        String content = StandardCharsets.UTF_8.decode(buffer).toString();

        // If file is empty or contains only whitespace, return empty map
        if (content.trim().isEmpty() || content.trim().equals("{}")) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(content,
                    objectMapper.getTypeFactory().constructMapType(Map.class, keyType, valueType));
        } catch (Exception e) {
            throw new IOException("Failed to parse JSON content", e);
        }
    }

    private <K, V> void writeMapToChannel(FileChannel channel, Map<K, V> data) throws IOException {
        // Convert to JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);

        // Write to channel
        ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        channel.position(0);
        channel.write(buffer);
        channel.truncate(buffer.array().length);
    }
} 