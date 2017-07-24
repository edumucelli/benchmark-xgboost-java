import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class RJavaPredictor implements BasePredictor {

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
            log.info(String.format("rShowMessage: %s",  message));
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

    private Rengine engine = new Rengine(new String[] {"--no-save"}, false, new LoggingConsole(log));

    private Integer numberOfColumns;
    private Integer numberOfRows;

    private Utils.Method method;
    private String modelFilename;

    RJavaPredictor(Utils.Method method, Integer numberOfColumns, Integer numberOfRows) {
        this.numberOfColumns = numberOfColumns;
        this.numberOfRows = numberOfRows;
        this.method = method;
        this.modelFilename = String.format("%s.benchmark.%s.r", method.getName(), numberOfColumns);
    }

//    private String modelName;
//    private String [] definedSources = new String[] {"3", "4"};
//    private String [] definedCategories = new String[] {"1", "4", "5", "6", "7"};
//    private String [] definedTags = new String[] {"bus_station", "casino", "cinema", "city_hall", "fast_food", "gas_station", "gas_station,highway_access,parking,public_transport,restaurant", "highway_access", "highway_access,parking", "highway_access,parking,public_transport", "highway_access,parking,public_transport,restaurant,train_station,waiting_area", "highway_access,parking,public_transport,train_station,waiting_area", "highway_access,parking,restaurant", "highway_access,parking,waiting_area", "highway_access,parking,waiting_area,public_transport", "highway_services", "library", "park", "parking", "parking,public_transport", "parking,public_transport,restaurant,train_station,waiting_area", "parking,public_transport,train_station", "parking,public_transport,train_station,waiting_area", "parking,waiting_area", "parking,waiting_area,public_transport", "post_office", "public_transport", "school", "shopping", "stadium", "train_station", "train_station,parking,public_transport,gas_station,highway_access", "university", "waiting_area,public_transport"};
//    private String [] definedDistanceCuts = new String[] {"(0,200]", "(200,400]", "(400,600]", "(600,800]", "(800,1e+03]", "(1e+03,1.2e+03]", "(1.2e+03,1.4e+03]", "(1.4e+03,1.6e+03]", "(1.6e+03,1.8e+03]", "(1.8e+03,2e+03]", "(2e+03,2.2e+03]", "(2.2e+03,2.4e+03]", "(2.4e+03,2.6e+03]", "(2.6e+03,2.8e+03]", "(2.8e+03,3e+03]", "(3e+03,3.2e+03]", "(3.2e+03,3.4e+03]", "(3.4e+03,3.6e+03]", "(3.6e+03,3.8e+03]", "(3.8e+03,4e+03]", "(4e+03,4.2e+03]", "(4.2e+03,4.4e+03]", "(4.4e+03,4.6e+03]", "(4.6e+03,4.8e+03]", "(4.8e+03,5e+03]", "(5e+03,5.2e+03]", "(5.2e+03,5.4e+03]", "(5.4e+03,5.6e+03]", "(5.6e+03,5.8e+03]", "(5.8e+03,6e+03]", "(6e+03,6.2e+03]", "(6.2e+03,6.4e+03]", "(6.4e+03,6.6e+03]", "(6.6e+03,6.8e+03]", "(6.8e+03,7e+03]", "(7e+03,7.2e+03]", "(7.2e+03,7.4e+03]", "(7.4e+03,7.6e+03]", "(7.6e+03,7.8e+03]", "(7.8e+03,8e+03]", "(8e+03,8.2e+03]", "(8.2e+03,8.4e+03]", "(8.4e+03,8.6e+03]", "(8.6e+03,8.8e+03]", "(8.8e+03,9e+03]", "(9e+03,9.2e+03]", "(9.2e+03,9.4e+03]", "(9.4e+03,9.6e+03]", "(9.6e+03,9.8e+03]", "(9.8e+03,1e+04]"};
//    private String [] definedDevices = new String[] {"Android", "iOS"};
//    private String [] definedTypes = new String[] {"extra_stopover", "from", "stopover", "to"};

    @Override
    public boolean loadModel() {
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
        String resource = getClass().getClassLoader().getResource(modelFilename).getFile();
        engine.assign("model_file", resource);
        log.info(engine.eval("model_file").asString());
        engine.eval("model_fit <- readRDS(model_file)");
        log.info("Model read finished");
        return true;
    }

    @Override
    public void predict() {
        engine.assign("NUMBER_OF_ROWS", new int[]{numberOfRows});
        engine.assign("NUMBER_OF_FEATURES", new int[]{numberOfColumns});

        int numberOfRepeats = 100;

        for (int j = 0; j < numberOfRepeats; j++) {
            engine.eval("data = data.frame(SELECTED = sample(c(\"true\", \"false\"), NUMBER_OF_ROWS, replace=TRUE, prob=c(0.5, 0.5)), matrix(rnorm(20), nrow=NUMBER_OF_ROWS, ncol=NUMBER_OF_FEATURES))");
            engine.eval("data$SELECTED = as.factor(data$SELECTED)");

            Instant start = Instant.now();
            REXP dataFrame = engine.eval("as.data.frame(predict(model_fit, newdata = data, type=\"prob\"))");
            Instant end = Instant.now();
            long repeatDuration = Duration.between(start, end).toMillis();
            log.info(String.format("%s, %s, %s, %s", numberOfRows, numberOfColumns, repeatDuration, method.getName()));
        }
    }

    //    public void setTesting(int numberOfElements) {
//        String [] sources = new String[numberOfElements];
//        String [] categories = new String[numberOfElements];
//        String [] tags = new String[numberOfElements];
//        String [] distanceCut = new String[numberOfElements];
//        String [] devices = new String[numberOfElements];
//        String [] types = new String[numberOfElements];
//
//        for (int i = 0; i < numberOfElements; i++) {
//            sources[i] = definedSources[(new Random()).nextInt(definedSources.length)];
//            categories[i] = definedCategories[(new Random()).nextInt(definedCategories.length)];
//            tags[i] = definedTags[(new Random()).nextInt(definedTags.length)];
//            distanceCut[i] = definedDistanceCuts[(new Random()).nextInt(definedDistanceCuts.length)];
//            devices[i] = definedDevices[(new Random()).nextInt(definedDevices.length)];
//            types[i] = definedTypes[(new Random()).nextInt(definedTypes.length)];
//        }
//
//        engine.assign("SOURCE", sources);
//        engine.assign("CATEGORY", categories);
//        engine.assign("TAGS", tags);
//        engine.assign("DISTANCE_CITY_CENTER_RANGE", distanceCut);
//        engine.assign("DEVICE", devices);
//        engine.assign("TYPE", types);
//        engine.eval("testing = data.frame(SOURCE, CATEGORY, TAGS, DISTANCE_CITY_CENTER_RANGE, DEVICE, TYPE)");

//    }

/*    public void setTrainingData(int numberOfElements) {
        // Prepare testing data frame
        double [] sepalLength = new double[numberOfElements];
        double [] sepalWidth = new double[numberOfElements];
        double [] petalLength = new double[numberOfElements];
        double [] petalWidth = new double[numberOfElements];

        for (int i = 0; i < numberOfElements; i++) {
            sepalLength[i] = Utils.randomInRange(0.0, 10.0);
            sepalWidth[i] = Utils.randomInRange(0.0, 10.0);
            petalLength[i] = Utils.randomInRange(0.0, 10.0);
            petalWidth[i] = Utils.randomInRange(0.0, 10.0);
        }

        engine.assign("Sepal.Length", sepalLength);
        engine.assign("Sepal.Width", sepalWidth);
        engine.assign("Petal.Length", petalLength);
        engine.assign("Petal.Width", petalWidth);
        engine.eval("testing = data.frame(Sepal.Length, Sepal.Width, Petal.Length, Petal.Width)");
    }*/

//    @Override
//    public void predict() {
//        engine.eval("predict.train(model_fit, data[, -1], type='prob')['true']");

//        double selectedProbability = engine.eval("predict(model_fit, newdata = testing, type='prob')['true']").asVector().at(0).asDouble();
//        log.info(selectedProbability);

//         log.info(engine.eval("testing[,1]"));
//         log.info(engine.eval(String.format("%s$bestTune", modelName)));
        // RFactor factor = engine.eval("predict(rf, newdata = testing)");
//        REXP dataFrame = engine.eval("as.data.frame(predict(model_fit, newdata = testing, type=\"prob\"))");

//        log.info(dataFrame);
//        log.info(dataFrame.asVector().at(0));

        // Get back the predicted species
//        int numberOfPredictions = dataFrame.asVector().at(0).asFactor().size();
//        for (int i = 0; i < numberOfPredictions; i++) {
//            log.info(String.format("Predicted class '%s' for factor %d", dataFrame.asVector().at(0).asFactor().at(i), i));
//        }

//        RList rList = new RList();
//
//        rList.put("a", new REXPInteger(new int[] { 0, 1, 2, 3 }));
//        rList.put("b", new REXPDouble(new double[] { 0.5, 1.2, 2.3, 3.0 }));

//        try {
//            engine.assign("x", String.valueOf(REXP.createDataFrame(rList)));
//        } catch (REXPMismatchException e) {
//            e.printStackTrace();
//        }

//        REXP mydf = REXP.createDataFrame(
//                new RList(
//                        new REXP[] {
//                                new REXPString(col1),
//                                new REXPString(col2),
//                                new REXPInteger(col3)},
//                        colNames)
//        );

        //engine.assign("myDataFrame", mydf);
//        engine.eval("print(x)");
//        log.info(engine.eval("x").asDoubleMatrix());
    }

//    public void run (int numberOfElements) {
//        Collection<Double> results = new ConcurrentLinkedQueue<>();
//        CompletableFuture<?>[] allFutures = new CompletableFuture[numberOfElements];
//
//        String source;
//        String category;
//        String tag;
//        String distanceCut;
//        String device;
//        String type;
//
//        long timeDiff = 0L;
//
//        for (int i = 0; i < numberOfElements; i++) {
//            // int id = i;
//
//            source = definedSources[(new Random()).nextInt(definedSources.length)];
//            category = definedCategories[(new Random()).nextInt(definedCategories.length)];
//            tag = definedTags[(new Random()).nextInt(definedTags.length)];
//            distanceCut = definedDistanceCuts[(new Random()).nextInt(definedDistanceCuts.length)];
//            device = definedDevices[(new Random()).nextInt(definedDevices.length)];
//            type = definedTypes[(new Random()).nextInt(definedTypes.length)];
//
//            long startTime = System.nanoTime();
//            predict(source, category, tag, distanceCut, device, type);
//            long stopTime = System.nanoTime();
//            timeDiff += stopTime - startTime;
//            // CompletableFuture<Double> future = CompletableFuture.supplyAsync(()-> new Predictor(id, engine, source, category, tag, distanceCut, device, type).get());
//            // allFutures[i] = future.thenAccept(results::add);
//
////            allFutures[i] = CompletableFuture
////                    .supplyAsync(()-> new Predictor(id, engine, source, category, tag, distanceCut, device, type).get())
////                    .thenAccept(results::add);
//        }
//
//        String result = String.format("%s,%s,%s", numberOfElements, TimeUnit.MILLISECONDS.convert(timeDiff, TimeUnit.NANOSECONDS), modelName);
//        log.info(result);
//
//        // log.info(results);
//        // CompletableFuture.allOf(allFutures).thenAccept(c -> log.info(results));
//    }

//    public void predict(String source, String category, String tags, String distanceCut, String device, String type) {
//        Object[] args = new Object[] {source, category, tags, distanceCut, device, type};
//        String dataFrame = String.format("data.frame(SOURCE = \"%s\", CATEGORY = \"%s\", TAGS = \"%s\", DISTANCE_CITY_CENTER_RANGE = \"%s\", DEVICE = \"%s\", TYPE = \"%s\")", args);
//        double selectedProbability = engine.eval(String.format("predict(model_fit, newdata = %s, type=\"prob\")[\"true\"]", dataFrame)).asVector().at(0).asDouble();
//        log.info(selectedProbability);
//    }
//
//    static class Predictor implements Supplier<Double> {
//
//        private final int id;
//        private final Rengine engine;
//        private final String source;
//        private final String category;
//        private final String tags;
//        private final String distanceCut;
//        private final String device;
//        private final String type;
//
//        Predictor(int id, Rengine engine, String source, String category, String tags, String distanceCut, String device, String type) {
//            this.id = id;
//            this.engine = engine;
//            this.source = source;
//            this.category = category;
//            this.tags = tags;
//            this.distanceCut = distanceCut;
//            this.device = device;
//            this.type = type;
//        }
//
//        @Override
//        public Double get() {
//            Object[] args = new Object[] {source, category, tags, distanceCut, device, type};
//            String dataFrame = String.format("data.frame(SOURCE = \"%s\", CATEGORY = \"%s\", TAGS = \"%s\", DISTANCE_CITY_CENTER_RANGE = \"%s\", DEVICE = \"%s\", TYPE = \"%s\")", args);
//            // log.info(String.format("[%s] %s", id, probability));
//            return engine.eval(String.format("predict(model_fit, newdata = %s, type=\"prob\")[\"true\"]", dataFrame)).asVector().at(0).asDouble();
//        }
//    }
//}
