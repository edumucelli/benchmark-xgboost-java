import lombok.Data;

@Data
public class PredictorResult {
    private final Integer numberOfRows;
    private final Integer numberOfColumns;
    private final Long repeatDuration;
    private final String label;

    public String toString() {
        return String.format("%s, %s, %s, %s", numberOfRows, numberOfColumns, repeatDuration, label);
    }
}
