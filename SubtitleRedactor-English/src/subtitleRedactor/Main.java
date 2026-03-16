package subtitleRedactor;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        new Window(stage);
    }

    @Override
    public void stop() {
        FixerAI.destroyActiveProcesses();
    }

    public static void main(String[] args) {
        launch();
    }
}
