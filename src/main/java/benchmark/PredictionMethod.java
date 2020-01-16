package benchmark;

public enum PredictionMethod {
    GBM("gbm"),
    GLM("glm"),
    XGB("xgbTree"),
    XGB_LINEAR("xgbLinear");

    private final String name;

    PredictionMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
