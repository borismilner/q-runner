package qa.structures;

public class ParameterDescription {
    private final String parameterName;

    private final String parameterType;

    public ParameterDescription(String parameterName, String parameterType) {
        this.parameterName = parameterName;
        this.parameterType = parameterType;
    }

    public String getParameterName() {
        return this.parameterName;
    }

    public String getParameterType() {
        return this.parameterType;
    }
}
