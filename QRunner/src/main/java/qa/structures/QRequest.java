package qa.structures;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

public class QRequest {
    @JsonProperty
    @NotEmpty
    private String instanceId;
    @JsonProperty
    @NotEmpty
    private String method;
    @JsonProperty
    private Map<String, Object> commandParameters;

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getCommandParameters() {
        return this.commandParameters;
    }

    public void setCommandParameters(Map<String, Object> commandParameters) {
        this.commandParameters = commandParameters;
    }
}
