package com.cstestforge.project.storage;

import java.io.Closeable;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Represents a lock on one or more files.
 * Implements Closeable to support try-with-resources pattern.
 */
public class FileLock implements Closeable {
    private List<java.nio.channels.FileLock> locks;
    private List<String> lockedPaths;

    public FileLock(List<java.nio.channels.FileLock> locks, List<String> lockedPaths) {
        this.locks = locks;
        this.lockedPaths = lockedPaths;
    }

    /**
     * Get the list of locked file paths
     * 
     * @return List of paths that are locked
     */
    public List<String> getLockedPaths() {
        return lockedPaths;
    }

    /**
     * Check if the lock is valid (all locks are still active)
     * 
     * @return true if all locks are valid
     */
    public boolean isValid() {
        if (locks == null || locks.isEmpty()) {
            return false;
        }
        
        for (java.nio.channels.FileLock lock : locks) {
            if (lock == null || !lock.isValid()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Release all locks
     */
    @Override
    public void close() {
        if (locks != null) {
            for (java.nio.channels.FileLock lock : locks) {
                try {
                    if (lock != null && lock.isValid()) {
                        lock.release();
                    }
                } catch (Exception e) {
                    // Just log the error but continue releasing other locks
                    System.err.println("Error releasing lock: " + e.getMessage());
                }
            }
            locks.clear();
        }
    }
} 