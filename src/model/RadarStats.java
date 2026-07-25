package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcule et maintient les statistiques en temps réel du radar.
 * Pd mesurée, Pfa mesurée, SNR moyen, histogramme des distances...
 */
public class RadarStats {

    private int totalDetections;
    private int trueDetections;
    private int falseAlarms;
    private int totalOpportunities;

    // Historique SNR pour calcul de moyenne glissante
    private final List<Double> snrHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 100;

    // Histogramme des détections par distance (10 tranches de 40 km)
    private final int[] detectionByRange = new int[10];
    private final int[] opportunityByRange = new int[10];

    public void recordDetection(Detection d) {
        totalOpportunities++;
        if (d.getType() == Detection.DetectionType.TRUE_DETECTION) {
            totalDetections++;
            trueDetections++;
            // Histogramme
            int bucket = (int) Math.min(d.getRangeKm() / 40.0, 9);
            detectionByRange[bucket]++;
            opportunityByRange[bucket]++;
            // SNR history
            snrHistory.add(d.getSnr());
            if (snrHistory.size() > MAX_HISTORY)
                snrHistory.remove(0);
        } else {
            falseAlarms++;
            totalDetections++;
        }
    }

    public void recordMiss(double rangeKm) {
        totalOpportunities++;
        int bucket = (int) Math.min(rangeKm / 40.0, 9);
        opportunityByRange[bucket]++;
    }

    /** Pd mesurée = détections vraies / opportunités totales */
    public double getMeasuredPd() {
        if (totalOpportunities == 0) return 0;
        return (double) trueDetections / totalOpportunities;
    }

    /** Pfa mesurée = fausses alarmes / total */
    public double getMeasuredPfa() {
        if (totalDetections == 0) return 0;
        return (double) falseAlarms / totalDetections;
    }

    /** SNR moyen en dB sur les 100 dernières détections */
    public double getMeanSnrDB() {
        if (snrHistory.isEmpty()) return 0;
        double sum = snrHistory.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / snrHistory.size();
        return mean > 0 ? 10 * Math.log10(mean) : -99;
    }

    /** Pd mesurée par tranche de distance (pour affichage courbe) */
    public double getPdByRange(int bucket) {
        if (bucket < 0 || bucket >= 10) return 0;
        if (opportunityByRange[bucket] == 0) return 0;
        return (double) detectionByRange[bucket] / opportunityByRange[bucket];
    }

    /** Distance centrale de la tranche (en km) */
    public double getRangeBucketCenter(int bucket) {
        return (bucket + 0.5) * 40.0;
    }

    public void reset() {
        totalDetections    = 0;
        trueDetections     = 0;
        falseAlarms        = 0;
        totalOpportunities = 0;
        snrHistory.clear();
        for (int i = 0; i < 10; i++) {
            detectionByRange[i]    = 0;
            opportunityByRange[i]  = 0;
        }
    }

    // Getters
    public int getTotalDetections()    { return totalDetections; }
    public int getTrueDetections()     { return trueDetections; }
    public int getFalseAlarms()        { return falseAlarms; }
    public int getTotalOpportunities() { return totalOpportunities; }
}
