package benchmark.predictor;

public interface Predictor {
    /**
     * Applies the prediction
     */
    void predict();

    /**
     * Prepare all the data required for the prediction, e.g., setting up dataframe with the correct amount
     * of features and samples. This method will be called before every call to `predict`.
     */
    void prepare();

    /**
     * Loads the Predictor's model file and prepares the predictor
     * @return did the model was successfully loaded
     */
    boolean loadModel();
}
