package view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Radar;

import java.util.function.Consumer;

public class ControlPanel extends ScrollPane {

    private final Slider   rotationSlider;
    private final Slider   powerSlider;
    private final Slider   gainSlider;
    private final Slider   rangeSlider;
    private final Slider   pfaSlider;
    private final CheckBox showTargetsBox;
    private final CheckBox showSweepTrailBox;
    private final Button   addAircraftBtn;
    private final Button   addDroneBtn;
    private final Button   addMissileBtn;
    private final Button   clearTargetsBtn;
    private final Button   resetStatsBtn;
    private final Label    rotLabel;
    private final Label    powerLabel;
    private final Label    gainLabel;
    private final Label    rangeLabel;
    private final Label    pfaLabel;

    private Consumer<Radar>   onRadarChanged;
    private Runnable          onAddAircraft;
    private Runnable          onAddDrone;
    private Runnable          onAddMissile;
    private Runnable          onClearTargets;
    private Runnable          onResetStats;
    private Consumer<Boolean> onShowTargets;
    private Consumer<Boolean> onShowSweepTrail;

    private static final String FONT_FAMILY = "Segoe UI, Arial, sans-serif";
    private static final String STYLE_TITLE =
            "-fx-text-fill: #00E5FF; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 11px; -fx-font-weight: bold;";
    private static final String STYLE_LABEL =
            "-fx-text-fill: #A0B0C0; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 11px;";
    private static final String STYLE_VALUE =
            "-fx-text-fill: #00FF66; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 11px; -fx-font-weight: bold;";
    private static final String STYLE_BTN =
            "-fx-background-color: #121E2A; -fx-text-fill: #00E5FF; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 11px; " +
                    "-fx-border-color: #1E3245; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;";
    private static final String STYLE_BTN_DANGER =
            "-fx-background-color: #2A1215; -fx-text-fill: #FF4D4D; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 11px; " +
                    "-fx-border-color: #4A1E22; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;";

    public ControlPanel(double width) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: #0B1015;");

        setContent(container);
        setFitToWidth(true);
        setStyle("-fx-background-color: #0B1015; -fx-background: #0B1015; -fx-border-color: #1E2D3D; -fx-border-width: 0 1 0 0;");
        setPrefWidth(width);

        // ── Section Paramètres Radar ──
        Label radarTitle = new Label("PARAMÈTRES RADAR");
        radarTitle.setStyle(STYLE_TITLE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        rotLabel = new Label("6.0 RPM"); rotLabel.setStyle(STYLE_VALUE);
        rotationSlider = makeSlider(1, 20, 6);
        addRow(grid, 0, "Rotation :", rotationSlider, rotLabel);

        powerLabel = new Label("500 kW"); powerLabel.setStyle(STYLE_VALUE);
        powerSlider = makeSlider(100, 2000, 500);
        addRow(grid, 1, "Puissance :", powerSlider, powerLabel);

        gainLabel = new Label("33 dB"); gainLabel.setStyle(STYLE_VALUE);
        gainSlider = makeSlider(20, 50, 33);
        addRow(grid, 2, "Gain :", gainSlider, gainLabel);

        rangeLabel = new Label("400 km"); rangeLabel.setStyle(STYLE_VALUE);
        rangeSlider = makeSlider(50, 500, 400);
        addRow(grid, 3, "Portée :", rangeSlider, rangeLabel);

        pfaLabel = new Label("1e-6"); pfaLabel.setStyle(STYLE_VALUE);
        pfaSlider = makeSlider(1, 10, 6);
        addRow(grid, 4, "Pfa (10^-x) :", pfaSlider, pfaLabel);

        // Listeners
        rotationSlider.valueProperty().addListener((o, old, val) -> {
            rotLabel.setText(String.format("%.1f RPM", val.doubleValue()));
            fireRadarChanged();
        });
        powerSlider.valueProperty().addListener((o, old, val) -> {
            powerLabel.setText(String.format("%.0f kW", val.doubleValue()));
            fireRadarChanged();
        });
        gainSlider.valueProperty().addListener((o, old, val) -> {
            gainLabel.setText(String.format("%.0f dB", val.doubleValue()));
            fireRadarChanged();
        });
        rangeSlider.valueProperty().addListener((o, old, val) -> {
            rangeLabel.setText(String.format("%.0f km", val.doubleValue()));
            fireRadarChanged();
        });
        pfaSlider.valueProperty().addListener((o, old, val) -> {
            pfaLabel.setText(String.format("1e-%.0f", val.doubleValue()));
            fireRadarChanged();
        });

        // ── Section Affichage ──
        Label dispTitle = new Label("AFFICHAGE TACTIQUE");
        dispTitle.setStyle(STYLE_TITLE);

        showTargetsBox = new CheckBox("Vraies cibles (debug overlay)");
        showTargetsBox.setSelected(true);
        showTargetsBox.setStyle(STYLE_LABEL);
        showTargetsBox.selectedProperty().addListener((o, old, val) -> { if (onShowTargets != null) onShowTargets.accept(val); });

        showSweepTrailBox = new CheckBox("Traînée du faisceau (sweep)");
        showSweepTrailBox.setSelected(true);
        showSweepTrailBox.setStyle(STYLE_LABEL);
        showSweepTrailBox.selectedProperty().addListener((o, old, val) -> { if (onShowSweepTrail != null) onShowSweepTrail.accept(val); });

        // ── Section Cibles ──
        Label targetTitle = new Label("GÉNÉRATION CIBLES");
        targetTitle.setStyle(STYLE_TITLE);

        addAircraftBtn = new Button("+ Avion");
        addDroneBtn    = new Button("+ Drone");
        addMissileBtn  = new Button("+ Missile");

        for (Button b : new Button[]{addAircraftBtn, addDroneBtn, addMissileBtn}) {
            b.setStyle(STYLE_BTN);
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }

        addAircraftBtn.setOnAction(e -> { if (onAddAircraft != null) onAddAircraft.run(); });
        addDroneBtn   .setOnAction(e -> { if (onAddDrone    != null) onAddDrone.run(); });
        addMissileBtn .setOnAction(e -> { if (onAddMissile  != null) onAddMissile.run(); });

        HBox targetBtns = new HBox(5, addAircraftBtn, addDroneBtn, addMissileBtn);

        clearTargetsBtn = new Button("Effacer toutes les cibles");
        clearTargetsBtn.setStyle(STYLE_BTN_DANGER);
        clearTargetsBtn.setMaxWidth(Double.MAX_VALUE);
        clearTargetsBtn.setOnAction(e -> { if (onClearTargets != null) onClearTargets.run(); });

        resetStatsBtn = new Button("Réinitialiser statistiques");
        resetStatsBtn.setStyle(STYLE_BTN_DANGER);
        resetStatsBtn.setMaxWidth(Double.MAX_VALUE);
        resetStatsBtn.setOnAction(e -> { if (onResetStats != null) onResetStats.run(); });

        container.getChildren().addAll(
                radarTitle, grid, new Separator(),
                dispTitle, showTargetsBox, showSweepTrailBox, new Separator(),
                targetTitle, targetBtns, clearTargetsBtn, resetStatsBtn
        );
    }

    private Slider makeSlider(double min, double max, double val) {
        Slider s = new Slider(min, max, val);
        s.setPrefWidth(110);
        return s;
    }

    private void addRow(GridPane grid, int row, String label, Slider slider, Label value) {
        Label lbl = new Label(label);
        lbl.setStyle(STYLE_LABEL);
        lbl.setPrefWidth(90);
        grid.add(lbl, 0, row);
        grid.add(slider, 1, row);
        grid.add(value, 2, row);
    }

    private void fireRadarChanged() {
        if (onRadarChanged == null) return;
        Radar r = new Radar();
        r.setRotationSpeedRPM(rotationSlider.getValue());
        r.setPeakPower(powerSlider.getValue() * 1000);
        r.setAntennaGainDB(gainSlider.getValue());
        r.setMaxRangeKm(rangeSlider.getValue());
        r.setPfa(Math.pow(10, -pfaSlider.getValue()));
        onRadarChanged.accept(r);
    }

    public void setOnRadarChanged(Consumer<Radar> cb)    { this.onRadarChanged = cb; }
    public void setOnAddAircraft(Runnable cb)            { this.onAddAircraft = cb; }
    public void setOnAddDrone(Runnable cb)               { this.onAddDrone = cb; }
    public void setOnAddMissile(Runnable cb)             { this.onAddMissile = cb; }
    public void setOnClearTargets(Runnable cb)           { this.onClearTargets = cb; }
    public void setOnResetStats(Runnable cb)             { this.onResetStats = cb; }
    public void setOnShowTargets(Consumer<Boolean> cb)   { this.onShowTargets = cb; }
    public void setOnShowSweepTrail(Consumer<Boolean> cb){ this.onShowSweepTrail = cb; }
}
