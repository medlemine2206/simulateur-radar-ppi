
import controller.RadarController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import view.ControlPanel;
import view.RadarDisplay;
import view.StatsPanel;

/**
 * SIMULATEUR RADAR PPI — JavaFX
 * Mohamed Lemine Ahmed Jeddou | CY Tech
 */
public class Main extends Application {

    private static final double RADAR_SIZE  = 650;
    private static final double PANEL_WIDTH = 300;

    @Override
    public void start(Stage stage) {
        // ── Création des composants ──
        RadarDisplay radarDisplay = new RadarDisplay(RADAR_SIZE);
        StatsPanel   statsPanel   = new StatsPanel(PANEL_WIDTH);
        ControlPanel controlPanel = new ControlPanel(PANEL_WIDTH);

        // ── Contrôleur ──
        RadarController controller = new RadarController(radarDisplay, statsPanel);
        controller.bindControlPanel(controlPanel);

        // ── Layout à 3 colonnes ──
        BorderPane root = new BorderPane();
        root.setLeft(controlPanel);
        root.setCenter(radarDisplay);
        root.setRight(statsPanel);
        root.setStyle("-fx-background-color: #060A0E;");

        // ── Scène ──
        Scene scene = new Scene(root, RADAR_SIZE + (PANEL_WIDTH * 2), RADAR_SIZE);

        stage.setTitle("Simulateur Radar PPI — Thales C2 Style | CY Tech");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // ── Démarrer la simulation ──
        controller.start();

        // ── Arrêter proprement ──
        stage.setOnCloseRequest(e -> controller.stop());
    }

    public static void main(String[] args) {
        launch(args);
    }
}