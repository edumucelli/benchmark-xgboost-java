package benchmark.predictor;

import benchmark.BoosterType;
import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import benchmark.PredictionMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

@Slf4j
public class XGBoost4JPredictor implements Predictor {

    private static final Random RANDOM = new Random();

    private float[] data;
    private final int numberOfColumns;
    private final int numberOfRows;

    private final String modelFilename;
    private Booster predictor;

    public XGBoost4JPredictor(BoosterType boosterType, int numberOfColumns, int numberOfRows) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.modelFilename = getModelFileName(boosterType);
        this.data = new float[numberOfColumns * numberOfRows];
    }

    @Override
    public void prepare() {
        for (int i = 0; i < data.length; i++) {
            data[i] = randomValue();
        }
    }

    @Override
    public void predict() {
        try {
            DMatrix matrix = new DMatrix(data, numberOfRows, numberOfColumns);
            // The resulting probabilities are predictions[i][0], where i is the
            // i-th element given to be predicted. I.e., second dimension of the matrix
            // seems to be useless
            float[][] predictions = predictor.predict(matrix);

        } catch (XGBoostError xgBoostError) {
            xgBoostError.printStackTrace();
        }

    }

    @Override
    public boolean loadModel() {
        InputStream resource = getClass().getClassLoader()
                .getResourceAsStream(modelFilename);

        if (resource == null) {
            return false;
        }

        try {
            predictor = XGBoost.loadModel(resource);
        } catch (IOException | XGBoostError e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String getModelFileName(BoosterType boosterType) {
        return String.format("%s.benchmark.%s.xgb", from(boosterType).getName(), this.numberOfColumns);
    }

    private PredictionMethod from(BoosterType boosterType) {
        if (boosterType.equals(BoosterType.TREE)) {
            return PredictionMethod.XGB;
        }
        return PredictionMethod.XGB_LINEAR;
    }

    private float randomValue() {
        return RANDOM.nextFloat();
    }
}
