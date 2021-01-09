package qa.osf;

import osf.manager.ModuleManager;
import qa.structures.QModule;

public class Container {
    private static ModuleManager moduleManager;

    public static QModule getService(Class<?> clazz) {
        if (Container.moduleManager == null) {
            Container.moduleManager = ModuleManager.getInstance();
            Container.moduleManager.init("serviceId");
        }
        return (QModule) Container.moduleManager.getComponent(clazz);
    }
}
