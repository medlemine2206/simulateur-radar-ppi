package model;

/**
 * Représente une cible détectable par le radar.
 * Position en coordonnées cartésiennes (x, y) en kilomètres.
 * La cible se déplace selon une vitesse (vx, vy) en km/s.
 */
public class Target {

    public enum TargetType {
        AIRCRAFT("Avion",   3.0),
        DRONE   ("Drone",   1.5),
        MISSILE ("Missile", 5.0),
        BIRD    ("Oiseau",  0.3);

        public final String label;
        public final double radarCrossSection; // RCS en m² — plus grand = plus facile à détecter

        TargetType(String label, double rcs) {
            this.label = label;
            this.radarCrossSection = rcs;
        }
    }

    private final int    id;
    private       double x;          // km
    private       double y;          // km
    private       double vx;         // km/s
    private       double vy;         // km/s
    private final TargetType type;
    private       boolean detected;
    private       int     detectionCount;
    private       int     missCount;

    public Target(int id, double x, double y, double vx, double vy, TargetType type) {
        this.id   = id;
        this.x    = x;
        this.y    = y;
        this.vx   = vx;
        this.vy   = vy;
        this.type = type;
        this.detected = false;
        this.detectionCount = 0;
        this.missCount = 0;
    }

    /** Met à jour la position selon le temps écoulé (en secondes). */
    public void update(double dt) {
        x += vx * dt;
        y += vy * dt;
    }

    /** Distance à l'origine (radar) en km. */
    public double distanceToRadar() {
        return Math.sqrt(x * x + y * y);
    }

    /** Azimut en degrés (0° = Nord, sens horaire). */
    public double azimuth() {
        double angle = Math.toDegrees(Math.atan2(x, y));
        return (angle + 360) % 360;
    }

    /** Vitesse scalaire en km/h. */
    public double speed() {
        return Math.sqrt(vx * vx + vy * vy) * 3600;
    }

    // ── Getters / Setters ──
    public int       getId()             { return id; }
    public double    getX()              { return x; }
    public double    getY()              { return y; }
    public double    getVx()             { return vx; }
    public double    getVy()             { return vy; }
    public TargetType getType()          { return type; }
    public boolean   isDetected()        { return detected; }
    public int       getDetectionCount() { return detectionCount; }
    public int       getMissCount()      { return missCount; }

    public void setDetected(boolean d) {
        this.detected = d;
        if (d) detectionCount++;
        else   missCount++;
    }

    public void setVx(double vx) { this.vx = vx; }
    public void setVy(double vy) { this.vy = vy; }

    @Override
    public String toString() {
        return String.format("[%s #%d] pos=(%.1f, %.1f) km | v=%.0f km/h | dist=%.1f km",
            type.label, id, x, y, speed(), distanceToRadar());
    }
}
