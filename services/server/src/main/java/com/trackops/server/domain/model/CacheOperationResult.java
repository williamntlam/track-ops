package com.trackops.server.domain.model;

/**
 * Represents the result of a cache operation, indicating success or failure
 * along with an optional error message.
 */
public class CacheOperationResult {
    
    private final boolean success;
    private final String errorMessage;
    
    private CacheOperationResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Creates a successful cache operation result.
     * 
     * @return a successful CacheOperationResult
     */
    public static CacheOperationResult success() {
        return new CacheOperationResult(true, null);
    }
    
    /**
     * Creates a failed cache operation result with an error message.
     * 
     * @param errorMessage the error message describing why the operation failed
     * @return a failed CacheOperationResult
     */
    public static CacheOperationResult failure(String errorMessage) {
        return new CacheOperationResult(false, errorMessage);
    }
    
    /**
     * Returns whether the cache operation was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Returns whether the cache operation failed.
     * 
     * @return true if failed, false otherwise
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * Returns the error message if the operation failed.
     * 
     * @return the error message, or null if the operation was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "CacheOperationResult{success=true}";
        } else {
            return "CacheOperationResult{success=false, errorMessage='" + errorMessage + "'}";
        }
    }
}
