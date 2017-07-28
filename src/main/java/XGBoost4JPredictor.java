import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
public class XGBoost4JPredictor implements BasePredictor {

    private Integer numberOfColumns;
    private Integer numberOfRows;

    private Integer numberOfRepeats;

    private String modelFilename;

    private Booster predictor;
    private Utils.Method method;

    private final String NAME = "XGBoost4J";

    private List<PredictorResult> results = new ArrayList<>();

    XGBoost4JPredictor(Utils.Method method, Integer numberOfColumns, Integer numberOfRows, Integer numberOfRepeats) {
        this.method = method;
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.numberOfRepeats = numberOfRepeats;
        this.modelFilename = String.format("%s.benchmark.%s.xgb", method.getName(), this.numberOfColumns);
    }

    @Override
    public List<PredictorResult> predict() {

        Random random = new Random();

        for (int j = 0; j < numberOfRepeats; j++) {
            long repeatDuration;

            float[] data = new float[numberOfColumns * numberOfRows];
            for (int i = 0; i < data.length; i++) {
                data[i] = random.nextFloat();
            }

            try {
                DMatrix matrix = new DMatrix(data, numberOfRows, numberOfColumns);
                // The resulting probabilities are predictions[i][0], where i is the
                // i-th element given to be predicted. I.e., second dimension of the matrix
                // seems to be useless
                Instant start = Instant.now();
                float[][] predictions = predictor.predict(matrix);
                Instant end = Instant.now();

                repeatDuration = Duration.between(start, end).toMillis();

                String label = String.format("%s (%s)", NAME, method.getName());
                log.info(String.format("%s, %s, %s, %s", numberOfRows, numberOfColumns, repeatDuration, label));

                PredictorResult result = new PredictorResult(numberOfRows, numberOfColumns, repeatDuration, label);
                results.add(result);
            } catch (XGBoostError xgBoostError) {
                xgBoostError.printStackTrace();
            }
        }
        return results;
    }

    @Override
    public boolean loadModel() {
        // Load model and create Predictor
        InputStream resource = XGBoost4JPredictor.class.getResourceAsStream(modelFilename);

        try {
            predictor = XGBoost.loadModel(resource);
        } catch (IOException | XGBoostError e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
