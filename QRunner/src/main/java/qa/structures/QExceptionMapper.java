package qa.structures;

import qa.exceptions.QException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.PrintWriter;
import java.io.StringWriter;

public class QExceptionMapper implements ExceptionMapper<QException> {
    @Override
    public Response toResponse(QException qException) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        qException.printStackTrace(printWriter);
        Shared.log.error(qException.getMessage(), qException);
        return Response.status(
                qException.status.getStatusCode(),
                stringWriter.toString()).build();
    }
}
