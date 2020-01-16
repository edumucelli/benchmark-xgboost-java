package benchmark;

import benchmark.predictor.PMMLPredictor;
import benchmark.predictor.Predictor;
import benchmark.predictor.RJavaPredictor;
import benchmark.predictor.XGBoost4JPredictor;
import benchmark.predictor.XGBoostPredictor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class Main {

    private static final String CSV_OUTPUT_FILENAME = "benchmark-xgboost-java.csv";
    /**
     * numberOfColumns and numberOfRows are the main experiment variables that will indicate how the different
     * predictors behave when handling "small" (100x100), i.e., 100 features x 100 samples, 100 features x 500
     * samples up to 1000 features x 1000 samples prediction matrix.
     *
     * Those values match with the available models in the resource folder. Just adding new values here won't work
     * as the predictors need a XGBoost file to read their models from. The current implementation is bundled with
     * models allowing only this values for the moment.
     */

    @Param({"100", "500", "1000"})
    public int numberOfColumns;

    @Param({"100", "500", "1000"})
    public int numberOfRows;

    /**
     * BoosterType aims to model the kind of booster will be used by the predictors. For instance when LINEAR
     * some will use GLM others will use XGB_LINEAR.
     */

    @Param({"LINEAR", "TREE"})
    public BoosterType boosterType;

    /**
     * The predictors. Each of the classes respecting the common interface {@code Predictor}. They represent the
     * libraries I am comparing, namely:
     * * PMMLPredictor: https://github.com/jpmml/jpmml-evaluator
     * * XGBoost4JPredictor: https://github.com/dmlc/xgboost/tree/master/jvm-packages
     * * XGBoostPredictor: https://github.com/komiya-atsushi/xgboost-predictor-java
     * * RJavaPredictor: rJava (https://github.com/s-u/rJava) + XGBoost from Caret (https://github.com/topepo/caret)
     */

    private PMMLPredictor pmmlPredictor;
    private XGBoost4JPredictor xgBoost4JPredictor;
    private XGBoostPredictor xgBoostPredictor;
    private RJavaPredictor rJavaPredictor;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .resultFormat(ResultFormatType.CSV)
                .result(CSV_OUTPUT_FILENAME)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        pmmlPredictor = new PMMLPredictor(
                boosterType,
                numberOfColumns,
                numberOfRows
        );

        xgBoost4JPredictor = new XGBoost4JPredictor(
                boosterType,
                numberOfColumns,
                numberOfRows
        );

        xgBoostPredictor = new XGBoostPredictor(
                boosterType,
                numberOfColumns,
                numberOfRows
        );

        rJavaPredictor = new RJavaPredictor(
                boosterType,
                numberOfColumns,
                numberOfRows
        );

        Stream.of(pmmlPredictor, xgBoost4JPredictor, xgBoostPredictor, rJavaPredictor)
                .forEach(Predictor::loadModel);
    }

    @Setup(Level.Invocation)
    public void setup() {
        Stream.of(pmmlPredictor, xgBoost4JPredictor, xgBoostPredictor, rJavaPredictor)
                .forEach(Predictor::prepare);
    }

    @Benchmark
    public void benchRJavaCaret() {
        rJavaPredictor.predict();
    }

    @Benchmark
    public void benchPMML() {
        pmmlPredictor.predict();
    }

    @Benchmark
    public void benchXGBoost4J() {
        xgBoost4JPredictor.predict();
    }

    @Benchmark
    public void benchXGBoostPredictor() {
        xgBoostPredictor.predict();
    }
}
