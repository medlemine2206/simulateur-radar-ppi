package view;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.Detection;
import model.Radar;
import model.RadarStats;
import model.Target;

import java.util.List;

public class StatsPanel extends VBox {

    private final Canvas statsCanvas;
    private final Canvas pdCurveCanvas;

    private static final Color BG       = Color.web("#0B1015");
    private static final Color TEXT_MAIN= Color.web("#A0B0C0");
    private static final Color TEXT_HI  = Color.web("#00E5FF");
    private static final Color GREEN    = Color.web("#00FF66");
    private static final Color RED      = Color.web("#FF3344");
    private static final Color YELLOW   = Color.web("#FFCC00");
    private static final Color GRID     = Color.web("#1E2D3D");

    private static final Font FONT_TITLE = Font.font("Segoe UI", 11);
    private static final Font FONT_BODY  = Font.font("Segoe UI", 11);
    private static final Font FONT_SMALL = Font.font("Segoe UI", 10);

    public StatsPanel(double width) {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-background-color: #0B1015; -fx-border-color: #1E2D3D; -fx-border-width: 0 0 0 1;");

        statsCanvas   = new Canvas(width - 20, 240);
        pdCurveCanvas = new Canvas(width - 20, 200);

        getChildren().addAll(statsCanvas, pdCurveCanvas);
    }

    public void update(Radar radar, List<Target> targets, List<Detection> detections, RadarStats stats) {
        drawStats(radar, targets, stats);
        drawPdCurve(radar, stats);
    }

    private void drawStats(Radar radar, List<Target> targets, RadarStats stats) {
        GraphicsContext gc = statsCanvas.getGraphicsContext2D();
        double w = statsCanvas.getWidth();
        double h = statsCanvas.getHeight();

        gc.setFill(BG); gc.fillRect(0, 0, w, h);

        gc.setFill(TEXT_HI); gc.setFont(FONT_TITLE);
        gc.fillText("STATISTIQUES RADAR", 0, 15);

        gc.setStroke(GRID); gc.setLineWidth(1);
        gc.strokeLine(0, 22, w, 22);

        gc.setFont(FONT_BODY);
        double y = 42;

        drawRow(gc, "Pd mesurée :", String.format("%.3f  (%.1f%%)", stats.getMeasuredPd(), stats.getMeasuredPd() * 100), pdColor(stats.getMeasuredPd()), y);
        y += 20;
        drawRow(gc, "Pfa mesurée :", String.format("%.4f", stats.getMeasuredPfa()), stats.getMeasuredPfa() > 0.05 ? RED : YELLOW, y);
        y += 20;
        drawRow(gc, "SNR moyen :", String.format("%.1f dB", stats.getMeanSnrDB()), stats.getMeanSnrDB() > 10 ? GREEN : YELLOW, y);
        y += 20;
        drawRow(gc, "Vrais détect. :", String.valueOf(stats.getTrueDetections()), TEXT_HI, y);
        y += 20;
        drawRow(gc, "Fausses alarm. :", String.valueOf(stats.getFalseAlarms()), RED, y);
        y += 20;
        drawRow(gc, "Opportunités :", String.valueOf(stats.getTotalOpportunities()), TEXT_MAIN, y);
        y += 25;

        // Cibles actives
        gc.setFill(TEXT_HI); gc.setFont(FONT_TITLE);
        gc.fillText("CIBLES ACTIVES (" + targets.size() + ")", 0, y);
        y += 10;
        gc.strokeLine(0, y, w, y);
        y += 15;

        gc.setFont(FONT_SMALL);
        for (int i = 0; i < Math.min(4, targets.size()); i++) {
            Target t = targets.get(i);
            gc.setFill(t.isDetected() ? GREEN : YELLOW);
            gc.fillText(String.format("%-7s %5.1fkm  %4.0fkm/h  Az%4.1f°",
                    t.getType().label, t.distanceToRadar(), t.speed(), t.azimuth()), 0, y);
            y += 15;
        }
    }

    private void drawRow(GraphicsContext gc, String label, String val, Color valColor, double y) {
        gc.setFill(TEXT_MAIN);
        gc.fillText(label, 0, y);
        gc.setFill(valColor);
        gc.fillText(val, 130, y);
    }

    private void drawPdCurve(Radar radar, RadarStats stats) {
        GraphicsContext gc = pdCurveCanvas.getGraphicsContext2D();
        double w = pdCurveCanvas.getWidth();
        double h = pdCurveCanvas.getHeight();

        gc.setFill(BG); gc.fillRect(0, 0, w, h);

        double pL = 30, pR = 15, pT = 25, pB = 30;
        double plotW = w - pL - pR;
        double plotH = h - pT - pB;

        gc.setFill(TEXT_HI); gc.setFont(FONT_TITLE);
        gc.fillText("Pd vs Distance", 0, 15);

        // Légende
        gc.setStroke(TEXT_HI); gc.setLineWidth(1.5);
        gc.strokeLine(pL + 100, 11, pL + 115, 11);
        gc.setFill(TEXT_MAIN); gc.setFont(FONT_SMALL);
        gc.fillText("Théorique", pL + 120, 15);

        gc.setFill(GREEN);
        gc.fillOval(pL + 175, 11, 5, 5);
        gc.fillText("Mesurée", pL + 183, 15);

        // Grille & Axes
        gc.setStroke(GRID); gc.setLineWidth(1);
        gc.strokeRect(pL, pT, plotW, plotH);

        // Graduation Y
        gc.setFont(FONT_SMALL); gc.setFill(TEXT_MAIN);
        for (double v = 0.0; v <= 1.0; v += 0.5) {
            double y = pT + plotH - (v * plotH);
            gc.fillText(String.format("%.1f", v), 2, y + 4);
        }

        // Graduation X (Portée)
        double maxR = radar.getMaxRangeKm();
        for (int i = 0; i <= 4; i++) {
            double x = pL + (plotW * i / 4.0);
            double val = maxR * i / 4.0;
            gc.fillText(String.format("%.0f", val), x - 8, h - 10);
        }
        gc.fillText("km", w - 18, h - 10);

        // Courbe Théorique
        gc.setStroke(TEXT_HI); gc.setLineWidth(1.5);
        gc.beginPath();
        boolean first = true;
        for (int px = 0; px < plotW; px++) {
            double range = maxR * px / plotW;
            if (range < 1) continue;
            double snr = radar.computeSNR(range, 3.0);
            double pd = radar.computePd(snr);
            double x = pL + px;
            double y = pT + plotH - pd * plotH;
            if (first) { gc.moveTo(x, y); first = false; } else gc.lineTo(x, y);
        }
        gc.stroke();

        // Points Mesurés
        gc.setFill(GREEN);
        for (int b = 0; b < 10; b++) {
            double pdM = stats.getPdByRange(b);
            double r = stats.getRangeBucketCenter(b);
            if (r > maxR) continue;
            double x = pL + (r / maxR) * plotW;
            double y = pT + plotH - pdM * plotH;
            gc.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    private Color pdColor(double pd) {
        return pd > 0.8 ? GREEN : pd > 0.5 ? YELLOW : RED;
    }
}
