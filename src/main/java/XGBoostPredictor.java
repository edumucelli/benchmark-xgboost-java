import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.util.FVec;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

@Slf4j
public class XGBoostPredictor implements BasePredictor {

    private Integer numberOfColumns;
    private Integer numberOfRows;

    private String modelFilename;

    private Predictor predictor;
    private Utils.Method method;

    private final String NAME = "XGB-Predictor";

    private List<PredictorResult> results = new ArrayList<>();

    XGBoostPredictor(Utils.Method method, Integer numberOfColumns, Integer numberOfRows) {
        this.method = method;
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.modelFilename = String.format("%s.benchmark.%s.xgb", method.getName(), this.numberOfColumns);
    }

    @Override
    public boolean loadModel() {
        // Load model and create Predictor
        InputStream resource = XGBoostPredictor.class.getResourceAsStream(modelFilename);

        try {
            predictor = new Predictor(resource);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<PredictorResult> predict() {

        int numberOfRepeats = 100;
        Random random = new Random();

        FVec featureVector;

        for (int j = 0; j < numberOfRepeats; j++) {
            long repeatDuration = 0L;
            for (int i = 0; i < numberOfRows; i++) {
                long rowDuration;

                double[] denseArray = DoubleStream.generate(random::nextDouble).limit(numberOfColumns).toArray();
                featureVector = FVec.Transformer.fromArray(denseArray, false);

                Instant start = Instant.now();
                //  Labels in caret are encoded and given to XGBoost as following
                //  levels(data$CLASS) > [1] "false" "true"
                //  ifelse(data$CLASS == levels(data$CLASS)[1], 1, 0)
                //  [1] 0 0 0 0 0 0 1 1 0 0
                //  The "predict" results are a (0,1) probability. Given t=0.5 threshold,
                //  p <= t: class == "false"
                //  p > t: class == "true"
                double[] prediction = predictor.predict(featureVector);
                //  int[] prediction = predictor.predictLeaf(featureVector);
                Instant end = Instant.now();

                rowDuration = Duration.between(start, end).toMillis();
                repeatDuration += rowDuration;
                // log.info(Arrays.toString(prediction));
            }
            log.info(String.format("%s, %s, %s, %s (%s)", numberOfRows, numberOfColumns, repeatDuration, NAME, method.getName()));
            PredictorResult result = new PredictorResult(numberOfRows, numberOfColumns, repeatDuration, String.format("%s (%s)", NAME, method.getName()));
            results.add(result);
        }
        return results;
    }
}
