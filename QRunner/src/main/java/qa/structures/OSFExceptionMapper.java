package qa.structures;

import osf.shared.exceptions.OsfException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

@Provider
public class OSFExceptionMapper implements ExceptionMapper<OsfException> {
    @Override
    public Response toResponse(OsfException osfException) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        osfException.printStackTrace(printWriter);
        Shared.log.error(osfException.getMessage(), osfException);
        return Response.status(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                stringWriter.toString()).build();
    }
}
