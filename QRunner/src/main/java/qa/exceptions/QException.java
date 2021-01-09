package qa.exceptions;

import javax.ws.rs.core.Response;

public class QException extends RuntimeException {
    public final Response.Status status;

    public QException(Response.Status status, String errorMessage) {
        super(errorMessage);
        this.status = status;
    }

    public QException(Response.Status status, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.status = status;
    }
}
