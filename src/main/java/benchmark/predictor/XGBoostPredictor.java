package benchmark.predictor;

import benchmark.BoosterType;
import biz.k11i.xgboost.util.FVec;
import lombok.extern.slf4j.Slf4j;
import benchmark.PredictionMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.DoubleStream;

@Slf4j
public class XGBoostPredictor implements Predictor {

    private final int numberOfColumns;
    private final int numberOfRows;

    private FVec featureVector;

    private final String modelFilename;
    private biz.k11i.xgboost.Predictor predictor;

    public XGBoostPredictor(BoosterType boosterType, int numberOfColumns, int numberOfRows) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.modelFilename = getModelFileName(boosterType);
        prepare();
    }

    @Override
    public boolean loadModel() {
        InputStream resource = getClass().getClassLoader().getResourceAsStream(modelFilename);

        try {
            predictor = new biz.k11i.xgboost.Predictor(resource);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void prepare() {
        double[] denseArray = DoubleStream.generate(this::randomValue)
                .limit(numberOfColumns)
                .toArray();
        featureVector = FVec.Transformer.fromArray(denseArray, false);
    }

    @Override
    public void predict() {
        for (int i = 0; i < numberOfRows; i++) {
            double[] prediction = predictor.predict(featureVector);
            //  int[] prediction = predictor.predictLeaf(featureVector);
        }
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

    private Double randomValue() {
        return Math.random();
    }
}
