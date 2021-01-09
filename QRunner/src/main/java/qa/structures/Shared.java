package qa.structures;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qa.config.Config;
import qa.exceptions.QException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;

public class Shared {
    public static final Logger log = LoggerFactory.getLogger("QLogger");
    public static Client client;
    public static ObjectMapper objectMapper;
    public static Config config;

    public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field;

        if (clazz == null) {
            throw new NoSuchFieldException(String.format("Field %s not found.", fieldName));
        }

        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException noSuchFieldException) {
            return Shared.getField(clazz.getSuperclass(), fieldName);
        }

        return field;
    }

    /**
     * Deserialize provided JSON string into a specific type-reference, for example a List<Point>
     *
     * @param type       The TypeReference to assign the json string deserialization
     * @param jsonPacket JSON string representation of the provided type item/s
     * @param <T>        The type of the TypeReference
     * @return An instance of the required type
     */
    public static <T> T fromJSON(TypeReference<T> type, String jsonPacket) {
        T data;

        try {
            data = Shared.objectMapper.readValue(jsonPacket, type);
        } catch (Exception exception) {
            throw new QException(Response.Status.INTERNAL_SERVER_ERROR, String.format("Failed deserializing: %s", exception.getMessage()), exception);
        }
        return data;
    }

    public static Class<?> classFromName(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException exception) {
            throw new QException(Response.Status.EXPECTATION_FAILED, String.format("Failed finding a java class: %s", className), exception);
        }
    }
}
