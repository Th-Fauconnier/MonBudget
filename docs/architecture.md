# Documentation d'Architecture : MonBudget 📐

Ce document détaille l'architecture logicielle, les patrons de conception, la stratégie de persistance, la manipulation arithmétique financière, la gestion du multithreading et le cycle de vie applicatif de **MonBudget**.

---

## 1. Vue d'Ensemble & Principes Directeurs

MonBudget est une application de bureau autonome conçue selon les principes de l'**architecture en couches (Layered Architecture)** et le patron **MVC (Modèle - Vue - Contrôleur)** adapté à JavaFX.

### Principes directeurs :
1. **Zéro perte de précision :** Tous les montants financiers sont manipulés en `BigDecimal` (échelle 2, arrondi banquier `HALF_EVEN`) et persistés en `INTEGER` (centimes). Les types flottants (`float`, `double`) sont formellement exclus des calculs comptables.
2. **Cycle de vie résilient :** Séparation stricte entre l'initialisation lourde hors thread graphique (`init()`), l'assemblage de l'interface (`start()`) et la libération des ressources (`stop()`). En cas d'échec critique au démarrage, une boîte de dialogue explicite prend le relais.
3. **Persistance robuste & Migrations automatiques :** La base SQLite est hébergée dans le dossier de données standard de l'utilisateur (`AppDataLocator`) avec le mode **WAL (Write-Ahead Logging)** activé. Les évolutions de schéma sont versionnées et appliquées de manière idempotente par **Flyway**.
4. **Interface non bloquante :** Les opérations d'accès disque/base de données et les calculs statistiques s'exécutent en arrière-plan via l'API de concurrence JavaFX (`Task<T>` / `Service<T>`) afin de préserver la réactivité du thread d'affichage (JavaFX Application Thread).
5. **Autonomie & Confidentialité :** Aucun serveur distant ni service cloud n'est requis. La base de données SQLite est embarquée et stockée localement sur la machine de l'utilisateur.

---

## 2. Organisation en Couches
```
┌─────────────────────────────────────────────────────────┐
│                   Couche Présentation                   │
│   • Fichiers FXML (Vues déclaratives)                   │
│   • Feuilles de styles CSS (BootstrapFX & Personnalisé) │
│   • Contrôleurs JavaFX (@FXML, Événements, Data Binding)│
└───────────────────────────┬─────────────────────────────┘
                            │ Appels
┌───────────────────────────▼─────────────────────────────┐
│                      Couche Métier                      │
│   • BalanceService (calculs de soldes, projections)     │
│   • SimulationService (scénarios what-if & épargne)     │
│   • MoneyHelper (conversions et arrondis)               │
└───────────────────────────┬─────────────────────────────┘
                            │ Appels
┌───────────────────────────▼─────────────────────────────┐
│               Couche Accès Données (DAO)                │
│   • OperationDao, BookDao, CategoryDao                  │
│   • DatabaseManager (Driver SQLite + Flyway + Mode WAL) │
└───────────────────────────┬─────────────────────────────┘
                            │ Lecture / Écriture
┌───────────────────────────▼─────────────────────────────┐
│             Stockage Local Sécurisé (OS)                │
│   • AppDataLocator (%APPDATA%, Library, .local/share)   │
│   • Fichier SQLite : monbudget.db                       │
└─────────────────────────────────────────────────────────┘
```
### Rôle de chaque couche :
* **Présentation (`com.monbudget.controller`, `views/`) :** Réceptionne les actions utilisateur, instancie les tâches asynchrones, alimente les contrôles graphiques (`LineChart`, `PieChart`, `TableView`) et applique le formattage monétaire via `NumberFormat`.
* **Métier / Services (`com.monbudget.service`) :** Contient la logique financière pure : consolidation des soldes effectifs/prévisionnels, calcul des ventilations par catégorie, calcul d'échéances et simulations budgétaires isolées.
* **DAO / Persistance (`com.monbudget.dao`) :** Encapsule le SQL natif avec des `PreparedStatement`. Fait le pont entre les types SQL bruts (textes ISO-8601, entiers en centimes) et les types Java (`LocalDate`, `BigDecimal`, `Record`).
* **Infrastructure de Données (`DatabaseManager`, `AppDataLocator`) :** Résout le chemin absolu sur l'OS hôte, configure les PRAGMAs SQLite et applique les migrations Flyway.
* **Modèle (`com.monbudget.model`) :** Enregistrements Java immuables (`records`) et énumérations (`enum`) représentant l'état du domaine.

---

## 3. Cycle de Vie Applicatif & Démarrage Résilient (`MainApp.java`)

Pour éviter tout gel d'écran ou crash silencieux lors d'un lancement en environnement installé :
```
[ Lancement JVM via Launcher.java ]
             │
             ▼
             MainApp.init()  (Launcher Thread - Hors UI)
             ├── 1. Résolution de l'emplacement disque (AppDataLocator)
             ├── 2. Exécution des migrations incrémentales (Flyway)
             └── 3. Ouverture connexion SQLite (Mode WAL + PRAGMA foreign_keys)
             │
  ┌──────────┴──────────┐
  │ Succès              │ Exception capturée (startupError)
  ▼                     ▼
  MainApp.start(Stage)    MainApp.start(Stage)
  ├── Chargement FXML     └── Affichage boîte de dialogue d'alerte (Alert ERROR)
  ├── Stylesheets (CSS)       avec panneau déroulant de la stack trace
  └── Affichage fenêtre       et fermeture propre via Platform.exit()
  │
  ▼
  MainApp.stop() (Fermeture de l'application)
  └── Fermeture propre de la connexion SQLite et libération des verrous WAL
```
## 4. Modèle de Données & Schéma SQL

### 4.1 Diagramme Entités-Relations (**ERD**)
```
┌──────────────┐          ┌──────────────────────┐          ┌──────────────┐
│     BOOK     │ 1      * │      OPERATION       │ *      1 │   CATEGORY   │
├──────────────┤──────────├──────────────────────┤──────────├──────────────┤
│ id (PK)      │          │ id (PK)              │          │ id (PK)      │
│ name         │          │ book_id (FK)         │          │ parent_id(FK)│
│ initial_bal  │          │ category_id (FK)     │          │ name         │
│ currency     │          │ payment_method_id(FK)│          │ type         │
└──────────────┘          │ amount (centimes)    │          └──────────────┘
                          │ status (PLANNED/SET) │
                          │ operation_date       │
                          │ transfer_op_id (FK)  │
                          └──────────┬───────────┘
                                     │ *
                                     │
                                     │ *
                          ┌──────────┴───────────┐
                          │    OPERATION_TAG     │
                          ├──────────────────────┤
                          │ operation_id (FK)    │
                          │ tag_id (FK)          │
                          └──────────────────────┘
```

### 4.2 Tables Principales

#### Table `book` (Livres thématiques)
| Colonne           | Type    | Contrainte                    | Description                                 |
|:------------------|:--------|:------------------------------|:--------------------------------------------|
| `id`              | INTEGER | PK AUTOINCREMENT              | Identifiant unique du registre              |
| `name`            | TEXT    | NOT NULL                      | Nom du registre (ex. *Vacances*, *Courant*) |
| `initial_balance` | INTEGER | NOT NULL DEFAULT 0            | Solde de départ en centimes                 |
| `currency`        | TEXT    | NOT NULL DEFAULT 'EUR'        | Devise du compte                            |
| `color_hex`       | TEXT    |                               | Code couleur d'identification UI            |
| `is_archived`     | INTEGER | CHECK (is_archived IN (0, 1)) | Indicateur d'archivage                      |

#### Table `operation` (Opérations effectives et prévisionnelles)
| Colonne                 | Type    | Contrainte                               | Description                                 |
|:------------------------|:--------|:-----------------------------------------|:--------------------------------------------|
| `id`                    | INTEGER | PK AUTOINCREMENT                         | Identifiant unique de l'opération           |
| `book_id`               | INTEGER | NOT NULL, FK -> book(id)                 | Registre rattaché                           |
| `category_id`           | INTEGER | FK -> category(id)                       | Catégorie budgétaire                        |
| `payment_method_id`     | INTEGER | FK -> payment_method(id)                 | Mode de règlement                           |
| `label`                 | TEXT    | NOT NULL                                 | Libellé de l'opération                      |
| `amount`                | INTEGER | NOT NULL                                 | Montant en centimes (+ recette, - dépense)  |
| `status`                | TEXT    | CHECK (status IN ('PLANNED', 'SETTLED')) | État d'encaissement / décaissement          |
| `operation_date`        | TEXT    | NOT NULL                                 | Date d'échéance ou de saisie (`YYYY-MM-DD`) |
| `settled_at`            | TEXT    | NULL                                     | Date effective du débit/crédit              |
| `transfer_operation_id` | INTEGER | FK -> operation(id)                      | Clé miroir lors d'un virement entre livres  |

#### Tables `simulation_scenario` et `simulation_entry`
Ces tables sont **totalement isolées** des opérations bancaires réelles. Elles permettent de simuler des projets d'épargne ou des achats sans altérer la comptabilité effective.

---

## 5. Gestion de la Persistance et Migrations (Flyway & WAL)

### 5.1 Emplacement Standardisé des Données (`AppDataLocator`)
Les chemins relatifs (ex. `jdbc:sqlite:monbudget.db`) sont proscrits afin de garantir les droits d'écriture sur les systèmes verrouillés (`Program Files` sous Windows, `/Applications` sous macOS).
* **Windows :** `%APPDATA%\MonBudget\monbudget.db`
* **macOS :** `~/Library/Application Support/MonBudget/monbudget.db`
* **Linux :** `~/.local/share/monbudget/monbudget.db` (ou `$XDG_DATA_HOME`)

### 5.2 Concurrence SQLite optimisée
La connexion JDBC est configurée via `SQLiteConfig` :
* **Clés étrangères :** `config.enforceForeignKeys(true)` (`PRAGMA foreign_keys = ON;`).
* **Mode WAL :** `config.setJournalMode(JournalMode.WAL)` : permet aux tâches de fond de lire sans être bloquées par les écritures de l'utilisateur.
* **Busy Timeout :** `config.setBusyTimeout(5000)` : attend 5 secondes avant de lever une erreur `database is locked`.

### 5.3 Versionnage du Schéma avec Flyway
Les scripts SQL résident sous `src/main/resources/db/migration/` selon la convention `V<Version>__<Description>.sql` :
* `V1__initial_schema.sql` : Tables initiales et index.
* Flyway trace automatiquement les migrations dans la table `flyway_schema_history` et applique uniquement les scripts non encore exécutés.

---

## 6. Stratégie Monétaire et Précision Arithmétique

L'usage de nombres à virgule flottante IEEE 754 (`double`/`float`) engendre des erreurs d'arrondi (ex. `0.1 + 0.2 = 0.30000000000000004`).

### Règle d'or :
* **En mémoire (Java) :** `java.math.BigDecimal` avec échelle fixe de 2 (`setScale(2, RoundingMode.HALF_EVEN)`).
* **En base (SQLite) :** Nombre entier `long` représentant les centimes (`15.40 €` = `1540L`).
* **Conversion centralisée via `MoneyHelper` :**
  ```java
  public static BigDecimal fromCents(long cents) {
      return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
  }

  public static long toCents(BigDecimal amount) {
      return amount.setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact();
  }
  ```

## 7. Gestion de la Concurrence et Threading JavaFX

Pour garantir une expérience utilisateur fluide sans blocage de l'interface lors des accès disque :
```
[ Événement Utilisateur (Clic, Sélecteur de date) ]
                   │
                   │ (JavaFX Application Thread)
                   ▼
      [ Instanciation d'un Task<T> ]
                   │
                   │ (Worker Thread d'arrière-plan)
                   ▼
      [ Exécution de la requête SQL (DAO) ]
  [ Calcul des soldes cumulés (BalanceService) ]
                   │
                   ▼
  [ Succès : callback Task.setOnSucceeded(...) ]
                   │
                   │ (Retour automatique sur JavaFX Thread)
                   ▼
  [ Mise à jour des Labels & Séries des Charts ]
```

## 8. Stratégie de Test et Logging
   ### 8.1 Organisation des tests

   - Tests unitaires (Modèles, Services, Contrôleurs FXML, Utilitaires) : Valident la logique métier et la conformité des composants. Les contrôleurs sont testés unitairement en isolant leurs dépendances via Mockito, et les classes utilitaires (`UiUtils`) intègrent l'initialisation sécurisée du toolkit JavaFX si nécessaire.

   - Tests d'intégration DAO : Valident le comportement réel des requêtes SQL et du mapping de persistance en s'exécutant sur des bases de données SQLite éphémères en mémoire vive (`jdbc:sqlite::memory:`).

### 8.2 Gestion des Journaux (SLF4J / Logback)
- `logback.xml` : Configure les niveaux de journalisation et la sortie des logs pour l'exécution normale (production et développement).

- `logback-test.xml` (situé dans `src/test/resources/`) : Isole et épure les logs générés lors de l'exécution des tests automatisés, garantissant des retours de build propres et sans bruit inutile dans la console.

## 9. Structure Modulaire du Code Source
```
src/main/java/com/monbudget/
├── Launcher.java             # Lanceur sans héritage Application (contournement JavaFX Shade)
├── MainApp.java              # Cycle de vie JavaFX (init/start/stop, gestion d'erreur)
├── controller/               # Contrôleurs MVC
│   ├── DashboardController.java
│   └── OperationDialogController.java
├── dao/                      # Data Access Objects (Requêtes préparées & JDBC)
│   ├── BookDao.java
│   ├── DatabaseManager.java  # Connexion SQLite WAL + initialisation Flyway
│   └── OperationDao.java
├── model/                   # Modèle de données (Records & Enums)
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
├── service/                  # Services métier (Calculs de soldes, KPIs)
│   ├── BalanceService.java
│   └── BalanceSummary.java
└── util/
    └── AppDataLocator.java   # Localisation multiplateforme du fichier .db
    └── MoneyHelper.java      # Utilitaire de conversion bidirectionnelle entre centimes (long) et montants décimaux (BigDecimal).
    └── UiUtils.java          # Rendu visuel et cell-factories pour ComboBox (Types, Catégories)
```

## 10. Packaging & Déploiement
Le packaging est assuré par le plugin Maven Shade Plugin, configuré pour :

 1. Fusionner les dépendances (JavaFX controls, SQLite JDBC, BootstrapFX, Ikonli) dans un unique Fat-JAR exécutable. 
 2. Éviter les conflits de signatures cryptographiques en excluant les fichiers `META-INF/*.SF`, `*.DSA`, `*.RSA`. 
 3. Désigner `com.monbudget.Launcher` comme classe principale pour initialiser la JVM avant le runtime JavaFX.

---
