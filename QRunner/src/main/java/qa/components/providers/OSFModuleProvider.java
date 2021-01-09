package qa.components.providers;

import qa.config.Config;
import qa.config.ModuleConfig;
import qa.exceptions.QException;
import qa.interfaces.QModuleProviderInterface;
import qa.osf.Container;
import qa.structures.QModule;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class OSFModuleProvider implements QModuleProviderInterface {

    public OSFModuleProvider() {
        // Do nothing
    }

    @Override
    public Map<String, QModule> getQModules(Config config) {
        Map<String, QModule> idToModule = new HashMap<>();
        for (ModuleConfig moduleConfig : config.getInstanceToModule()) {
            QModule qModule = null;
            String moduleName = moduleConfig.getqModule();
            try {
                qModule = Container.getService(Class.forName(moduleName));
            } catch (ClassNotFoundException classNotFoundException) {
                throw new QException(Response.Status.INTERNAL_SERVER_ERROR, String.format("Error loading module: %s", moduleName), classNotFoundException);
            }
            String moduleId = moduleConfig.getInstanceId();
            idToModule.put(moduleId, qModule);
        }
        return idToModule;
    }
}
