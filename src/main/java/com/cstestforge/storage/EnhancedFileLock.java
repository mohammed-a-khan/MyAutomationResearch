package com.cstestforge.storage;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Enhanced file locking mechanism with timeout, retry logic, and deadlock detection.
 */
public class EnhancedFileLock implements AutoCloseable {
    private static final Map<String, String> ACTIVE_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Thread> LOCK_OWNERS = new ConcurrentHashMap<>();
    private static final ReentrantLock GLOBAL_LOCK = new ReentrantLock();
    
    private final File file;
    private final String lockId;
    private final long timeoutMs;
    private RandomAccessFile randomAccessFile;
    private FileChannel channel;
    private FileLock lock;
    private final AtomicBoolean released = new AtomicBoolean(false);
    private final String threadId = UUID.randomUUID().toString();
    
    /**
     * Constructor for EnhancedFileLock with configurable timeout
     * 
     * @param file File to lock
     * @param timeoutMs Timeout in milliseconds
     */
    public EnhancedFileLock(File file, long timeoutMs) {
        this.file = file;
        this.lockId = file.getAbsolutePath();
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Constructor with default timeout of 30 seconds
     * 
     * @param file File to lock
     */
    public EnhancedFileLock(File file) {
        this(file, TimeUnit.SECONDS.toMillis(30));
    }
    
    /**
     * Acquires the lock with retry logic and deadlock detection
     * 
     * @return true if lock acquired successfully
     * @throws Exception if lock acquisition fails
     */
    public boolean acquire() throws Exception {
        if (released.get()) {
            throw new IllegalStateException("Lock has been released and cannot be reacquired");
        }
        
        // Check for potential deadlock
        checkForDeadlock();
        
        // Try to acquire the global synchronization lock
        if (!GLOBAL_LOCK.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Timed out acquiring global lock");
        }
        
        try {
            // Check if this thread already owns the lock (reentrant)
            String currentOwner = ACTIVE_LOCKS.get(lockId);
            if (currentOwner != null && currentOwner.equals(threadId)) {
                // Already own this lock, just return
                return true;
            }
            
            // Check if another thread holds the lock
            if (currentOwner != null) {
                throw new LockConflictException("Lock already held by another thread");
            }
            
            // Try to acquire with retries
            return acquireWithRetry();
        } finally {
            GLOBAL_LOCK.unlock();
        }
    }
    
    /**
     * Checks for potential deadlock scenarios
     */
    private void checkForDeadlock() {
        Thread currentThread = Thread.currentThread();
        
        // Simple deadlock detection - if this thread already holds locks
        // and is trying to acquire a lock held by another thread holding locks by this thread
        for (Map.Entry<String, Thread> entry : LOCK_OWNERS.entrySet()) {
            if (entry.getValue().equals(currentThread)) {
                String otherLockId = entry.getKey();
                String otherLockOwner = ACTIVE_LOCKS.get(otherLockId);
                
                if (otherLockOwner != null && !otherLockOwner.equals(threadId)) {
                    Thread otherThread = LOCK_OWNERS.get(otherLockId);
                    if (otherThread != null) {
                        // Potential deadlock detected
                        throw new PotentialDeadlockException(
                                "Potential deadlock detected: Thread " + currentThread.getName() +
                                " is attempting to lock " + lockId + " while holding " + otherLockId +
                                " which is wanted by thread " + otherThread.getName());
                    }
                }
            }
        }
    }
    
    /**
     * Attempts to acquire the file lock with retry logic
     */
    private boolean acquireWithRetry() throws Exception {
        Instant startTime = Instant.now();
        int attempts = 0;
        long backoffMs = 50;  // Start with 50ms backoff
        
        while (Duration.between(startTime, Instant.now()).toMillis() < timeoutMs) {
            attempts++;
            
            try {
                // Create parent directory if it doesn't exist
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Create lock file if it doesn't exist
                if (!file.exists()) {
                    file.createNewFile();
                }
                
                randomAccessFile = new RandomAccessFile(file, "rw");
                channel = randomAccessFile.getChannel();
                lock = channel.tryLock();
                
                if (lock != null) {
                    // Lock acquired successfully
                    ACTIVE_LOCKS.put(lockId, threadId);
                    LOCK_OWNERS.put(lockId, Thread.currentThread());
                    return true;
                }
            } catch (Exception e) {
                // Close resources if there was an exception
                closeResources();
                
                // If we've reached timeout, throw the exception
                if (Duration.between(startTime, Instant.now()).toMillis() >= timeoutMs) {
                    throw e;
                }
            }
            
            // Exponential backoff with a cap
            Thread.sleep(Math.min(backoffMs, 1000));
            backoffMs *= 1.5;
        }
        
        throw new TimeoutException("Timed out acquiring lock for " + file.getAbsolutePath() + " after " + attempts + " attempts");
    }
    
    /**
     * Releases the lock and cleans up resources
     */
    public void release() {
        if (released.compareAndSet(false, true)) {
            closeResources();
            ACTIVE_LOCKS.remove(lockId);
            LOCK_OWNERS.remove(lockId);
        }
    }
    
    /**
     * Closes all file resources
     */
    private void closeResources() {
        try {
            if (lock != null) {
                lock.release();
                lock = null;
            }
        } catch (Exception e) {
            // Log but continue cleanup
        }
        
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (Exception e) {
            // Log but continue cleanup
        }
        
        try {
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
        } catch (Exception e) {
            // Log but continue cleanup
        }
    }
    
    /**
     * For use with try-with-resources
     */
    @Override
    public void close() {
        release();
    }
    
    /**
     * Exception thrown when lock acquisition times out
     */
    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when a lock conflict is detected
     */
    public static class LockConflictException extends RuntimeException {
        public LockConflictException(String message) {
            super(message);
        }
    }
    
    /**
     * Exception thrown when a potential deadlock is detected
     */
    public static class PotentialDeadlockException extends RuntimeException {
        public PotentialDeadlockException(String message) {
            super(message);
        }
    }
} 