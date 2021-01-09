package qa.config;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class Config extends Configuration {

    private String commandAggregatorAddress;
    @NotEmpty
    private String outputAggregatorAddress;
    private boolean isAutoPullingCommands;
    private int autoPullingIntervalSeconds;

    private String applicationId;

    @NotEmpty
    private List<ModuleConfig> instanceToModule;

    private List<String> activeModuleLoaders;

    private List<String> activeObjectFactories;

    public List<ModuleConfig> getInstanceToModule() {
        return this.instanceToModule;
    }

    public void setInstanceToModule(List<ModuleConfig> instanceToModule) {
        this.instanceToModule = instanceToModule;
    }

    public String getOutputAggregatorAddress() {
        return this.outputAggregatorAddress;
    }

    public void setOutputAggregatorAddress(String outputAggregatorAddress) {
        this.outputAggregatorAddress = outputAggregatorAddress;
    }

    public String getCommandAggregatorAddress() {
        return this.commandAggregatorAddress;
    }

    public void setCommandAggregatorAddress(String commandAggregatorAddress) {
        this.commandAggregatorAddress = commandAggregatorAddress;
    }

    public boolean isAutoPullingCommands() {
        return this.isAutoPullingCommands;
    }

    public void setAutoPullingCommands(boolean autoPullingCommands) {
        this.isAutoPullingCommands = autoPullingCommands;
    }

    public int getAutoPullingIntervalSeconds() {
        return this.autoPullingIntervalSeconds;
    }

    public void setAutoPullingIntervalSeconds(int autoPullingIntervalSeconds) {
        this.autoPullingIntervalSeconds = autoPullingIntervalSeconds;
    }

    public String getApplicationId() {
        return this.applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public List<String> getActiveModuleLoaders() {
        return this.activeModuleLoaders;
    }

    public void setActiveModuleLoaders(List<String> activeModuleLoaders) {
        this.activeModuleLoaders = activeModuleLoaders;
    }

    public List<String> getActiveObjectFactories() {
        return this.activeObjectFactories;
    }

    public void setActiveObjectFactories(List<String> activeObjectFactories) {
        this.activeObjectFactories = activeObjectFactories;
    }

}
