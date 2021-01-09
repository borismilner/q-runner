package qa.components.factories;

import qa.exceptions.QException;
import qa.structures.ObjectFactory;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

public class BasicObjectFactory extends ObjectFactory {
    private final static String valueField = "value";

    public BasicObjectFactory() {
        this.supportedObjects = Map.of(
                "UUID", Map.of(BasicObjectFactory.valueField, String.class),
                "String", Map.of(BasicObjectFactory.valueField, String.class),
                "Integer", Map.of(BasicObjectFactory.valueField, String.class),
                "Double", Map.of(BasicObjectFactory.valueField, String.class)
        );
    }

    @Override
    public Object construct(String objectId, Map<String, Object> parameters) {
        switch (objectId) {
            case "UUID":
                return UUID.fromString((String) parameters.get(BasicObjectFactory.valueField));
            case "String":
                return parameters.get(BasicObjectFactory.valueField);
            case "Integer":
                return Integer.valueOf((String) parameters.get(BasicObjectFactory.valueField));
            case "Double":
                return Double.valueOf((String) parameters.get(BasicObjectFactory.valueField));
            default:
                throw new QException(Response.Status.NOT_ACCEPTABLE, String.format("No factory logic for: %s", objectId));
        }
    }
}
