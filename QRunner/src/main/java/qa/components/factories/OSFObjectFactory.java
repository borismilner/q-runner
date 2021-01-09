package qa.components.factories;

import com.fasterxml.jackson.core.type.TypeReference;
import osf.modules.elm.Ellipse;
import osf.modules.elm.OSFEntity;
import osf.modules.elm.Point;
import osf.modules.elm.structures.EntityReference;
import osf.objects.structures.SystemId;
import qa.exceptions.QException;
import qa.structures.ObjectFactory;
import qa.structures.QModule;
import qa.structures.Shared;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class OSFObjectFactory extends ObjectFactory {

    private static final String valueField = "value";
    private final String latitude = "latitude";
    private final String longitude = "longitude";
    private final String altitude = "altitude";
    private final String radius1 = "radius1";
    private final String radius2 = "radius2";

    public OSFObjectFactory() {
        this.supportedObjects = Map.of(
                "EntityReference", Map.of(
                        "systemIdMemoryKey", String.class,
                        "minVersion", String.class,
                        "maxVersion", String.class
                ),
                "SystemId", Map.of(OSFObjectFactory.valueField, String.class),
                "Point", Map.of(
                        this.latitude, String.class,
                        this.longitude, String.class,
                        this.altitude, String.class
                ),
                "Ellipse", Map.of(
                        this.latitude, String.class,
                        this.longitude, String.class,
                        this.altitude, String.class,
                        this.radius1, String.class,
                        this.radius2, String.class
                ),
                "ListOfPoints", Map.of(OSFObjectFactory.valueField, String.class)
        );
    }

    @Override
    public Object construct(String objectId, Map<String, Object> parameters) {
        String systemIdMemoryKey = (String) parameters.get("systemIdMemoryKey");
        switch (objectId) {
            case "EntityReference" -> {
                this.validateMemoryKey(systemIdMemoryKey);
                OSFEntity entity = (OSFEntity) QModule.getMemory().get(systemIdMemoryKey);
                SystemId systemId = entity.getSystemId();
                return new EntityReference(
                        systemId,
                        entity.getClass(),
                        Integer.parseInt((String) parameters.get("minVersion")),
                        Integer.parseInt((String) parameters.get("maxVersion"))
                );
            }
            case "SystemId" -> {
                this.validateMemoryKey(systemIdMemoryKey);
                OSFEntity sourceEntity = (OSFEntity) QModule.getMemory().get(systemIdMemoryKey);
                return sourceEntity.getSystemId();
            }
            case "Point" -> {
                Point point = new Point();
                point.setLatitude(Double.parseDouble((String) parameters.get(this.latitude)));
                point.setLongitude(Double.parseDouble((String) parameters.get(this.longitude)));
                point.setAltitude(Double.parseDouble((String) parameters.get(this.altitude)));
                return point;
            }
            case "Ellipse" -> {
                Ellipse ellipse = new Ellipse();
                Point center = new Point();
                center.setLatitude(Double.parseDouble((String) parameters.get(this.latitude)));
                center.setLongitude(Double.parseDouble((String) parameters.get(this.longitude)));
                center.setAltitude(Double.parseDouble((String) parameters.get(this.altitude)));
                ellipse.setCenter(center);
                ellipse.setRadius1(Double.parseDouble((String) parameters.get(this.radius1)));
                ellipse.setRadius2(Double.parseDouble((String) parameters.get(this.radius2)));
                return ellipse;
            }
            case "ListOfPoints" -> {
                TypeReference<List<Point>> listOfPointsType = new TypeReference<>() {
                };
                return Shared.fromJSON(listOfPointsType, (String) parameters.get(OSFObjectFactory.valueField));
            }
            default -> throw new QException(Response.Status.NOT_ACCEPTABLE, String.format("No factory logic for: %s", objectId));
        }
    }

    private void validateMemoryKey(String objectMemoryKey) {
        if (!QModule.getMemory().containsKey(objectMemoryKey)) {
            throw new QException(Response.Status.NO_CONTENT, String.format("No object stored under key: %s", objectMemoryKey));
        }
    }
}
