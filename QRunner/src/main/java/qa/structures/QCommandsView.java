package qa.structures;

import io.dropwizard.views.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QCommandsView extends View {
    private final List<MethodDescription> commands = new ArrayList<>();

    protected QCommandsView() {
        super("web/commands.mustache");
    }

    public List<MethodDescription> getCommands() throws IOException {
        QModule qModule = new QModule("", "");
        QResponse qaCommands = qModule.getCommands();
        Map<String, Object> response = qaCommands.getResponse();
        for (Object x : response.values()) {
            this.commands.addAll((ArrayList<MethodDescription>) x);
        }
        return this.commands;
    }
}
