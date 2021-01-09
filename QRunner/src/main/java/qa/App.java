package qa;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import qa.config.Config;
import qa.interfaces.QModuleProviderInterface;
import qa.structures.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App extends Application<Config> {
    RequestHandler requestHandler;

    public static void main(String[] args) throws Exception {
        App app = new App();
        Shared.log.info("Initiating QRunner");
        app.run(args);
    }


    @Override
    public void run(Config config, Environment environment) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        Shared.objectMapper = environment.getObjectMapper();
        Shared.client = new JerseyClientBuilder(environment).build("RestClient");
        Shared.config = config;
        this.requestHandler = new RequestHandler();

        List<String> activeObjectFactories = config.getActiveObjectFactories();
        for (String factory : activeObjectFactories) {
            ObjectFactory objectFactory;
            objectFactory = (ObjectFactory) Class.forName(factory).getConstructor().newInstance();
            Shared.log.debug(String.format("Registering object-factory: %s", factory));
            QModule.registerObjectFactory(objectFactory);
        }
        List<String> activeModuleLoaders = config.getActiveModuleLoaders();
        for (String loader : activeModuleLoaders) {
            QModuleProviderInterface moduleProvider = (QModuleProviderInterface) Class.forName(loader).getConstructor().newInstance();
            Map<String, QModule> qModules = moduleProvider.getQModules(config);
            for (Map.Entry<String, QModule> entry : qModules.entrySet()) {
                String qModuleKey = entry.getKey();
                QModule qModule = entry.getValue();
                Shared.log.debug(String.format("Starting module-id %s with qa-module %s", qModuleKey, qModule.getClass().getSimpleName()));
                this.requestHandler.registerQModule(qModuleKey, qModule);
            }
        }
        environment.healthChecks().register("dummy", new HealthCheck() {
            @Override
            protected Result check() {
                return Result.healthy();
            }
        });
        environment.jersey().register(this.requestHandler);
        environment.jersey().register(new OSFExceptionMapper());
        environment.jersey().register(new QExceptionMapper());

        if (config.isAutoPullingCommands()) {
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(this::pullGatewayCommands, 0, config.getAutoPullingIntervalSeconds(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.addBundle(new ViewBundle<>());
        bootstrap.addBundle(new AssetsBundle("/qa/structures/web", "/web", "index.html"));
    }

    private void pullGatewayCommands() {
        String aggregatorAddress = String.format("%s/pullGatewayCommands", Shared.config.getCommandAggregatorAddress());
        WebTarget webTarget = Shared.client.target(aggregatorAddress);
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(Map.of("applicationId", Shared.config.getApplicationId())));
        List<QRequest> qRequests = (List<QRequest>) response.getEntity();
        for (QRequest qRequest : qRequests) {
            this.requestHandler.runCommand(qRequest);
        }
    }
}
