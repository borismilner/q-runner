package qa.interfaces;

import qa.config.Config;
import qa.structures.QModule;

import java.util.Map;

public interface QModuleProviderInterface {
    Map<String, QModule> getQModules(Config config);
}
