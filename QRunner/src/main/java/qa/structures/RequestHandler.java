package qa.structures;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;


@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class RequestHandler {

    private final Map<String, QModule> mapIdToBusinessLogic = new HashMap<>();
    private final List<QResponse> aggregatedOutputs = new ArrayList<>();
    private final Map<String, List<QRequest>> mapAppNameToGatewayCommands = new HashMap<>();

    public void registerQModule(String serviceId, QModule qModule) {
        this.mapIdToBusinessLogic.put(serviceId, qModule);
    }

    @POST
    @Path("/runCommand")
    public Response runCommand(QRequest qRequest) {
        String instanceId = qRequest.getInstanceId();
        QModule qModule = this.mapIdToBusinessLogic.get(instanceId);
        if (qModule == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(String.format("Module does not exist: %s", instanceId)).build();
        }
        Response response = qModule.runCommand(qRequest);
        Response reply = this.transmitOutput((QResponse) response.getEntity());
        if (reply.getStatus() != Response.Status.OK.getStatusCode()) {
            QResponse responseEntity = (QResponse) response.getEntity();
            responseEntity.getResponse().put("SIMULATOR-ERROR", "Failed pushing response to aggregator");
        }
        return response;
    }

    @POST
    @Path("/pushResponse")
    public Response pushResponse(QResponse response) {
        this.aggregatedOutputs.add(response);
        return Response.ok(response).build();
    }

    @POST
    @Path("/fetchResponses")
    public Response fetchResponses() {
        this.aggregatedOutputs.sort(Comparator.comparing(QResponse::getTimestamp));
        return Response.ok(this.aggregatedOutputs).build();
    }

    @POST
    @Path("/clearResponses")
    public Response clearResponses() {
        int beforeClearing = this.aggregatedOutputs.size();
        this.aggregatedOutputs.clear();
        return Response.ok(Map.of("cleared", beforeClearing)).build();
    }

    @POST
    @Path("/storeGatewayCommands")
    public Response storeGatewayCommands(QGatewayRequest gatewayRequest) {
        String applicationId = gatewayRequest.getApplicationId();
        this.mapAppNameToGatewayCommands.computeIfAbsent(applicationId, app -> new ArrayList<>());
        this.mapAppNameToGatewayCommands.get(applicationId).addAll(gatewayRequest.getRequests());
        return Response.ok(Map.of("stored", gatewayRequest)).build();
    }

    @POST
    @Path("/pullGatewayCommands")
    public Response pullGatewayCommands(String applicationId) {
        if (!this.mapAppNameToGatewayCommands.containsKey(applicationId)) {
            return Response.ok(Map.of("commands", List.of())).build();
        }
        List<QRequest> commands = this.mapAppNameToGatewayCommands.get(applicationId);
        return Response.ok(Map.of("commands", commands)).build();
    }

    @POST
    @Path("/clearGatewayCommands")
    public Response clearGatewayCommands(String applicationId) {
        if (!this.mapAppNameToGatewayCommands.containsKey(applicationId)) {
            return Response.status(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    String.format("No stored commands for application: %s", applicationId)).build();
        }
        this.mapAppNameToGatewayCommands.get(applicationId).clear();
        return Response.ok(Map.of("cleared", true)).build();
    }


    private Response transmitOutput(QResponse response) {
        String aggregatorAddress = String.format("%s/pushResponse", Shared.config.getOutputAggregatorAddress());
        WebTarget webTarget = Shared.client.target(aggregatorAddress);
        return webTarget
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(response));
    }

    @GET
    @Path("/getQaCommands")
    @Produces(MediaType.TEXT_HTML)
    public QCommandsView getPerson() {
        return new QCommandsView();
    }

}
