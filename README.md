# Simulateur Radar PPI 

**Mohamed Lemine Ahmed Jeddou | CY Tech | 2026**

Simulateur d'un radar de surveillance aérienne de type PPI (Plan Position Indicator),
inspiré des radars Thales (GMA400, GM200). Implémente la physique réelle du radar.

---

## Physique implémentée

### Équation radar fondamentale
```
SNR = (Pt × G² × λ² × σ) / ((4π)³ × R⁴ × k × T × B × F)
```

| Symbole | Signification             | Valeur par défaut |
|---------|--------------------------|-------------------|
| Pt      | Puissance crête           | 500 kW            |
| G       | Gain antenne              | 33 dB             |
| λ       | Longueur d'onde           | 10 cm (bande S)   |
| σ       | Section efficace (RCS)    | 3 m² (avion)      |
| R       | Distance cible-radar      | variable          |
| k       | Constante Boltzmann       | 1.38×10⁻²³        |
| T       | Température système       | 290 K             |
| B       | Bande passante            | 1 MHz             |
| F       | Facteur de bruit          | 5 dB              |

### Probabilité de détection
```
Pd = Q(Q⁻¹(Pfa) - √(2×SNR))
```

- Q : fonction Q gaussienne (queue de la distribution normale)
- Pfa : probabilité de fausse alarme (réglable, défaut 10⁻⁶)
- Plus le SNR est élevé, plus Pd → 1

### Bruit de mesure
- Erreur de distance : bruit gaussien σ = 0.5% de la portée
- Erreur d'azimut : bruit gaussien σ = 0.3°
- Généré par la méthode Box-Muller

---

## Architecture MVC

```
src/
├── Main.java                          # Point d'entrée JavaFX
├── model/
│   ├── Radar.java                     # Physique radar (SNR, Pd, Pfa)
│   ├── Target.java                    # Cibles (position, vitesse, RCS)
│   ├── Detection.java                 # Résultat de détection
│   └── RadarStats.java                # Statistiques temps réel
├── view/
│   ├── RadarDisplay.java              # Écran PPI (Canvas JavaFX)
│   ├── StatsPanel.java                # Courbe Pd, statistiques
│   └── ControlPanel.java             # Contrôles utilisateur
└── controller/
    └── RadarController.java           # Boucle simulation, détection
```

---

## Installation et compilation

### Prérequis
- Java 17+
- JavaFX SDK 17+ (télécharger sur https://gluonhq.com/products/javafx/)

### Compilation (ligne de commande)
```bash
# Depuis le dossier RadarSimulator/
javac --module-path /chemin/vers/javafx/lib \
      --add-modules javafx.controls,javafx.graphics \
      -d out \
      src/model/*.java src/view/*.java src/controller/*.java src/Main.java
```

### Exécution
```bash
java --module-path /chemin/vers/javafx/lib \
     --add-modules javafx.controls,javafx.graphics \
     -cp out Main
```

### Avec IntelliJ IDEA (recommandé)
1. File → New Project → Java
2. Copier tous les fichiers src/ dans le projet
3. File → Project Structure → Libraries → ajouter JavaFX lib/
4. Run → Edit Configurations → VM options :
   `--module-path /chemin/javafx/lib --add-modules javafx.controls,javafx.graphics`
5. Run Main

---

## Fonctionnalités

### Écran PPI
- Affichage circulaire type radar réel (fond noir/vert)
- Faisceau rotatif avec traîne lumineuse
- Anneaux de distance (5 cercles concentriques)
- Lignes d'azimut tous les 30°
- Points de détection avec fade progressif
- Fausses alarmes en rouge

### Statistiques temps réel
- Pd mesurée vs Pd théorique (courbe)
- Pfa mesurée
- SNR moyen en dB
- Compteurs de détections / fausses alarmes
- Liste des cibles actives

### Contrôles
- Vitesse de rotation (1–20 tr/min)
- Puissance d'émission (100 kW – 2 MW)
- Gain antenne (20–50 dB)
- Portée maximale (50–500 km)
- Probabilité de fausse alarme
- Ajout/suppression de cibles (avion, drone, missile)
- Mode debug (affichage vraies positions)

---

## Types de cibles

| Type    | RCS (m²) | Vitesse typique |
|---------|----------|----------------|
| Avion   | 3.0      | 800–1200 km/h  |
| Drone   | 1.5      | 180–360 km/h   |
| Missile | 5.0      | 2500–3600 km/h |
| Oiseau  | 0.3      | 30–100 km/h    |

---


