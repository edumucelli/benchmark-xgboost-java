import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class PMMLPredictor implements BasePredictor {

    private Integer numberOfColumns;
    private Integer numberOfRows;

    private Integer numberOfRepeats;

    private Utils.Method method;
    private String modelFilename;

    private ModelEvaluator<?> evaluator;

    private final String NAME = "PMML";

    private List<PredictorResult> results = new ArrayList<>();

    PMMLPredictor(Utils.Method method, Integer numberOfColumns, Integer numberOfRows, Integer numberOfRepeats) {
        this.method = method;
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.numberOfRepeats = numberOfRepeats;
        this.modelFilename = String.format("%s.benchmark.%s.pmml", this.method.getName(), this.numberOfColumns);
    }

    @Override
    public boolean loadModel() {

        PMML pmml;

        InputStream resource = PMMLPredictor.class.getResourceAsStream(modelFilename);
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
    public List<PredictorResult> predict() {

        Map<FieldName, ?> predictions;
        List<InputField> inputFields;
        Map<FieldName, FieldValue> arguments;

        inputFields = evaluator.getInputFields();

        for (int j = 0; j < numberOfRepeats; j++) {
            arguments = new LinkedHashMap<>();
            long repeatDuration = 0L;
            for (int i = 0; i < numberOfRows; i++) {
                long rowDuration;
                for (InputField inputField : inputFields) {

                    FieldName inputFieldName = inputField.getName();
                    FieldValue inputFieldValue = inputField.prepare(randomValue());

                    arguments.put(inputFieldName, inputFieldValue);
                }

                Instant start = Instant.now();

                predictions = evaluator.evaluate(arguments);
                ProbabilityDistribution probabilityDistribution = (ProbabilityDistribution) predictions.get(evaluator.getTargetField().getName());
                probabilityDistribution.getProbability("true");

                Instant end = Instant.now();

                rowDuration = Duration.between(start, end).toMillis();
                repeatDuration += rowDuration;
            }

            String label = String.format("%s (%s)", NAME, method.getName().toUpperCase());

            log.info(String.format("%s, %s, %s, %s", numberOfRows, numberOfColumns, repeatDuration, label));

            PredictorResult result = new PredictorResult(numberOfRows, numberOfColumns, repeatDuration, label);
            results.add(result);
        }

        return results;

//        inputFields = evaluator.getInputFields();
//        for (InputField inputField : inputFields) {
//            FieldName inputFieldName = inputField.getName();
//            FieldValue inputFieldValue = inputField.prepare(2);
//            arguments.put(inputFieldName, inputFieldValue);
//        }
//        ((ProbabilityDistribution) evaluator.evaluate(arguments).get(evaluator.getTargetFieldName())).getProbability("versicolor");
    }

//    void predict() {
//
//
//        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();
//
//        FieldName sepalLengthName = FieldName.create("Sepal.Length");
//        FieldName sepalWidthName = FieldName.create("Sepal.Width");
//        FieldName petalLengthName = FieldName.create("Petal.Length");
//        FieldName petalWidthName = FieldName.create("Petal.Width");
//
//        FieldValue sepalLengthValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 5.1);
//        FieldValue sepalWidthValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 3.8);
//        FieldValue petalLengthValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 1.5);
//        FieldValue petalWidthValue = FieldValueUtil.create(DataType.DOUBLE, OpType.CONTINUOUS, 0.3);
//
//        arguments.put(sepalLengthName, sepalLengthValue);
//        arguments.put(sepalWidthName, sepalWidthValue);
//        arguments.put(petalLengthName, petalLengthValue);
//        arguments.put(petalWidthName, petalWidthValue);
//
////        List<InputField> inputFields = evaluator.getInputFields();
////        for (InputField inputField : inputFields) {
////            FieldName inputFieldName = inputField.getName();
////            log.info(inputFieldName.toString());
////            double value = 5.8;
////            FieldValue inputFieldValue = inputField.prepare(value);
////            arguments.put(inputFieldName, inputFieldValue);
////        }
//
////        log.info(arguments.toString());
//
//        Map<FieldName, ?> results = evaluator.evaluate(arguments);
//
//        List<TargetField> targetFields = evaluator.getTargetFields();
//        for(TargetField targetField : targetFields){
//            FieldName targetFieldName = targetField.getName();
//            ProbabilityDistribution probabilityDistribution = (ProbabilityDistribution) results.get(targetFieldName);
//            log.info(targetFieldName.toString() + ": " + probabilityDistribution.toString());
//            log.info(probabilityDistribution.getProbability("setosa").toString());
//        }
//
////        List<OutputField> outputFields = evaluator.getOutputFields();
////        for(OutputField outputField : outputFields){
////            FieldName outputFieldName = outputField.getName();
////            Object outputFieldValue = results.get(outputFieldName);
////            log.info(outputFieldName.toString() + ": " + outputFieldValue.toString());
////        }
//
//    }

    private Double randomValue() {
        return Math.random();
    }

}
