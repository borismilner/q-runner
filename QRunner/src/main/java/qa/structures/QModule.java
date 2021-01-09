package qa.structures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qa.exceptions.QException;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

public class QModule {

    protected final static Set<ObjectFactory> objectFactories = new HashSet<>();
    private final static Map<String, Object> memory = new HashMap<>();
    protected final String instanceId;
    protected final String moduleId;
    protected Logger log;

    public QModule(String instanceId, String moduleId) {
        this.instanceId = instanceId;
        this.moduleId = moduleId;
        this.log = LoggerFactory.getLogger(moduleId);
    }

    public static boolean registerObjectFactory(ObjectFactory factory) {
        return QModule.objectFactories.add(factory);
    }

    public static boolean unregisterObjectFactory(String id) {
        return QModule.objectFactories.removeIf(factory -> factory.getId().equals(id));
    }

    public static Map<String, Object> getMemory() {
        return QModule.memory;
    }

    /**
     * Create an object and store it under provided memory key
     *
     * @param objectId    Identifies the required object
     * @param parameters  Map of parameters for the request
     * @param toMemoryKey Memory key under which the created object is to be saved
     * @return Response
     */
    public QResponse createObject(String objectId, Map<String, Object> parameters, String toMemoryKey) {

        if (parameters.containsKey("objectClass") && parameters.containsKey("objectJson")) {
            String objectClass = (String) parameters.get("objectClass");
            String objectJson = (String) parameters.get("objectJson");
            Class<?> clazz = Shared.classFromName(objectClass);
            try {
                Object object = Shared.objectMapper.readValue(objectJson, clazz);
                return new QResponse(
                        this.moduleId,
                        this.storeInMemory(object, toMemoryKey)
                );
            } catch (JsonProcessingException exception) {
                throw new QException(Response.Status.BAD_REQUEST, String.format("Failed working with json:%n%s", objectJson), exception);
            }
        }

        for (ObjectFactory factory : QModule.objectFactories) {
            if (factory.isSupported(objectId, parameters)) {
                Object object = factory.construct(objectId, parameters);
                return new QResponse(
                        this.moduleId,
                        this.storeInMemory(object, toMemoryKey)
                );
            }
        }
        throw new QException(Response.Status.NOT_IMPLEMENTED, String.format("Failed to find a factory for object %s with parameters %s", objectId, parameters));
    }

    /**
     * Create an object of the provided class by passing it's constructor objects from memory
     *
     * @param objectClass           The class of the created object
     * @param toMemoryKey           The memory key the object to be stored under
     * @param constructorMemoryKeys The memory keys of the objects to pass into the required class-constructor
     * @return Response
     */
    public QResponse createObject(@NotEmpty String objectClass, @NotEmpty String toMemoryKey, String... constructorMemoryKeys) {
        List<Object> constructorParameters = this.getFromMemory(constructorMemoryKeys);
        Class<?> clazz = Shared.classFromName(objectClass);
        Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : allConstructors) {
            try {
                Object object = constructor.newInstance(constructorParameters);
                return new QResponse(
                        this.moduleId,
                        this.storeInMemory(object, toMemoryKey)
                );
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException ignored) {
                // Ignore
            }
        }
        throw new QException(Response.Status.EXPECTATION_FAILED, String.format("Failed to construct object of class: %s with provided constructor parameters", objectClass));
    }

    public Response runCommand(QRequest qRequest) {
        String requestMethod = qRequest.getMethodName();

        Set<Method> allMethods = new HashSet<>();
        allMethods.addAll(Arrays.asList(this.getClass().getDeclaredMethods().clone()));
        allMethods.addAll(Arrays.asList(this.getClass().getMethods().clone()));
        allMethods.removeIf(method -> !method.getName().equals(requestMethod));
        for (Method method : allMethods) {
            Parameter[] requiredParameters = method.getParameters();
            List<Object> providedParameters = new ArrayList<>();
            Map<String, Object> commandParameters = qRequest.getParameters();
            for (Parameter parameter : requiredParameters) {
                String parameterName = parameter.getName();
                if (!commandParameters.containsKey(parameterName)) {
                    providedParameters.clear();
                    break;
                } else {
                    providedParameters.add(commandParameters.get(parameterName));
                }
            }
            if (providedParameters.size() != requiredParameters.length) {
                continue;
            }
            try {
                method.setAccessible(true);
                Object reply = method.invoke(this, providedParameters.toArray());
                QResponse qResponse = (QResponse) reply;
                return Response.status(qResponse.getStatus()).entity(qResponse).build();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new QException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        }
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                String.format("Instance %s contains %d methods called %s but none receive the parameters : %s",
                        this.instanceId, allMethods.size(), requestMethod, qRequest.getParameters())).build();
    }

    /**
     * View a JSON serialization of the object stored under the provided key in memory
     *
     * @param fromMemoryKey The key in memory to view
     * @return Response
     */
    public QResponse viewMemory(@NotEmpty String fromMemoryKey) {
        this.validateMemoryKey(fromMemoryKey);
        return new QResponse(
                this.moduleId,
                Map.of(fromMemoryKey, this.getFromMemory(fromMemoryKey))
        );
    }

    /**
     * Empty internal memory
     *
     * @return Response
     */
    public QResponse clearMemory() {
        QModule.memory.clear();
        return new QResponse(
                this.moduleId,
                Map.of("cleared", true)
        );
    }

    /**
     * Set the value of a specified field of the specified object with a value taken from a specified memory key
     *
     * @param targetMemoryKey The object to edit
     * @param fieldName       The field name
     * @param fromMemoryKey   The key in memory to take the value from
     * @return Response
     */
    public QResponse editObject(@NotEmpty String targetMemoryKey, @NotEmpty String fieldName, @NotEmpty String fromMemoryKey) throws NoSuchFieldException, IllegalAccessException {
        this.validateMemoryKey(fromMemoryKey);
        Object target = QModule.memory.get(targetMemoryKey);
        Object source = QModule.memory.get(fromMemoryKey);
        Field theField = Shared.getField(target.getClass(), fieldName);
        theField.set(target, source);
        return new QResponse(
                this.moduleId,
                Map.of("field_set", fieldName, "value", source, "updated_target", target)
        );
    }

    public QResponse ping() {
        return new QResponse(
                this.moduleId,
                Map.of("ping-response", "pong")
        );
    }

    //region Helpers
    protected void validateMemoryKey(String objectMemoryKey) {
        if (!QModule.memory.containsKey(objectMemoryKey)) {
            throw new QException(Response.Status.NO_CONTENT, String.format("No object stored under key: %s", objectMemoryKey));
        }
    }

    public Map<String, Object> storeInMemory(Object object, String toMemoryKey) {
        Object previousObject = QModule.memory.put(toMemoryKey, object);
        if (previousObject != null) {
            this.log.warn(String.format("Overwritten previously existing object under key: %s", toMemoryKey));
        }
        return Map.of("stored", toMemoryKey, "overwritten", previousObject != null);
    }

    /**
     * Get a list of several objects from memory
     *
     * @param fromKeys an array of the keys for the required objects
     * @return A list of the required objects
     */
    protected List<Object> getFromMemory(String... fromKeys) {
        List<Object> objects = new ArrayList<>();
        for (String memoryKey : fromKeys) {
            this.validateMemoryKey(memoryKey);
            objects.add(QModule.memory.get(memoryKey));
        }
        return objects;
    }

    /**
     * Get an object from memory
     *
     * @param fromKey The key under which the objet is stored
     * @return The required object
     */
    protected Object getFromMemory(@NotEmpty String fromKey) {
        this.validateMemoryKey(fromKey);
        return QModule.memory.get(fromKey);
    }

    public QResponse getCommands() throws IOException {
        ClassPath classPath = ClassPath.from(this.getClass().getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> qaClassInfos = classPath.getTopLevelClassesRecursive("qa");

        QResponse response = new QResponse(this.moduleId, new HashMap<>());
        Map<String, Object> responseMap = response.getResponse();

        for (ClassPath.ClassInfo info : qaClassInfos) {
            Class<?> clazz = info.load();
            String className = info.getName();

            List<MethodDescription> qaMethods = new ArrayList<>();

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (!QResponse.class.isAssignableFrom(method.getReturnType())) {
                    continue;
                }

                MethodDescription methodDescription = new MethodDescription(method.getName());
                List<ParameterDescription> params = methodDescription.getParameters();

                Parameter[] parameters = method.getParameters();
                for (Parameter parameter : parameters) {
                    params.add(new ParameterDescription(
                            parameter.getName(),
                            parameter.getType().getSimpleName()));
                }
                qaMethods.add(methodDescription);
            }
            if (!qaMethods.isEmpty()) {
                qaMethods.sort(Comparator.comparing(MethodDescription::getMethodName));
                responseMap.put(className, qaMethods);
            }
        }
        return response;
    }
    //endregion
}
