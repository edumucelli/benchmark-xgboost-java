package benchmark.predictor;

import benchmark.BoosterType;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.ProbabilityDistribution;
import org.xml.sax.SAXException;
import benchmark.PredictionMethod;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PMMLPredictor implements Predictor {

    private final int numberOfRows;
    private Map<FieldName, FieldValue> matrix = new LinkedHashMap<>();

    private final String modelFilename;
    private ModelEvaluator<?> evaluator;

    public PMMLPredictor(BoosterType boosterType, int numberOfColumns, int numberOfRows) {
        this.numberOfRows = numberOfRows;
        this.modelFilename = getModelFileName(boosterType, numberOfColumns);
    }

    @Override
    public boolean loadModel() {
        PMML pmml;

        InputStream resource = getClass().getClassLoader().getResourceAsStream(modelFilename);
        log.info("Reading model " + modelFilename);

        try(InputStream in = resource) {
            pmml = org.jpmml.model.PMMLUtil.unmarshal(in);
        } catch (SAXException | JAXBException | IOException e) {
            e.printStackTrace();
            return false;
        }

        ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
        this.evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
        return true;
    }

    @Override
    public void prepare() {
        List<InputField> inputFields = evaluator.getInputFields();
        for (InputField inputField : inputFields) {
            matrix.put(inputField.getName(), inputField.prepare(randomValue()));
        }
    }

    @Override
    public void predict() {
        // Multiple evaluate calls are needed as Java PMML does not support one single evaluate with multiple
        // values to be predicted. Refer to https://groups.google.com/forum/#!topic/jpmml/0dpRRU-EtvQ for my
        // discussion with the lib maintainer (Villu Ruusmann).

        // TODO: Evaluate the usage of an Executor for parallel prediction with Runtime.getRuntime().availableProcessors()
        for (int i = 0; i < numberOfRows; i++) {
            Map<FieldName, ?> predictions = evaluator.evaluate(matrix);
            ProbabilityDistribution probabilityDistribution =
                    (ProbabilityDistribution) predictions.get(evaluator.getTargetField().getName());
            probabilityDistribution.getProbability("true");
        }

    }

    private String getModelFileName(BoosterType boosterType, Integer numberOfColumns) {
        return String.format("%s.benchmark.%s.pmml", from(boosterType).getName(), numberOfColumns);
    }

    private PredictionMethod from(BoosterType boosterType) {
        if (boosterType.equals(BoosterType.TREE)) {
            return PredictionMethod.GBM;
        }
        return PredictionMethod.GLM;
    }

    private Double randomValue() {
        return Math.random();
    }
}
