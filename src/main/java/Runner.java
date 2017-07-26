import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class Runner {
    public static void main(String[] args) throws IOException {

        // log.info(System.getProperty("java.library.path"));

        String csvFile = "results_boosting.csv"; // "results_glm.csv";
        FileWriter writer = new FileWriter(csvFile);

        List<PredictorResult> results = new ArrayList<>();

        List<Integer> columns = Arrays.asList(100, 500, 1000);  // Number of features
        List<Integer> rows = Arrays.asList(100, 500, 1000);     // Number of elements to predict

        for (Integer numberOfColumns : columns) {
            for (Integer numberOfRows : rows) {

                // Utils.Method.GBM or GLM
                RJavaPredictor rJavaPredictor = new RJavaPredictor(Utils.Method.GBM, numberOfColumns, numberOfRows);

                if (rJavaPredictor.loadModel()) {
                    results.addAll(rJavaPredictor.predict());
                }

                // Utils.Method.GBM or GLM
                PMMLPredictor pmmlPredictor = new PMMLPredictor(Utils.Method.GBM, numberOfColumns, numberOfRows);

                if (pmmlPredictor.loadModel()) {
                    results.addAll(pmmlPredictor.predict());
                }

                // Utils.Method.XGB or XGB_LINEAR
                XGBoostPredictor xgBoostPredictor = new XGBoostPredictor(Utils.Method.XGB, numberOfColumns, numberOfRows);

                if (xgBoostPredictor.loadModel()) {
                    results.addAll(xgBoostPredictor.predict());
                }

                // Utils.Method.XGB or XGB_LINEAR
                XGBoost4JPredictor xgBoost4jPredictor = new XGBoost4JPredictor(Utils.Method.XGB, numberOfColumns, numberOfRows);

                if (xgBoost4jPredictor.loadModel()) {
                    results.addAll(xgBoost4jPredictor.predict());
                }

            }
        }

        results.forEach(result -> Utils.writeLine(writer, result.toString()));

        writer.flush();
        writer.close();
    }
}
