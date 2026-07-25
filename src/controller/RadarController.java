package controller;

import javafx.animation.AnimationTimer;
import model.*;
import view.ControlPanel;
import view.RadarDisplay;
import view.StatsPanel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Contrôleur principal du simulateur radar.
 * Orchestre la boucle de simulation, la détection et la mise à jour des vues.
 *
 * Boucle principale (60 fps via AnimationTimer) :
 * 1. Mettre à jour l'azimut du radar
 * 2. Déplacer les cibles
 * 3. Pour chaque cible dans le faisceau → simuler détection
 * 4. Nettoyer les détections obsolètes
 * 5. Redessiner les vues
 */
public class RadarController {

    private final Radar        radar;
    private final List<Target> targets     = new ArrayList<>();
    private final List<Detection> detections = new ArrayList<>();
    private final RadarStats   stats       = new RadarStats();
    private final RadarDisplay display;
    private final StatsPanel   statsPanel;
    private final Random       rng         = new Random();

    private AnimationTimer animationTimer;
    private long           lastNanoTime    = -1;
    private int            targetIdCounter = 1;

    // Paramètres de simulation
    private static final double BEAM_WIDTH_DEG = 1.5; // largeur faisceau (°)

    public RadarController(RadarDisplay display, StatsPanel statsPanel) {
        this.display    = display;
        this.statsPanel = statsPanel;
        this.radar      = new Radar();

        // Initialiser avec quelques cibles
        addTarget(Target.TargetType.AIRCRAFT);
        addTarget(Target.TargetType.AIRCRAFT);
        addTarget(Target.TargetType.DRONE);
    }

    /** Configure les callbacks du panneau de contrôle. */
    public void bindControlPanel(ControlPanel cp) {
        cp.setOnRadarChanged(newRadar -> {
            radar.setRotationSpeedRPM(newRadar.getRotationSpeedRPM());
            radar.setPeakPower(newRadar.getPeakPower());
            radar.setAntennaGainDB(newRadar.getAntennaGainDB());
            radar.setMaxRangeKm(newRadar.getMaxRangeKm());
            radar.setPfa(newRadar.getPfa());
        });
        cp.setOnAddAircraft(()  -> addTarget(Target.TargetType.AIRCRAFT));
        cp.setOnAddDrone(()     -> addTarget(Target.TargetType.DRONE));
        cp.setOnAddMissile(()   -> addTarget(Target.TargetType.MISSILE));
        cp.setOnClearTargets(() -> { targets.clear(); detections.clear(); });
        cp.setOnResetStats(()   -> stats.reset());
        cp.setOnShowTargets(b   -> display.setShowTargets(b));
        cp.setOnShowSweepTrail(b-> display.setShowSweepTrail(b));
    }

    /** Démarre la boucle d'animation. */
    public void start() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long nowNano) {
                if (lastNanoTime < 0) { lastNanoTime = nowNano; return; }
                double dt = (nowNano - lastNanoTime) / 1e9; // secondes
                lastNanoTime = nowNano;
                update(dt);
            }
        };
        animationTimer.start();
    }

    /** Arrête la simulation. */
    public void stop() {
        if (animationTimer != null) animationTimer.stop();
    }

    // ── Boucle principale ──

    private void update(double dt) {
        // 1. Mise à jour azimut radar
        double previousAz = radar.getCurrentAzimuthDeg();
        radar.updateAzimuth(dt);
        double currentAz = radar.getCurrentAzimuthDeg();

        // 2. Déplacement des cibles
        updateTargets(dt);

        // 3. Détection dans le faisceau
        processDetections(previousAz, currentAz);

        // 4. Vieillissement et nettoyage des détections
        updateDetections();

        // 5. Rendu
        display.draw(radar, targets, detections);
        statsPanel.update(radar, targets, detections, stats);
    }

    /** Met à jour les positions des cibles et gère les sorties de zone. */
    private void updateTargets(double dt) {
        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target t = it.next();
            t.update(dt);
            // Si la cible sort de 120% de la portée max → la retirer
            if (t.distanceToRadar() > radar.getMaxRangeKm() * 1.2) {
                it.remove();
            }
        }
    }

    /**
     * Simule les détections pour les cibles dans le faisceau entre previousAz et currentAz.
     */
    private void processDetections(double prevAz, double currAz) {
        double deltaAz = angleDiff(currAz, prevAz);
        if (Math.abs(deltaAz) < 0.01) return;

        for (Target target : targets) {
            double targetAz = target.azimuth();
            double range    = target.distanceToRadar();

            if (range > radar.getMaxRangeKm() || range < 0.5) continue;

            // Vérifier si la cible est dans le faisceau balayé
            if (isInSweep(targetAz, prevAz, currAz, deltaAz)) {
                // Simuler la détection
                boolean detected = radar.simulateDetection(target);
                target.setDetected(detected);

                double snr = radar.computeSNR(range, target.getType().radarCrossSection);
                double pd  = radar.computePd(snr);

                if (detected) {
                    // Ajouter bruit de mesure gaussien (erreur de position)
                    double rangeNoise = gaussianNoise(0, range * 0.005); // 0.5% d'erreur
                    double azNoise    = gaussianNoise(0, 0.3);            // 0.3° d'erreur

                    Detection d = new Detection(
                        range + rangeNoise,
                        targetAz + azNoise,
                        snr, pd,
                        Detection.DetectionType.TRUE_DETECTION,
                        target);
                    detections.add(d);
                    stats.recordDetection(d);
                } else {
                    stats.recordMiss(range);
                }

                // Fausse alarme possible dans cette cellule
                if (radar.simulateFalseAlarm()) {
                    double faRange = range * (0.8 + rng.nextDouble() * 0.4);
                    double faAz    = targetAz + gaussianNoise(0, 5);
                    Detection fa = new Detection(
                        faRange, faAz, 0, 0,
                        Detection.DetectionType.FALSE_ALARM,
                        null);
                    detections.add(fa);
                    stats.recordDetection(fa);
                }
            }
        }
    }

    /** Vieillit les détections et supprime celles qui sont trop anciennes. */
    private void updateDetections() {
        Iterator<Detection> it = detections.iterator();
        while (it.hasNext()) {
            Detection d = it.next();
            d.incrementAge();
            if (d.isFaded()) it.remove();
        }
    }

    // ── Utilitaires géométriques ──

    /**
     * Vérifie si un azimut est dans la zone balayée entre prevAz et currAz.
     * Gère le cas du passage par 360°/0°.
     */
    private boolean isInSweep(double targetAz, double prevAz, double currAz, double delta) {
        // Normaliser les angles
        double relPrev   = ((targetAz - prevAz) % 360 + 360) % 360;
        double relCurr   = ((targetAz - currAz) % 360 + 360) % 360;
        double beamHalf  = BEAM_WIDTH_DEG / 2.0;

        return relPrev <= Math.abs(delta) + beamHalf || relCurr <= beamHalf;
    }

    /**
     * Différence angulaire signée (toujours dans [0, 360[).
     */
    private double angleDiff(double a, double b) {
        double diff = (a - b) % 360;
        return diff < 0 ? diff + 360 : diff;
    }

    /**
     * Génère un bruit gaussien (Box-Muller).
     */
    private double gaussianNoise(double mean, double sigma) {
        double u1 = rng.nextDouble();
        double u2 = rng.nextDouble();
        double z  = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return mean + sigma * z;
    }

    // ── Gestion des cibles ──

    /** Ajoute une cible aléatoire dans la zone de portée. */
    public void addTarget(Target.TargetType type) {
        double maxR  = radar.getMaxRangeKm() * 0.85;
        double range = 50 + rng.nextDouble() * (maxR - 50);
        double az    = rng.nextDouble() * 360;
        double x     = range * Math.sin(Math.toRadians(az));
        double y     = range * Math.cos(Math.toRadians(az));

        // Vitesse selon le type
        double speed;
        switch (type) {
            case MISSILE:  speed = rng.nextDouble() * 0.3 + 0.7; break; // 0.7-1.0 km/s
            case AIRCRAFT: speed = rng.nextDouble() * 0.15 + 0.2; break; // 0.2-0.35 km/s
            case DRONE:    speed = rng.nextDouble() * 0.05 + 0.05; break; // 0.05-0.1 km/s
            default:       speed = rng.nextDouble() * 0.03 + 0.01; break;
        }

        double heading = rng.nextDouble() * 360;
        double vx = speed * Math.sin(Math.toRadians(heading));
        double vy = speed * Math.cos(Math.toRadians(heading));

        targets.add(new Target(targetIdCounter++, x, y, vx, vy, type));
    }

    // Getters
    public Radar           getRadar()      { return radar; }
    public List<Target>    getTargets()    { return targets; }
    public List<Detection> getDetections() { return detections; }
    public RadarStats      getStats()      { return stats; }
}
