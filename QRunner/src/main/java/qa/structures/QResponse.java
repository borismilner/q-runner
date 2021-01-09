package qa.structures;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class QResponse {

    DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private String moduleId;
    private Map<String, Object> response;
    private String timestamp;

    private Response.Status status;

    public QResponse(String moduleId, Map<String, Object> response, Response.Status status) {
        this.moduleId = moduleId;
        this.timestamp = this.formatter.format(Instant.now());
        this.response = response;
        this.status = status;
    }

    public QResponse(String moduleId, Map<String, Object> response) {
        this(moduleId, response, Response.Status.OK);
    }

    public QResponse() {

    }

    public String getModuleId() {
        return this.moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getResponse() {
        return this.response;
    }

    public Response.Status getStatus() {
        return this.status;
    }

}
