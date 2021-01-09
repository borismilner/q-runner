package qa.config;

public class ModuleConfig {

    private String instanceId;
    private String qModule;

    public ModuleConfig(String instanceId, String qModule) {
        this.instanceId = instanceId;
        this.qModule = qModule;
    }

    public ModuleConfig() {

    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getqModule() {
        return this.qModule;
    }

    public void setqModule(String qModule) {
        this.qModule = qModule;
    }
}
