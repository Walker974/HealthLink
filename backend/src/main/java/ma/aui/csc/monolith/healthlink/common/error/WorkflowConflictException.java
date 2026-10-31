package ma.aui.csc.monolith.healthlink.common.error;

public class WorkflowConflictException extends RuntimeException {
    public WorkflowConflictException(String message) {
        super(message);
    }
}
