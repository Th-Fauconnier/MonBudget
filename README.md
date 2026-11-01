# MonBudget 💰

> Application de bureau moderne et intuitive pour la gestion de finances personnelles, développée sous JavaFX 21.

![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue.svg)
![SQLite](https://img.shields.io/badge/Database-SQLite%20(WAL)-lightgrey.svg)
![Flyway](https://img.shields.io/badge/Migrations-Flyway%2010-red.svg)
![BootstrapFX](https://img.shields.io/badge/UI-BootstrapFX%200.4.0-purple.svg)
![AssertJ](https://img.shields.io/badge/AssertJ-3.27.7-red.svg)
![License](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Java CI with Maven](https://github.com/Th-Fauconnier/monbudget-java/actions/workflows/maven.yml/badge.svg)

---

## 📌 Présentation

**MonBudget** est une application locale autonome conçue pour les particuliers souhaitant piloter leur budget simplement, sans jargon comptable complexe (pas de partie double ni d'écritures fiscales).

### Fonctionnalités principales :
- 📚 **Livres & comptes multiples :** isolez et organisez vos finances par domaine (*Vie courante*, *Épargne*, *Vacances*, *Travaux*) et basculez d'un compte à l'autre en un clic depuis le bandeau principal.
- ⏱️ **Réalisé vs Prévisionnel :** suivez vos dépenses effectives tout en anticipant vos flux récurrents, salaires et factures à venir.
- 📊 **Visualisations claires :**
  - Trajectoire de trésorerie et solde projeté via un graphique linéaire (`LineChart`).
  - Ventilation des charges par catégorie avec infobulles monétaires (`PieChart`).
- 🏷️ **Gestion des catégories :** créez et organisez des catégories typées (*Dépense* ou *Revenu*) avec palette de couleurs personnalisée et support des catégories parentes.
- 🔮 **Simulateur budgétaire :** testez des scénarios d'épargne programmée ou l'impact d'achats importants sans altérer vos données réelles.
- 🔒 **100 % local & confidentiel :** vos données restent strictement sur votre machine dans une base SQLite embarquée (zéro télémétrie, aucun service cloud requis).

---

## 🛠️ Stack technique

- **Langage :** Java 21 LTS
- **Interface graphique :** [JavaFX 21.0.2](https://openjfx.io/) (Contrôles natifs, FXML, CSS)
- **Composants d'interface :**
  - [BootstrapFX 0.4.0](https://github.com/kordamp/bootstrapfx) (thème CSS moderne et réactif)
  - [Ikonli 12.3.1](https://kordamp.org/ikonli/) (pack d'icônes vectorielles *Feather*)
- **Persistance & Base de données :**
  - SQLite via le pilote JDBC `sqlite-jdbc` 3.45.1.0 en mode **WAL (Write-Ahead Logging)**
  - Gestion automatisée et versionnée des schémas via **Flyway 10** (`flyway-core`)
  - Répertoire de stockage local conforme aux standards OS via `AppDataLocator` (`%APPDATA%`, `Library/Application Support`, `.local/share`)
- **Gestion monétaire :** calculs arithmétiques stricts en `BigDecimal` (montants persistés sous forme d'entiers en centimes pour éviter tout artefact d'arrondi)
- **Tests :**
  - **Tests unitaires (Modèles, Services & Contrôleurs) :** JUnit 5.10.2, Mockito & AssertJ 3.27.7 (isolation de la logique métier, validation du comportement des contrôleurs FXML par mock des services et gestion des composants graphiques).
  - **Tests utilitaires & UI (`UiUtils`) :** Validation de la configuration dynamique des composants graphiques (factories de cellules, convertisseurs et styles dynamiques pour les sélecteurs de type et de catégories, avec initialisation sécurisée du toolkit via `Platform.startup`)
  - **Tests d'intégration DAO :** Exécution sur base SQLite en mémoire (`jdbc:sqlite::memory:`)
  - **CI/CD :** GitHub Actions automatisé (avec exécution via `xvfb-run` pour garantir le support JavaFX en mode *headless*)
- **Build & packaging :** Apache Maven & Maven Shade Plugin (assemblage en Fat-JAR autonome)

---

## 📋 Prérequis

- **JDK 21** ou supérieur installé (ex. [Eclipse Adoptium Temurin 21](https://adoptium.net/))
- **Apache Maven 3.8+** installé et accessible dans la variable d'environnement `PATH`
- **Git** (recommandé pour récupérer les sources)

Vérifiez vos versions avec les commandes suivantes :
```bash
java -version
mvn -version
```

## 🚀 Démarrage rapide
1. Cloner le projet
```bash
git clone [https://github.com/Th-Fauconnier/monbudget-java.git](https://github.com/Th-Fauconnier/monbudget-java.git)
cd monbudget-java
```

2. Lancer l'application en mode développement

Grâce au plugin JavaFX configuré dans Maven, vous pouvez compiler et lancer directement le projet sans configuration externe :
```bash
mvn javafx:run
```

## 🧪 Tests et Intégration Continue

Le projet implémente une stratégie de test rigoureuse couvrant l'ensemble des couches de l'application :
- **Les tests unitaires** (Modèles, Services) et **utilitaires** (`MoneyHelper`, initialisation du toolkit UI dans `UiUtilsTest`) utilisant JUnit, AssertJ et Mockito.
- **Les tests d'intégration** (DAOs) s'exécutant sur une base SQLite éphémère en mémoire vive.

### Lancer les tests en local
Pour exécuter l'ensemble de la suite de tests :

```bash
mvn clean test
```

## 📦 Compilation et packaging (Fat-JAR)

Pour distribuer l'application sous forme de fichier binaire unique exécutable sur n'importe quel ordinateur équipé d'une JVM 21 :
```
mvn clean package
```
Cette commande compile le code, applique les transformations de ressources et génère le fichier autonome dans le répertoire `target/` :
  * `target/monbudget-java-1.0.0-all.jar`

Lancer le binaire généré :
```
java -jar target/monbudget-java-1.0.0-all.jar
```
Note d'exécution : L'application intègre une classe d'amorçage `com.monbudget.Launcher` sans héritage de `javafx.application.Application` pour contourner la vérification stricte des composants modulaires JavaFX au démarrage du Fat-JAR.

## 🗂️ Arborescence du projet
```
monbudget-java/
├── pom.xml                                  # Configuration Maven du projet
├── README.md                                # Documentation générale
├── docs/                                    # Documentation technique approfondie
│   ├── architecture.md                      # Schéma relationnel, Flyway, threading et services
│   ├── build-packaging.md                   # Guide Maven Shade et installeurs natifs (jpackage)
│   └── ui-guidelines.md                     # Guide de style, BootstrapFX et règles d'UX
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/monbudget/
    │   │       ├── Launcher.java            # Point d'entrée pour le Fat-JAR
    │   │       ├── MainApp.java             # Cycle de vie JavaFX (init / start / stop)
    │   │       ├── controller/              # Contrôleurs FXML
    │   │       │   ├── DashboardController.java       # Vue principale et filtres
    │   │       │   ├── OperationDialogController.java # Création / édition d'opération
    │   │       │   ├── BookDialogController.java      # Création de compte/livre
    │   │       │   └── CategoryDialogController.java  # Création de catégorie
    │   │       ├── dao/                     # Couche DAO (Requêtes préparées JDBC)
    │   │       │   ├── DatabaseManager.java # Connexion SQLite WAL et Flyway
    │   │       │   ├── BookDao.java
    │   │       │   ├── CategoryDao.java
    │   │       │   └── OperationDao.java
    │   │       ├── model/                   # Modèle de données (Records & Enums)
    │   │       │   ├── Book.java            # Entité Livre / Compte
    │   │       │   ├── Category.java        # Entité Catégorie (avec support parent/enfant)
    │   │       │   ├── Operation.java       # Entité Opération (réalisée ou prévisionnelle)
    │   │       │   ├── PaymentMethod.java   # Mode de paiement (CB, Virement, Prélèvement...)
    │   │       │   ├── RecurrenceRule.java  # Règle de répétition des flux récurrents
    │   │       │   ├── SimulationEntry.java # Ligne de simulation budgétaire
    │   │       │   ├── SimulationScenario.java # Scénario de test prospectif
    │   │       │   ├── Tag.java             # Étiquette transversale d'opération
    │   │       │   └── enums/
    │   │       │       ├── Frequency.java       # (DAILY, WEEKLY, MONTHLY, YEARLY)
    │   │       │       ├── OperationStatus.java # (PLANNED, CLEARED, CANCELLED)
    │   │       │       └── OperationType.java   # (EXPENSE, INCOME, TRANSFER)
    │   │       ├── service/                 # Moteur métier (Soldes, Projections, KPIs)
    │   │       │   ├── BalanceService.java
    │   │       │   ├── BalanceSummary.java
    │   │       │   ├── OperationService.java
    │   │       │   └── SimulationService.java
    │   │       └── util/
    │   │           ├── AppDataLocator.java  # Résolution du dossier de données local
    │   │           └── MoneyHelper.java     # Conversions centimes <-> BigDecimal
    │   │           └── UiUtils.java         # Rendu visuel et cell-factories pour ComboBox (Types, Catégories)
    │   └── resources/
    │       ├── com/monbudget/views/         # Écrans FXML
    │       │   ├── dashboard-view.fxml
    │       │   ├── operation-dialog-view.fxml
    │       │   ├── book-dialog-view.fxml
    │       │   └── category-dialog-view.fxml
    │       ├── css/                         # Feuilles de style (custom-theme.css)
    │       └── db/
    │       │   └── migration/
    │       │       └── V1__initial_schema.sql  # Script DDL initial géré par Flyway
    │       ├── logback.xml                  # Configuration des logs pour l'exécution principale
    └── test/
        └── java/
            └── com/monbudget/
                ├── controller/              # Tests unitaires des contrôleurs FXML (Mockito)
                ├── dao/                     # Tests d'intégration DAO (SQLite in-memory)
                ├── model/                   # Tests unitaires des records et fabriques
                ├── service/                 # Tests unitaires des services métier (Mockito)
                └── util/                    # Tests unitaires et d'initialisation UI (ex: UiUtilsTest)
        └── resources/
           └── logback-test.xml              # Configuration des logs dédiée aux tests automatisés
```

## ⌨️ Fonctionnalités clés de l'interface
  * Sélecteur de compte direct : basculez instantanément d'un livre à l'autre grâce au menu déroulant en haut de l'écran avec pastilles de couleur.

  * Création rapide : ajoutez de nouveaux comptes (`➕ Nouveau compte`) ou de nouvelles catégories (`🏷️ Nouvelle catégorie`) directement depuis le bandeau d'en-tête.

  * Pointage en un clic : cliquez sur le bouton d'action d'une opération prévisionnelle pour la transformer en écriture réalisée à la date du jour.

  * Sélecteur de période : filtrez la plage temporelle pour recalculer immédiatement les KPIs, le graphique d'évolution et la liste des mouvements.

  * Ventilation interactive : survolez les portions du camembert pour consulter le montant total et la désignation de chaque catégorie de dépense.

## 📖 Documentation détaillée

Retrouvez les spécifications complètes dans le dossier [`docs/`](docs/) :

* 📐 **[Architecture logicielle & Persistance](docs/architecture.md)**  
  *Organisation en couches (MVC), schéma SQL relationnel, gestion SQLite WAL, migrations Flyway et cycle de vie `MainApp`.*

* 📦 **[Guide de Compilation & Déploiement](docs/build-packaging.md)**  
  *Cycle de vie Maven, contournement du runtime JavaFX via `Launcher`, création du Fat-JAR et génération d'installateurs natifs (`jpackage`).*

* 🎨 **[Charte Graphique & Règles d'Interface](docs/ui-guidelines.md)**  
  *Palette sémantique, intégration BootstrapFX & Ikonli Feather, formatage strict des montants comptables et dialogue d'erreurs standardisé.*

## 👤 Auteur

**Thierry Fauconnier**

* GitHub : [@Th-Fauconnier](https://github.com/Th-Fauconnier)
* LinkedIn : [Thierry Fauconnier](https://www.linkedin.com/in/thierry-fauconnier-89233a213)
* Email : [th.fauconnier@outlook.fr](mailto:th.fauconnier@outlook.fr)

📄 Licence

Ce projet est sous licence GPLv3. Pour plus de détails, consultez le fichier [LICENSE](LICENSE.md)).