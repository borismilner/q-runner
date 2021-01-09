package qa.components.providers;

import qa.config.Config;
import qa.interfaces.QModuleProviderInterface;
import qa.structures.QModule;
import qa.structures.QResponse;

import java.util.Map;
import java.util.Random;

public class MockModuleProvider implements QModuleProviderInterface {

    @Override
    public Map<String, QModule> getQModules(Config config) {
        return Map.of(
                "instance_1", new MockQModule(),
                "instance_2", new MockQModule()
        );
    }

    class MockQModule extends QModule {
        public MockQModule() {
            super("instance_" + new Random().nextInt(), "mockModule");
        }

        public QResponse greetMe() {
            return new QResponse(this.moduleId, Map.of("hello_from", this.instanceId));
        }
    }
}
