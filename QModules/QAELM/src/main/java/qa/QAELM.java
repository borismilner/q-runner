package qa;

import com.google.common.reflect.ClassPath;
import osf.modules.elm.ELM;
import osf.modules.elm.OSFEntity;
import osf.modules.elm.OSFImmutableEntity;
import osf.modules.elm.OSFWritableEntity;
import osf.modules.elm.cache.OSFPackage;
import osf.modules.elm.structures.EntityReference;
import osf.objects.BaseOsfObject;
import osf.objects.enums.ClassMD;
import osf.objects.structures.OSFAttribute;
import osf.objects.structures.SystemId;
import osf.shared.annotations.InjectService;
import osf.shared.exceptions.OsfException;
import qa.structures.QModule;
import qa.structures.QResponse;
import qa.structures.Shared;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@InjectService
public class QAELM extends QModule {
    private final ELM elm;

    public QAELM(String serviceId, String moduleId, ELM elm) {
        super(serviceId, moduleId);
        this.elm = elm;
    }

    public QResponse getAllTestClasses() {

        String packageName = "osf.modules.elm";

        List<String> nonTestClasses = Arrays.asList(
                "osf.modules.elm.OSFWritableEntity",
                "osf.modules.elm.OSFImmutableEntity",
                "osf.modules.elm.Point",
                "osf.modules.elm.Ellipse"
        );

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        List<String> testClasses = new ArrayList<>();

        try {
            for (ClassPath.ClassInfo info : ClassPath.from(loader).getTopLevelClasses()) {
                if (info.getName().startsWith(packageName)) {
                    Class<?> clazz = info.load();
                    if (OSFWritableEntity.class.isAssignableFrom(clazz) || OSFImmutableEntity.class.isAssignableFrom(clazz)) {
                        if (nonTestClasses.contains(clazz.getName())) {
                            continue;
                        }
                        testClasses.add(clazz.getName());
                    }
                }
            }
            testClasses.sort(String::compareTo);
            return new QResponse(
                    this.moduleId,
                    Map.of("utel-classes", testClasses)
            );
        } catch (IOException exception) {
            String errorMessage = String.format("Failed while reflecting available test-classes in package: %s", packageName);
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }

    public QResponse changeElmOwnerName(@NotEmpty String ownerName) throws NoSuchFieldException {
        Field serviceIdField = Shared.getField(ELM.class, "serviceId");
        try {
            serviceIdField.set(this.elm, ownerName);
            return new QResponse(
                    this.moduleId,
                    Map.of("you_became", ownerName)
            );
        } catch (IllegalAccessException illegalAccessException) {
            String errorMessage = String.format("Failed setting serviceId in elm: %s", illegalAccessException.getMessage());
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }

    public QResponse getWritableFor(@NotEmpty String immutableTypeName, @NotEmpty String toMemoryKey) {

        List<String> availableTestClasses = (List<String>) this.getAllTestClasses().getResponse().get("utel-classes");
        if (!availableTestClasses.contains(immutableTypeName)) {
            String errorMessage = String.format("%s is not a recognized class. choose one from: %s", immutableTypeName, String.join(",", availableTestClasses));
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.BAD_REQUEST
            );
        }

        Class<? extends OSFImmutableEntity> theClass;
        try {
            theClass = (Class<? extends OSFImmutableEntity>) Class.forName(immutableTypeName);
        } catch (ClassNotFoundException classNotFoundException) {
            String errorMessage = classNotFoundException.getMessage();
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.BAD_REQUEST
            );
        }
        OSFWritableEntity writableEntity = this.elm.getWritableFor(theClass);
        BaseOsfObject previous = (BaseOsfObject) QModule.getMemory().put(toMemoryKey, writableEntity);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "created_in", toMemoryKey,
                        "is_overwritten", previous != null,
                        "writable_entity_view", writableEntity)
        );

    }

    public QResponse viewMetadata(@NotEmpty String fromMemoryKey) {
        this.validateMemoryKey(fromMemoryKey);
        Object entity = QModule.getMemory().get(fromMemoryKey);
        if (!OSFEntity.class.isAssignableFrom(entity.getClass())) {
            String errorMessage = String.format("The object at %s is not an entity, it is: %s", fromMemoryKey, entity.getClass().getName());
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.BAD_REQUEST
            );
        }

        OSFEntity theEntity = (OSFEntity) entity;
        Map<ClassMD, Object> metadata = new HashMap<>();
        for (ClassMD classMD : ClassMD.values()) {
            OSFAttribute osfAttribute = null;
            try {
                osfAttribute = theEntity.getClassMetadata(classMD);
            } catch (OsfException exception) {
                // Do nothing - means the entity has no such meta-data
            }
            if (osfAttribute == null) {
                continue;
            }
            metadata.put(classMD, Objects.requireNonNullElse(osfAttribute.value, "not-set"));
        }
        return new QResponse(
                this.moduleId,
                Map.of(
                        "fromMemoryKey", fromMemoryKey,
                        "metadata", metadata
                )
        );
    }

    public QResponse getWritableFromImmutable(@NotEmpty String fromMemoryKey, @NotEmpty String toMemoryKey) {
        this.validateMemoryKey(fromMemoryKey);
        Object immutable = QModule.getMemory().get(fromMemoryKey);
        if (!OSFImmutableEntity.class.isAssignableFrom(immutable.getClass())) {
            String errorMessage = String.format("The object at %s is not an immutable entity, it is: %s", fromMemoryKey, immutable.getClass().getName());
            return new QResponse(
                    this.moduleId,
                    Map.of("error_message", errorMessage),
                    Response.Status.BAD_REQUEST
            );
        }
        OSFWritableEntity writable = this.elm.getWritableFor((OSFImmutableEntity) immutable);
        return new QResponse(
                this.moduleId,
                this.storeInMemory(writable, toMemoryKey)
        );
    }

    public QResponse submitWritable(@NotEmpty String fromMemoryKey, @NotEmpty String toMemoryKey) {
        this.validateMemoryKey(fromMemoryKey);
        Object writable = QModule.getMemory().get(fromMemoryKey);
        OSFImmutableEntity submittedEntity = this.elm.submit((OSFWritableEntity) writable, "QA-Operator", "QA-Role", "QA-WorkGroup");
        return new QResponse(
                this.moduleId,
                this.storeInMemory(submittedEntity, toMemoryKey)
        );
    }

    public QResponse getSpecific(@NotEmpty String systemIdMemoryKey, @Positive int version) {
        this.validateMemoryKey(systemIdMemoryKey);
        OSFImmutableEntity immutableEntity = this.elm.getSpecific((SystemId) QModule.getMemory().get(systemIdMemoryKey), version);
        return new QResponse(
                this.moduleId,
                Map.of("immutableEntity", immutableEntity)
        );
    }

    public QResponse getAllVersions(@NotEmpty String systemIdMemoryKey) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage allVersions = this.elm.getAllVersions(systemId);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "system_id", systemId,
                        "type", allVersions.iterator().next().getClass().getName(),
                        "all_versions", allVersions.getAll()
                ));
    }

    public QResponse exists(@NotEmpty String entityReferenceMemoryKey) {
        this.validateMemoryKey(entityReferenceMemoryKey);
        EntityReference entityReference = (EntityReference) QModule.getMemory().get(entityReferenceMemoryKey);
        boolean exists = this.elm.exists(entityReference);
        return new QResponse(
                this.moduleId,
                Map.of("exists", exists)
        );
    }

    public QResponse exists(@NotEmpty String systemIdMemoryKey, @Positive int version) {
        this.validateMemoryKey(systemIdMemoryKey);
        boolean exists = this.elm.exists((SystemId) QModule.getMemory().get(systemIdMemoryKey), version);
        return new QResponse(
                this.moduleId,
                Map.of("exists", exists)
        );
    }

    public QResponse getLast(@NotEmpty String entityReferenceMemoryKey) {
        this.validateMemoryKey(entityReferenceMemoryKey);
        EntityReference entityReference = (EntityReference) QModule.getMemory().get(entityReferenceMemoryKey);
        OSFImmutableEntity last = this.elm.getLast(entityReference);
        return new QResponse(
                this.moduleId,
                Map.of("last_entity", last)
        );
    }

    public QResponse getLast(@NotEmpty String systemIdMemoryKey, @Positive int minVersion, @Positive int maxVersion) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFImmutableEntity last = this.elm.getLast(systemId, minVersion, maxVersion);
        return new QResponse(
                this.moduleId,
                Map.of("last_entity", last)
        );
    }

    public QResponse getLast(@NotEmpty String systemIdMemoryKey, @Positive int howMany) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage lastEntities = this.elm.getLast(systemId, howMany);
        return new QResponse(
                this.moduleId,
                Map.of("last_entities", lastEntities.getAll())
        );
    }

    public QResponse getAll() {
        return new QResponse(
                this.moduleId,
                Map.of("all_entities", this.elm.getAll().getAll())
        );
    }

    public QResponse size() {
        return new QResponse(
                this.moduleId,
                Map.of("elm_size", this.elm.size())
        );
    }

    public QResponse suggestGC(boolean isUrgent) {
        this.elm.suggestGC(isUrgent);
        return new QResponse(
                this.moduleId,
                Map.of("suggested", true, "isUrgent", isUrgent)
        );
    }

    public QResponse getSpecificHierarchy(@NotEmpty String systemIdMemoryKey, @Positive int version) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage specificHierarchy = this.elm.getSpecificHierarchy(systemId, version);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "version", version,
                        "hierarchy", specificHierarchy.getAll()
                )
        );

    }

    public QResponse getSpecificHierarchy(@NotEmpty String systemIdMemoryKey, @Positive int version, @Positive int depth) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage specificHierarchy = this.elm.getSpecificHierarchy(systemId, version, depth);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "version", version,
                        "hierarchy", specificHierarchy.getAll()
                )
        );
    }

    public QResponse getLastHierarchy(@NotEmpty String systemIdMemoryKey) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage lastHierarchy = this.elm.getLastHierarchy(systemId);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "lastHierarchy", lastHierarchy.getAll())
        );
    }

    public QResponse getLastHierarchy(@NotEmpty String systemIdMemoryKey, @Positive int depth) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage lastHierarchy = this.elm.getLastHierarchy(systemId, depth);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "max-depth", depth,
                        "lastHierarchy", lastHierarchy.getAll())
        );
    }

    public QResponse getLatestValidHierarchy(@NotEmpty String systemIdMemoryKey) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage latestValidHierarchy = this.elm.getLatestValidHierarchy(systemId);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "latestValidHierarchy", latestValidHierarchy.getAll())
        );
    }

    public QResponse getLatestValidHierarchy(@NotEmpty String systemIdMemoryKey, @Positive int depth) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage latestValidHierarchy = this.elm.getLatestValidHierarchy(systemId, depth);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "max-depth", depth,
                        "latestValidHierarchy", latestValidHierarchy.getAll()
                )
        );
    }

    public QResponse getSpecificParents(@NotEmpty String systemIdMemoryKey, @Positive int version) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        OSFPackage specificParents = this.elm.getSpecificParents(systemId, version);
        return new QResponse(
                this.moduleId,
                Map.of("parents", specificParents.getAll())
        );
    }

    public QResponse get(@NotEmpty String entityClassString, @NotEmpty String userQuery) throws ClassNotFoundException {
        Class<? extends OSFImmutableEntity> entityClass = (Class<? extends OSFImmutableEntity>) Class.forName(entityClassString);
        OSFPackage queryResults = this.elm.get(entityClass, userQuery);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "query", userQuery,
                        "query_results", queryResults.getAll()
                )
        );
    }

    public QResponse isDeletedSystemWide(@NotEmpty String systemIdMemoryKey) {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        boolean deletedSystemWide = this.elm.isDeletedSystemWide(systemId);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "deletedSystemWide", deletedSystemWide
                )
        );
    }

    public QResponse setAsDeletedSystemWide(@NotEmpty String systemIdMemoryKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.validateMemoryKey(systemIdMemoryKey);
        SystemId systemId = (SystemId) QModule.getMemory().get(systemIdMemoryKey);
        Method setAsDeletedSystemWide = ELM.class.getDeclaredMethod("setAsDeletedSystemWide");
        setAsDeletedSystemWide.setAccessible(true);
        setAsDeletedSystemWide.invoke(this.elm, systemId);
        return new QResponse(
                this.moduleId,
                Map.of(
                        "systemId", systemId,
                        "set_as_system_wide_deleted", true
                )
        );
    }
}
