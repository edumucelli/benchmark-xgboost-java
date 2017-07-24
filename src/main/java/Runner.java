import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Runner {
    public static void main(String[] args) {
        int numberOfColumns = 1000;     // Number of features
        int numberOfRows = 1000;        // Number of elements to predict

        log.info(System.getProperty("java.library.path"));

        RJavaPredictor rJavaPredictor = new RJavaPredictor(Utils.Method.GLM, numberOfColumns, numberOfRows);

        if (rJavaPredictor.loadModel()) {
            rJavaPredictor.predict();
        }

        // GBM, GLM
        PMMLPredictor pmmlPredictor = new PMMLPredictor(Utils.Method.GLM, numberOfColumns, numberOfRows);

        if (pmmlPredictor.loadModel()) {
            pmmlPredictor.predict();
        }

        XGBoostPredictor xgBoostPredictor = new XGBoostPredictor(numberOfColumns, numberOfRows);

        if (xgBoostPredictor.loadModel()) {
            xgBoostPredictor.predict();
        }

        XGBoost4JPredictor xgBoost4jPredictor = new XGBoost4JPredictor(numberOfColumns, numberOfRows);

        if (xgBoost4jPredictor.loadModel()) {
            xgBoost4jPredictor.predict();
        }
    }
}
