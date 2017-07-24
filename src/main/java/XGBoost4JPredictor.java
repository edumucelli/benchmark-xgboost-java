import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

@Slf4j
public class XGBoost4JPredictor implements BasePredictor {

    private Integer numberOfColumns;
    private Integer numberOfRows;

    private String modelFilename;

    private Booster predictor;
    private Utils.Method method = Utils.Method.XGBOOST4J;

    XGBoost4JPredictor(Integer numberOfColumns, Integer numberOfRows) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.modelFilename = String.format("xgbTree.benchmark.%s.xgb", this.numberOfColumns);
    }

    @Override
    public void predict() {

        Random random = new Random();
        int numberOfRepeats = 100;

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
                log.info(String.format("%s, %s, %s, %s", numberOfRows, numberOfColumns, repeatDuration, method.getName()));
                // log.info(Arrays.deepToString(predictions));
            } catch (XGBoostError xgBoostError) {
                xgBoostError.printStackTrace();
            }
        }
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
