package model;

/**
 * Modèle physique du radar.
 *
 * Implémente l'équation radar fondamentale (Radar Range Equation) :
 *
 *   SNR = (P_t × G² × λ² × σ) / ((4π)³ × R⁴ × k × T × B × F)
 *
 * Paramètres :
 *   P_t : puissance d'émission (W)
 *   G   : gain d'antenne (linéaire)
 *   λ   : longueur d'onde (m)
 *   σ   : section efficace radar de la cible (RCS, m²)
 *   R   : distance cible-radar (m)
 *   k   : constante de Boltzmann (1.38e-23)
 *   T   : température système (K)
 *   B   : bande passante (Hz)
 *   F   : facteur de bruit
 *
 * Puis on calcule la probabilité de détection selon le SNR :
 *   Pd = Q(sqrt(2*SNR) - sqrt(-2*ln(Pfa))) où Q est la fonction Q gaussienne
 */
public class Radar {

    // ── Paramètres physiques du radar ──
    private double peakPower;        // Puissance crête (W)
    private double antennaGainDB;    // Gain antenne (dB)
    private double frequencyGHz;     // Fréquence (GHz)
    private double noiseFigureDB;    // Facteur de bruit (dB)
    private double bandwidthMHz;     // Bande passante (MHz)
    private double maxRangeKm;       // Portée max affichée (km)
    private double rotationSpeedRPM; // Vitesse de rotation (tours/min)
    private double pfa;              // Probabilité de fausse alarme

    // ── État courant ──
    private double currentAzimuthDeg; // Azimut courant de l'antenne (°)

    // ── Constantes physiques ──
    private static final double SPEED_OF_LIGHT = 3e8;   // m/s
    private static final double BOLTZMANN      = 1.38e-23;
    private static final double TEMP_KELVIN    = 290.0; // température système standard

    public Radar() {
        // Paramètres inspirés d'un radar de surveillance aérienne (type GMA400)
        this.peakPower        = 500_000; // 500 kW
        this.antennaGainDB    = 33.0;    // 33 dB
        this.frequencyGHz     = 3.0;     // bande S (2-4 GHz)
        this.noiseFigureDB    = 5.0;     // 5 dB
        this.bandwidthMHz     = 1.0;     // 1 MHz
        this.maxRangeKm       = 400.0;   // 400 km
        this.rotationSpeedRPM = 6.0;     // 6 tours/min = 10 secondes/tour
        this.pfa              = 1e-6;    // 1 fausse alarme par million
        this.currentAzimuthDeg = 0.0;
    }

    // ── Calculs physiques ──

    /** Longueur d'onde en mètres. */
    public double wavelength() {
        return SPEED_OF_LIGHT / (frequencyGHz * 1e9);
    }

    /** Gain antenne en linéaire. */
    public double antennaGainLinear() {
        return Math.pow(10, antennaGainDB / 10.0);
    }

    /** Facteur de bruit en linéaire. */
    public double noiseFigureLinear() {
        return Math.pow(10, noiseFigureDB / 10.0);
    }

    /**
     * Calcule le SNR reçu pour une cible donnée.
     * Équation radar : SNR = (Pt × G² × λ² × σ) / ((4π)³ × R⁴ × k × T × B × F)
     *
     * @param rangeKm  Distance de la cible en km
     * @param rcs      Section efficace radar de la cible en m²
     * @return SNR linéaire (sans unité)
     */
    public double computeSNR(double rangeKm, double rcs) {
        double R    = rangeKm * 1000.0;          // m
        double G    = antennaGainLinear();
        double lam  = wavelength();              // m
        double B    = bandwidthMHz * 1e6;        // Hz
        double F    = noiseFigureLinear();

        double numerator   = peakPower * G * G * lam * lam * rcs;
        double denominator = Math.pow(4 * Math.PI, 3)
                           * Math.pow(R, 4)
                           * BOLTZMANN * TEMP_KELVIN * B * F;

        return numerator / denominator;
    }

    /**
     * Calcule la probabilité de détection (Pd) pour un SNR donné.
     *
     * Modèle de Albersheim (approximation de la courbe ROC) :
     *   A = ln(0.62 / Pfa)
     *   B = ln(Pd / (1 - Pd))
     *   SNR_dB = -5*log10(A) + (6.2 + 4.54/sqrt(n+0.44)) * log10(B + 0.12*A + 1.7)
     *
     * On inverse ici pour obtenir Pd à partir du SNR.
     * Approximation : Pd = Q(Q⁻¹(Pfa) - sqrt(2*SNR))
     *
     * @param snr SNR linéaire
     * @return Probabilité de détection entre 0 et 1
     */
    public double computePd(double snr) {
        if (snr <= 0) return 0.0;

        // Seuil de détection : Q(threshold) = Pfa
        // threshold = Q_inv(Pfa)
        double threshold = qInverse(pfa);

        // SNR en amplitude (pas en puissance pour la formule)
        double snrAmplitude = Math.sqrt(2.0 * snr);

        // Pd = Q(threshold - snrAmplitude)
        double pd = qFunction(threshold - snrAmplitude);

        return Math.max(0.0, Math.min(1.0, pd));
    }

    /**
     * Simule une détection pour une cible donnée.
     * Tire un nombre aléatoire et le compare à Pd.
     *
     * @param target La cible à détecter
     * @return true si détectée
     */
    public boolean simulateDetection(Target target) {
        double range = target.distanceToRadar();
        if (range > maxRangeKm || range < 0.1) return false;

        double snr = computeSNR(range, target.getType().radarCrossSection);
        double pd  = computePd(snr);

        // Tirage de Bernoulli : détection avec probabilité pd
        boolean detected = Math.random() < pd;

        // Fausse alarme possible même sans cible (simulée séparément)
        return detected;
    }

    /**
     * Simule une fausse alarme dans une cellule donnée.
     * @return true si fausse alarme
     */
    public boolean simulateFalseAlarm() {
        return Math.random() < pfa * 1000; // normalisé pour affichage
    }

    // ── Mises à jour ──

    /**
     * Met à jour l'azimut de l'antenne.
     * @param dtSeconds temps écoulé en secondes
     */
    public void updateAzimuth(double dtSeconds) {
        double degreesPerSecond = rotationSpeedRPM * 360.0 / 60.0;
        currentAzimuthDeg = (currentAzimuthDeg + degreesPerSecond * dtSeconds) % 360.0;
    }

    // ── Fonctions mathématiques ──

    /**
     * Fonction Q gaussienne : Q(x) = P(Z > x) pour Z ~ N(0,1)
     * Approximation précise (erreur < 1.5e-7)
     */
    public static double qFunction(double x) {
        return 0.5 * erfc(x / Math.sqrt(2));
    }

    /**
     * Inverse de la fonction Q : Q⁻¹(p)
     * Approximation de Beasley-Springer-Moro
     */
    public static double qInverse(double p) {
        if (p <= 0) return Double.POSITIVE_INFINITY;
        if (p >= 1) return Double.NEGATIVE_INFINITY;
        return -inverseNormalCDF(p);
    }

    /** erfc(x) = 1 - erf(x) — approximation précise */
    private static double erfc(double x) {
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
        double poly = t * (0.254829592
                   + t * (-0.284496736
                   + t * (1.421413741
                   + t * (-1.453152027
                   + t * 1.061405429))));
        double result = poly * Math.exp(-x * x);
        return x >= 0 ? result : 2.0 - result;
    }

    /** Inverse de la CDF normale standard — algorithme de Beasley-Springer */
    private static double inverseNormalCDF(double p) {
        double[] a = {-3.969683028665376e+01,  2.209460984245205e+02,
                      -2.759285104469687e+02,   1.383577518672690e+02,
                      -3.066479806614716e+01,   2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01,  1.615858368580409e+02,
                      -1.556989798598866e+02,   6.680131188771972e+01,
                      -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01,
                      -2.400758277161838e+00, -2.549732539343734e+00,
                       4.374664141464968e+00,  2.938163982698783e+00};
        double[] d = { 7.784695709041462e-03,  3.224671290700398e-01,
                       2.445134137142996e+00,  3.754408661907416e+00};

        double pLow  = 0.02425;
        double pHigh = 1.0 - pLow;
        double q, r, x = 0;

        if (p < pLow) {
            q = Math.sqrt(-2 * Math.log(p));
            x = (((((c[0]*q+c[1])*q+c[2])*q+c[3])*q+c[4])*q+c[5]) /
                ((((d[0]*q+d[1])*q+d[2])*q+d[3])*q+1);
        } else if (p <= pHigh) {
            q = p - 0.5;
            r = q * q;
            x = (((((a[0]*r+a[1])*r+a[2])*r+a[3])*r+a[4])*r+a[5])*q /
                (((((b[0]*r+b[1])*r+b[2])*r+b[3])*r+b[4])*r+1);
        } else {
            q = Math.sqrt(-2 * Math.log(1 - p));
            x = -(((((c[0]*q+c[1])*q+c[2])*q+c[3])*q+c[4])*q+c[5]) /
                 ((((d[0]*q+d[1])*q+d[2])*q+d[3])*q+1);
        }
        return x;
    }

    // ── Getters / Setters ──
    public double getMaxRangeKm()        { return maxRangeKm; }
    public double getCurrentAzimuthDeg() { return currentAzimuthDeg; }
    public double getRotationSpeedRPM()  { return rotationSpeedRPM; }
    public double getPeakPower()         { return peakPower; }
    public double getFrequencyGHz()      { return frequencyGHz; }
    public double getAntennaGainDB()     { return antennaGainDB; }
    public double getPfa()               { return pfa; }
    public double getWavelengthCm()      { return wavelength() * 100; }

    public void setMaxRangeKm(double r)        { this.maxRangeKm = r; }
    public void setRotationSpeedRPM(double s)  { this.rotationSpeedRPM = s; }
    public void setPeakPower(double p)         { this.peakPower = p; }
    public void setFrequencyGHz(double f)      { this.frequencyGHz = f; }
    public void setAntennaGainDB(double g)     { this.antennaGainDB = g; }
    public void setPfa(double pfa)             { this.pfa = pfa; }
    public void setCurrentAzimuthDeg(double a) { this.currentAzimuthDeg = a; }
}
