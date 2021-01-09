package qa.structures;

import java.util.List;

public class QGatewayRequest {
    private String applicationId;

    private List<QRequest> requests;

    public String getApplicationId() {
        return this.applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public List<QRequest> getRequests() {
        return this.requests;
    }

    public void setRequests(List<QRequest> requests) {
        this.requests = requests;
    }
}
