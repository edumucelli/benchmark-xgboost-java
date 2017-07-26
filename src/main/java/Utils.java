import java.io.IOException;
import java.io.Writer;
import java.util.Random;

public class Utils {

    private static Random random = new Random();

    enum Method {
        GBM("gbm"),
        GLM("glm"),
        XGB("xgbTree"),
        XGB_LINEAR("xgbLinear");

        private final String name;

        Method(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    static void writeLine(Writer w, String line) {
        StringBuilder sb = new StringBuilder();
        try {
            w.append(sb.append(line).append("\n").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
