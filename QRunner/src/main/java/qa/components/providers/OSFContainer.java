package qa.components.providers;

import osf.manager.ModuleManager;
import qa.structures.QModule;

public class OSFContainer {
    private static ModuleManager moduleManager;

    public static QModule getService(Class<?> clazz) {
        if (OSFContainer.moduleManager == null) {
            OSFContainer.moduleManager = ModuleManager.getInstance();
            OSFContainer.moduleManager.init("serviceId");
        }
        return (QModule) OSFContainer.moduleManager.getComponent(clazz);
    }
}
