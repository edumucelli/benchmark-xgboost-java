import java.util.Random;

public class Utils {

    private static Random random = new Random();

    public static double randomInRange(double min, double max) {
        double range = max - min;
        double scaled = random.nextDouble() * range;
        return scaled + min;
    }

    enum Method {
        GBM("gbm"),
        GLM("glm"),
        XGB("xgbTree"),
        XGBOOST4J("XGBoost4j"),
        XGBOOST_PREDICTOR("XGBPredictor");

        private final String name;

        Method(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
