package qa.structures;

import java.util.ArrayList;
import java.util.List;

public class MethodDescription {

    private final List<ParameterDescription> parameters = new ArrayList<>();
    private final String methodName;

    public MethodDescription(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public List<ParameterDescription> getParameters() {
        return this.parameters;
    }
}
