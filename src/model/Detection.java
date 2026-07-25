package model;

/**
 * Représente une détection par le radar.
 * Peut être une vraie détection ou une fausse alarme.
 */
public class Detection {

    public enum DetectionType { TRUE_DETECTION, FALSE_ALARM }

    private final double rangeKm;
    private final double azimuthDeg;
    private final double snr;
    private final double pd;
    private final DetectionType type;
    private final Target target; // null si fausse alarme
    private final long   timestamp;
    private       int    age; // frames depuis la détection (pour le fade)

    public Detection(double rangeKm, double azimuthDeg,
                     double snr, double pd,
                     DetectionType type, Target target) {
        this.rangeKm    = rangeKm;
        this.azimuthDeg = azimuthDeg;
        this.snr        = snr;
        this.pd         = pd;
        this.type       = type;
        this.target     = target;
        this.timestamp  = System.currentTimeMillis();
        this.age        = 0;
    }

    /** Coordonnée X en km (pour affichage cartésien). */
    public double getX() {
        return rangeKm * Math.sin(Math.toRadians(azimuthDeg));
    }

    /** Coordonnée Y en km (pour affichage cartésien). */
    public double getY() {
        return rangeKm * Math.cos(Math.toRadians(azimuthDeg));
    }

    /** SNR en dB. */
    public double getSnrDB() {
        return snr > 0 ? 10 * Math.log10(snr) : -99;
    }

    /** Opacité pour le fade (diminue avec l'âge). */
    public double getOpacity() {
        return Math.max(0.0, 1.0 - age / 50.0);
    }

    public void incrementAge() { age++; }
    public boolean isFaded()   { return age > 50; }

    // ── Getters ──
    public double        getRangeKm()    { return rangeKm; }
    public double        getAzimuthDeg() { return azimuthDeg; }
    public double        getSnr()        { return snr; }
    public double        getPd()         { return pd; }
    public DetectionType getType()       { return type; }
    public Target        getTarget()     { return target; }
    public long          getTimestamp()  { return timestamp; }
    public int           getAge()        { return age; }

    @Override
    public String toString() {
        return String.format("[%s] R=%.1f km Az=%.1f° SNR=%.1f dB Pd=%.2f",
            type, rangeKm, azimuthDeg, getSnrDB(), pd);
    }
}
