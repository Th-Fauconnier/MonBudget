# Guide de Compilation et de Packaging (Build & Packaging) 📦

Ce document explique le cycle de vie Maven, les subtilités d'exécution propres à JavaFX, la génération d'un Fat-JAR autonome via le **Maven Shade Plugin** et la création d'installateurs natifs pour Windows, macOS et Linux avec **jpackage**.

---

## 1. Prérequis d'Environnement

* **JDK :** Java 21 LTS (ex. Eclipse Adoptium Temurin 21 ou GraalVM for JDK 21).
* **Outil de build :** Apache Maven 3.8.0 ou supérieur.
* **Système d'exploitation :** Windows 10/11, macOS (x86_64 ou Apple Silicon), ou Linux (glibc 2.27+).

Vérifiez la configuration active :
```bash
java -version
mvn -version
```

## 2. Commandes de Développement
### 2.1 Compilation du projet

Compile les sources (`src/main/java`) et copie les ressources FXML, CSS et des scripts Flyway (`src/main/resources/db/migration/`) vers `target/classes` :
```Bash
mvn clean compile
```

### 2.2 Exécution des tests

Lance la suite de tests globale (tests unitaires des modèles, services, contrôleurs FXML et utilitaires + tests d'intégration DAO sur SQLite en mémoire) :

```bash

mvn test
```

Pour exécuter une classe de test spécifique :
```bash
mvn test -Dtest=BookDaoTest
```

### 2.3 Lancement direct en mode développement

Le plugin `javafx-maven-plugin` gère automatiquement la résolution des modules graphiques et des bibliothèques natives :
```bash
mvn javafx:run
```

## 3. Comprendre le Problème du "Fat-JAR" sous JavaFX
   **Le piège du lanceur standard**

Depuis Java 11, JavaFX est découplé du JDK standard. Si une classe principale hérite de `javafx.application.Application` et est invoquée directement depuis un JAR non modulaire via `java -jar mon-app.jar`, la JVM déclenche l'erreur :
```plaintext
Error: JavaFX runtime components are missing, and are required to run this application
```

**La solution : le pont `Launcher.java`**

Pour contourner cette vérification stricte lors du chargement initial de la JVM, l'application possède une classe relais sans héritage :
```java
package com.monbudget;

public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
```
La classe `Launcher` initialise l'environnement standard, puis déclenche le cycle de vie JavaFX de `MainApp`. C'est cette classe qui est configurée comme point d'entrée dans le manifeste du JAR.

## 4. Génération du Fat-JAR Autonome (Maven Shade)

Le Fat-JAR regroupe dans un fichier binaire unique l'ensemble du code compilé, les ressources applicatives (scripts SQL Flyway, FXML, thèmes CSS, configuration de journalisation logback.xml) ainsi que toutes les dépendances : JavaFX Controls/FXML, SQLite JDBC, Flyway Core & SQLite, BootstrapFX, Ikonli Feather, SLF4J/Logback et AssertJ.
### 4.1 Production du binaire

(Note : La phase package déclenche par défaut l'ensemble des tests unitaires et d'intégration. Utilisez -DskipTests uniquement en cas de besoin de build d'urgence).
```bash
mvn clean package
```

Deux fichiers sont créés dans le répertoire `target/` :
 1. `monbudget-java-1.0.0.jar` : JAR brut ne contenant que vos classes.
 2. `monbudget-java-1.0.0-all.jar` : **Fat-JAR autonome avec toutes les dépendances.**

### 4.2 Lancement du Fat-JAR
```bash
java -jar target/monbudget-java-1.0.0-all.jar
```

### 4.3 Gestion des filtres de sécurité dans le Shade Plugin

Pour éviter les erreurs `SecurityException: Invalid signature file digest` causées par la fusion de bibliothèques signées numériquement, le pom.xml filtre les signatures cryptographiques :
```xml
<filters>
    <filter>
        <artifact>*:*</artifact>
        <excludes>
            <exclude>META-INF/*.SF</exclude>
            <exclude>META-INF/*.DSA</exclude>
            <exclude>META-INF/*.RSA</exclude>
        </excludes>
    </filter>
</filters>
```

## 5. Création d'Installateurs Natifs avec `jpackage`

L'outil `jpackage` (inclus nativement dans le JDK 21) permet d'embarquer un runtime Java réduit (JRE minimale) avec le Fat-JAR pour produire un exécutable installable sans obliger l'utilisateur final à installer Java.

**Règle multi-plateforme :** `jpackage` ne permet pas la cross-compilation. Pour créer un `.exe`, lancez la commande sous Windows ; pour un `.dmg` ou .`pkg`, sous macOS ; pour un `.deb` ou `.rpm`, sous Linux.

### 5.1 Préparation préalable

Générez d'abord le Fat-JAR dans un sous-dossier propre :
```bash
mvn clean package
mkdir -p dist/input
cp target/monbudget-java-1.0.0-all.jar dist/input/
```

### 5.2 Windows (Installateur `.exe` ou `.msi`)

Prérequis Windows : **WiX Toolset v3.11+** installé et ajouté au `PATH`.
```powershell
jpackage `
  --type exe `
  --input dist/input `
  --dest dist/output `
  --name "MonBudget" `
  --main-jar monbudget-java-1.0.0-all.jar `
  --main-class com.monbudget.Launcher `
  --app-version 1.0.0 `
  --vendor "MonBudget" `
  --description "Gestion de finances personnelles" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --icon src/main/resources/icons/app.ico
  ```

### 5.3 macOS (Image disque `.dmg` ou paquet `.pkg`)
```bash
jpackage \
  --type dmg \
  --input dist/input \
  --dest dist/output \
  --name "MonBudget" \
  --main-jar monbudget-java-1.0.0-all.jar \
  --main-class com.monbudget.Launcher \
  --app-version 1.0.0 \
  --vendor "MonBudget" \
  --mac-package-name "MonBudget" \
  --icon src/main/resources/icons/app.icns
  ```

### 5.4 Linux (Paquet Debian `.deb` ou RedHat `.rpm`)
```bash
jpackage \
  --type deb \
  --input dist/input \
  --dest dist/output \
  --name "monbudget" \
  --main-jar monbudget-java-1.0.0-all.jar \
  --main-class com.monbudget.Launcher \
  --app-version 1.0.0 \
  --vendor "MonBudget" \
  --description "Gestion de finances personnelles" \
  --linux-shortcut \
  --linux-menu-group "Office;Finance;" \
  --icon src/main/resources/icons/app.png
  ```

## 6. Diagnostic et Résolution des Problèmes Fréquents
| Problème / Message d'erreur                                  | Cause probable                                                                          | Solution                                                                                                      |
|:-------------------------------------------------------------|:----------------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------|
| `JavaFX runtime components are missing`                      | Lancement direct du JAR pointant vers `MainApp` au lieu de `Launcher`.                  | Vérifier que `<mainClass>` pointe bien sur `com.monbudget.Launcher` dans le `maven-shade-plugin`.             |
| `Invalid signature file digest for Manifest main attributes` | Conflit de signature cryptographique dans une dépendance ombrée (ex. SQLite ou Ikonli). | S'assurer que le filtre excluant `META-INF/*.SF`, `*.DSA`, `*.RSA` est présent dans le Shade Plugin.          |	
| `Access Denied` / `database is locked` dès l'installation | Chemin relatif de base de données pointant dans `Program Files` ou `/Applications` (lecture seule). | Vérifier que l'application s'appuie sur `AppDataLocator` pour héberger `monbudget.db` dans le dossier utilisateur.          |	
| `FlywayException: Validate failed: Migrations have failed validation` | Un script SQL dans `db/migration/` a été altéré après avoir déjà été exécuté sur la base locale. | Ne jamais modifier un script `V<n>__` déjà joué ; créer un nouveau script incrémental (ex. `V2__...sql`). En développement local, supprimer le fichier .db de test si nécessaire.          |
| `SQLite library not loaded` (Linux / ARM)                    | Incompatibilité de l'architecture native SQLite.                                        | Vérifier la version de `sqlite-jdbc` (les versions 3.45+ incluent les binaires aarch64 et x86_64).            |
| Crash au démarrage sans message d'erreur                     | Erreur de syntaxe dans un fichier FXML ou chemin de ressource erroné.                   | Lancer le JAR en ligne de commande (`java -jar ...`) pour lire la trace d'exception complète `LoadException`. |