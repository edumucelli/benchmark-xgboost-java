import java.util.List;

public interface BasePredictor {
    List<PredictorResult> predict();
    boolean loadModel();
}
