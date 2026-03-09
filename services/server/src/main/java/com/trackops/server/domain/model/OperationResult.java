package com.trackops.server.domain.model;

/**
 * Represents the result of any operation, indicating success or failure
 * along with an optional error message.
 */
public class OperationResult {
    
    private final boolean success;
    private final String errorMessage;
    
    private OperationResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Creates a successful operation result.
     * 
     * @return a successful OperationResult
     */
    public static OperationResult success() {
        return new OperationResult(true, null);
    }
    
    /**
     * Creates a failed operation result with an error message.
     * 
     * @param errorMessage the error message describing why the operation failed
     * @return a failed OperationResult
     */
    public static OperationResult failure(String errorMessage) {
        return new OperationResult(false, errorMessage);
    }
    
    /**
     * Returns whether the operation was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Returns whether the operation failed.
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
            return "OperationResult{success=true}";
        } else {
            return "OperationResult{success=false, errorMessage='" + errorMessage + "'}";
        }
    }
}
