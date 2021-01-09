package qa.structures;

import java.util.Map;

public abstract class ObjectFactory {
    protected Map<String, Map<String, Class<?>>> supportedObjects;

    public abstract Object construct(String objectId, Map<String, Object> parameters);

    public boolean isSupported(String objectId, Map<String, Object> parameters) {
        if (!this.supportedObjects.containsKey(objectId)) {
            return false;
        }
        Map<String, Class<?>> fieldNameToClass = this.supportedObjects.get(objectId);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!fieldNameToClass.containsKey(entry.getKey())) {
                return false;
            }
            if (fieldNameToClass.get(entry.getKey()) != entry.getValue().getClass()) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Map<String, Class<?>>> getSupportedObjectDescriptions() {
        return this.supportedObjects;
    }

    public String getId() {
        return this.getClass().getSimpleName();
    }
}
