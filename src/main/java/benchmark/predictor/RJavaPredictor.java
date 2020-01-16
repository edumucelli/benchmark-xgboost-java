package benchmark.predictor;

import benchmark.BoosterType;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import benchmark.PredictionMethod;

import java.util.logging.Logger;

public class RJavaPredictor implements Predictor {

    private static Logger log = Logger.getLogger("Runner");

    static class LoggingConsole implements RMainLoopCallbacks {
        private Logger log;

        LoggingConsole(Logger log) {
            this.log = log;
        }

        public void rWriteConsole(Rengine re, String text, int oType) {
            if (!text.isEmpty()) {
                log.info(String.format("rWriteConsole: %s", text));
            }
        }

        public void rBusy(Rengine re, int which) {
            log.info(String.format("rBusy: %s", which));
        }
        public void rShowMessage(Rengine re, String message) {
            log.info(String.format("rShowMessage: %s", message));
        }
        public String rReadConsole(Rengine re, String prompt, int addToHistory) {
            return null;
        }
        public String rChooseFile(Rengine re, int newFile) {
            return null;
        }
        public void rFlushConsole(Rengine re) {}
        public void rLoadHistory(Rengine re, String filename) {}
        public void rSaveHistory(Rengine re, String filename) {}
    }

    private final int numberOfColumns;
    private final int numberOfRows;

    private final String modelFilename;
    private Rengine engine = Rengine.getMainEngine();
    private final String predictionType;

    public RJavaPredictor(BoosterType boosterType, int numberOfColumns, int numberOfRows) {
        this.numberOfRows = numberOfRows;
        this.numberOfColumns = numberOfColumns;
        this.predictionType = getPredictionType(boosterType);
        this.modelFilename = getModelFileName(boosterType, numberOfColumns);

        if (engine == null) {
            engine = new Rengine(new String[]{"--no-save"}, false, new LoggingConsole(log));
        }
    }

    @Override
    public boolean loadModel() {
        log.info(System.getProperty("java.library.path"));

        // https://binfalse.de/2011/02/20/talking-r-through-java/
        if (!Rengine.versionCheck()) {
            System.err.println("Version mismatch - Java files don't match library version.");
            return false;
        }

        if (!engine.waitForR()) {
            System.out.println("Cannot load R");
            return false;
        }

        // Load library
        engine.eval(".libPaths(c('/home/eduardo/Software/R',.libPaths()))");
        engine.eval("library(caret)");
        engine.eval("options(java.parameters = \"-Xmx8g\")");

        // Load pre computed model
        String resource = getClass().getClassLoader()
                .getResource(modelFilename)
                .getFile();
        engine.assign("model_file", resource);
        log.info(engine.eval("model_file").asString());
        engine.eval("model_fit <- readRDS(model_file)");
        log.info("Model read finished");
        return true;
    }

    @Override
    public void prepare() {
        engine.assign("NUMBER_OF_ROWS", new int[]{numberOfRows});
        engine.assign("NUMBER_OF_FEATURES", new int[]{numberOfColumns});
        engine.assign("PREDICTION_TYPE", predictionType);

        engine.eval("data = data.frame(SELECTED = sample(c(\"true\", \"false\"), NUMBER_OF_ROWS, replace=TRUE, prob=c(0.5, 0.5)), matrix(rnorm(20), nrow=NUMBER_OF_ROWS, ncol=NUMBER_OF_FEATURES))");
        engine.eval("data$SELECTED = as.factor(data$SELECTED)");
    }

    @Override
    public void predict() {
        REXP dataFrame = engine.eval("as.data.frame(predict(model_fit, newdata = data, type=PREDICTION_TYPE))");
//      REXP dataFrame = engine.eval("predict.glm(model_fit, data)");
    }

    private String getModelFileName(BoosterType boosterType, int numberOfColumns) {
        return String.format("%s.benchmark.%s.r", from(boosterType).getName(), numberOfColumns);
    }

    private PredictionMethod from(BoosterType boosterType) {
        if (boosterType.equals(BoosterType.TREE)) {
            return PredictionMethod.GBM;
        }
        return PredictionMethod.GLM;
    }

    private String getPredictionType(BoosterType boosterType) {
        if (boosterType.equals(BoosterType.TREE)) {
            return "prob";
        }
        return "response";
    }
}
