package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import model.Detection;
import model.Radar;
import model.Target;

import java.util.List;

public class RadarDisplay extends Canvas {

    private static final Color BG_COLOR       = Color.web("#060A0E");
    private static final Color RADAR_BG      = Color.web("#0A121A");
    private static final Color GRID_COLOR    = Color.web("#1E3245");
    private static final Color GRID_SUB      = Color.web("#12202E");
    private static final Color SWEEP_COLOR   = Color.web("#00FF66");
    private static final Color TEXT_MAIN     = Color.web("#80A0C0");
    private static final Color TEXT_HI       = Color.web("#00E5FF");

    private static final Font FONT_HUD = Font.font("Segoe UI", 11);
    private static final Font FONT_SYM = Font.font("Segoe UI", 10);

    private Radar           radar;
    private List<Target>    targets;
    private List<Detection> detections;
    private boolean         showTargets    = true;
    private boolean         showSweepTrail = true;

    public RadarDisplay(double size) {
        super(size, size);
    }

    public void draw(Radar radar, List<Target> targets, List<Detection> detections) {
        this.radar      = radar;
        this.targets    = targets;
        this.detections = detections;

        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double radius = Math.min(w, h) / 2 - 25;

        // Reset
        gc.setFill(BG_COLOR);
        gc.fillRect(0, 0, w, h);

        // Fond circulaire PPI
        gc.setFill(RADAR_BG);
        gc.fillOval(cx - radius, cy - radius, 2 * radius, 2 * radius);

        drawCoastlineMap(gc, cx, cy, radius);
        drawGridAndRings(gc, cx, cy, radius);

        if (showSweepTrail) drawSweepTrail(gc, cx, cy, radius);
        drawSweepLine(gc, cx, cy, radius);

        if (showTargets) drawTargetOverlay(gc, cx, cy, radius);
        drawDetections(gc, cx, cy, radius);

        drawOuterCompass(gc, cx, cy, radius);
        drawHUD(gc, w, h);
    }

    private void drawCoastlineMap(GraphicsContext gc, double cx, double cy, double r) {
        gc.setStroke(Color.web("#142432"));
        gc.setLineWidth(1.2);

        double scale = r / radar.getMaxRangeKm();
        gc.beginPath();
        gc.moveTo(cx - 160 * scale, cy - 250 * scale);
        gc.lineTo(cx - 90 * scale,  cy - 120 * scale);
        gc.lineTo(cx - 130 * scale, cy + 40 * scale);
        gc.lineTo(cx - 50 * scale,  cy + 220 * scale);
        gc.stroke();
    }

    private void drawGridAndRings(GraphicsContext gc, double cx, double cy, double radius) {
        int numRings = 5;
        gc.setLineWidth(0.8);

        for (int i = 1; i <= numRings; i++) {
            double r = radius * i / numRings;
            gc.setStroke(i == numRings ? GRID_COLOR.brighter() : GRID_SUB);
            gc.strokeOval(cx - r, cy - r, 2 * r, 2 * r);

            // Distance
            gc.setFill(TEXT_MAIN);
            gc.setFont(FONT_SYM);
            gc.fillText(String.format("%.0f km", radar.getMaxRangeKm() * i / numRings), cx + 4, cy - r + 12);
        }

        // Azimuts
        gc.setStroke(GRID_SUB);
        for (int deg = 0; deg < 360; deg += 30) {
            double rad = Math.toRadians(deg - 90);
            gc.strokeLine(cx, cy, cx + radius * Math.cos(rad), cy + radius * Math.sin(rad));
        }

        // Centre
        gc.setStroke(TEXT_HI);
        gc.setLineWidth(1.0);
        gc.strokeLine(cx - 5, cy, cx + 5, cy);
        gc.strokeLine(cx, cy - 5, cx, cy + 5);
    }

    private void drawSweepTrail(GraphicsContext gc, double cx, double cy, double radius) {
        double az = radar.getCurrentAzimuthDeg();
        for (int i = 1; i <= 50; i++) {
            double trailAz = az - i;
            double trailRad = Math.toRadians(trailAz - 90);
            double alpha = 0.12 * (1.0 - i / 50.0);

            gc.setFill(Color.color(0, 1.0, 0.4, alpha));
            double[] xP = {cx, cx + radius * Math.cos(trailRad), cx + radius * Math.cos(Math.toRadians(trailAz - 1 - 90))};
            double[] yP = {cy, cy + radius * Math.sin(trailRad), cy + radius * Math.sin(Math.toRadians(trailAz - 1 - 90))};
            gc.fillPolygon(xP, yP, 3);
        }
    }

    private void drawSweepLine(GraphicsContext gc, double cx, double cy, double radius) {
        double az = radar.getCurrentAzimuthDeg();
        double rad = Math.toRadians(az - 90);
        double endX = cx + radius * Math.cos(rad);
        double endY = cy + radius * Math.sin(rad);

        gc.setStroke(SWEEP_COLOR);
        gc.setLineWidth(1.8);
        gc.strokeLine(cx, cy, endX, endY);
    }

    private void drawTargetOverlay(GraphicsContext gc, double cx, double cy, double radius) {
        for (Target t : targets) {
            if (t.distanceToRadar() > radar.getMaxRangeKm()) continue;

            double[] pos = toScreenCoords(t.getX(), t.getY(), cx, cy, radius);
            double px = pos[0], py = pos[1];

            // Vecteur Vitesse
            double headingRad = Math.toRadians(t.azimuth() - 90);
            double vectorLen = (t.speed() / 100.0) * 8;
            gc.setStroke(Color.web("#00E5FF66"));
            gc.setLineWidth(1.2);
            gc.strokeLine(px, py, px + vectorLen * Math.sin(headingRad), py - vectorLen * Math.cos(headingRad));

            // Symboles
            switch (t.getType()) {
                case AIRCRAFT:
                    gc.setStroke(Color.web("#00E5FF"));
                    gc.strokePolygon(new double[]{px, px - 5, px + 5}, new double[]{py - 6, py + 5, py + 5}, 3);
                    break;
                case DRONE:
                    gc.setStroke(Color.web("#FFCC00"));
                    gc.strokePolygon(new double[]{px, px - 5, px, px + 5}, new double[]{py - 5, py, py + 5, py}, 4);
                    break;
                case MISSILE:
                    gc.setStroke(Color.web("#FF3344"));
                    gc.strokeLine(px - 4, py + 4, px + 4, py - 4);
                    gc.strokeLine(px + 4, py - 4, px, py - 4);
                    gc.strokeLine(px + 4, py - 4, px + 4, py);
                    break;
            }

            gc.setFill(TEXT_MAIN);
            gc.setFont(FONT_SYM);
            gc.fillText(String.format("%s #%d", t.getType().label, t.getId()), px + 8, py + 2);
        }
    }

    private void drawDetections(GraphicsContext gc, double cx, double cy, double radius) {
        for (Detection d : detections) {
            if (d.isFaded()) continue;
            double[] pos = toScreenCoords(d.getX(), d.getY(), cx, cy, radius);
            double op = d.getOpacity();

            if (d.getType() == Detection.DetectionType.TRUE_DETECTION) {
                gc.setFill(Color.color(0, 1.0, 0.4, op));
                gc.fillOval(pos[0] - 3, pos[1] - 3, 6, 6);
                gc.setStroke(Color.color(0, 1.0, 0.4, op * 0.4));
                gc.strokeOval(pos[0] - 6, pos[1] - 6, 12, 12);
            } else {
                gc.setFill(Color.color(1.0, 0.2, 0.2, op * 0.8));
                gc.fillRect(pos[0] - 2, pos[1] - 2, 4, 4);
            }
        }
    }

    private void drawOuterCompass(GraphicsContext gc, double cx, double cy, double r) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r - 4, cy - r - 4, 2 * (r + 4), 2 * (r + 4));

        gc.setFont(FONT_SYM);
        gc.setTextAlign(TextAlignment.CENTER);

        for (int deg = 0; deg < 360; deg += 30) {
            double rad = Math.toRadians(deg - 90);
            double lx = cx + (r + 14) * Math.cos(rad);
            double ly = cy + (r + 14) * Math.sin(rad);

            gc.setFill(deg % 90 == 0 ? TEXT_HI : TEXT_MAIN);
            String label = deg == 0 ? "360°" : deg + "°";
            gc.fillText(label, lx, ly + 4);
        }
    }

    private void drawHUD(GraphicsContext gc, double w, double h) {
        gc.setFont(FONT_HUD);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(TEXT_HI);
        gc.fillText(String.format("AZIMUTH : %05.1f°", radar.getCurrentAzimuthDeg()), 15, 25);

        gc.setFill(TEXT_MAIN);
        gc.fillText(String.format("FREQ : %.1f GHz | λ : %.1f cm", radar.getFrequencyGHz(), radar.getWavelengthCm()), 15, h - 15);
    }

    private double[] toScreenCoords(double wX, double wY, double cx, double cy, double r) {
        double scale = r / radar.getMaxRangeKm();
        return new double[]{ cx + wX * scale, cy - wY * scale };
    }

    public void setShowTargets(boolean b) { this.showTargets = b; }
    public void setShowSweepTrail(boolean b) { this.showSweepTrail = b; }
}