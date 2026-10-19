# Directives Graphiques et Règles d'Interface (UI Guidelines) 🎨

Ce document rassemble les conventions d'ergonomie, les styles visuels, l'intégration des dépendances graphiques (BootstrapFX 0.4.0 et Ikonli 12.3.1), les règles de formatage financier ainsi que la gestion visuelle des erreurs dans l'application MonBudget.

---

## 1. Principes d'Expérience Utilisateur (UX)

L'application s'adresse à des particuliers non comptables. Les règles suivantes sont obligatoires pour toute nouvelle interface :

1. **Zéro jargon financier obscur :** Préférer les termes « Dépense », « Rentrée », « En attente », « Réalisé » à « Débit », « Crédit », « Rapprochement », « Exercice comptable ».
2. **Distinction immédiate du Réalisé et du Prévisionnel :** Les montants et opérations non encore débités/crédités doivent être visuellement identifiables au premier coup d'œil via des badges sémantiques distincts (`badge-settled` en vert émeraude, `badge-planned` en ambre doré).
3. **Fluidité absolue (60 fps) :** Tout traitement lourd (requêtes SQLite de synthèse, calculs de projection via `BalanceService`, exécution des migrations Flyway) doit s'exécuter hors du thread d'affichage via `javafx.concurrent.Task` avec indicateur d'activité si la durée dépasse 200 ms.
4. **Validation proactive :** Formater les champs de saisie en temps réel (virgules converties en points, interdiction des montants négatifs saisis manuellement là où le type « Dépense » applique déjà le signe négatif en base).
5.  **Résilience et transparence :** Aucun échec technique (base verrouillée, migration Flyway compromise, ressource manquante) ne doit causer une fermeture brutale sans explication. Une boîte de dialogue standardisée doit informer l'utilisateur avec un panneau technique déroulant.

---

## 2. Palette Chromatique et Sémantique

La charte graphique repose sur une base claire, moderne et contrastée, conforme aux recommandations WCAG AA (ratio de contraste supérieur à 4,5:1 pour le texte courant).

### 2.1 Couleurs Sémantiques

| Rôle                       | Teinte          | Code Hex  | Classe BootstrapFX équivalente   | Usage                                                          |
|:---------------------------|:----------------|:----------|:---------------------------------|:---------------------------------------------------------------|
| **Primaire**               | Bleu cobalt     | `#007bff` | `btn-primary`, `lbl-primary`     | Actions principales, onglet actif                              |
| **Positif / Revenu**       | Vert émeraude   | `#28a745` | `lbl-success`, `text-success`    | Entrées d'argent, solde créditeur, badge « Réalisé »           |
| **Négatif / Dépense**      | Rouge corail    | `#dc3545` | `lbl-danger`, `text-danger`      | Dépenses, solde débiteur, alertes de découvert                 |
| **Prévisionnel / Neutre**  | Ambre doré      | `#ffc107` | `lbl-warning`, `text-warning`    | Opérations prévisionnelles, échéances à venir                  |
| **Projection**             | Vert sarcelle   | `#16a085` | `lbl-warning`, `text-warning`    | kpi-value-projected	Solde prévisionnel estimé en fin de période |
| **Fond général**           | Blanc cassé     | `#f8f9fa` | `-fx-background-color: #f8f9fa;` | Fond des fenêtres principales                                  |
| **Surfaces / Cartes**      | Blanc pur       | `#ffffff` | `panel-default`, `panel-body`    | Cartes KPI, panneaux graphiques, tableaux                      |
| **Bordures & Séparateurs** | Gris perle      | `#dee2e6` | `-fx-border-color: #dee2e6;`     | Contours des tableaux et séparateurs                           |
| **Texte principal**        | Gris anthracite | `#212529` | `-fx-text-fill: #212529;`        | Corps de texte et étiquettes                                   |
| **Texte secondaire**       | Gris moyen      | `#6c757d` | `text-muted`                     | Notes, dates d'opération, unités de mesure                     |

---

## 3. Typographie et Formatage des Données

### 3.1 Police de Caractères
* **Famille par défaut :** Police système sans-serif native (`Segoe UI` sous Windows, `San Francisco` sous macOS, `Ubuntu Sans` / `DejaVu Sans` sous Linux).
* **Hiérarchie :**
    * **Titres d'écrans :** 20 px, gras (`-fx-font-weight: bold;`).
    * **Chiffres clés / KPI :** 18 px à 22 px, semi-gras.
    * **Titres de sections / Tableaux :** 14 px, demi-gras.
    * **Texte courant et cellules :** 12 px à 13 px, régulier.
    * **Mentions annexes / Métadonnées :** 11 px, italique ou gris secondaire.

### 3.2 Formatage Monétaire Strict
Les montants monétaires affichés doivent **toujours** respecter la convention francophone et s'appuyer sur `BigDecimal` ::
* Format : `1 250,50 €` (séparateur de milliers par une espace insécable, virgule décimale, symbole monétaire avec espace insécable).
* Implémentation requise :
  ```java
  private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);
  // Exemple de sortie : CURRENCY_FORMAT.format(new BigDecimal("1250.50")) -> "1 250,50 €"
  ```
* Alignement en tableau : Tout montant financier affiché dans un TableView doit obligatoirement être aligné à droite pour permettre une lecture verticale aisée des chiffres.
* Coloration contextuelle :

  * Dépenses : classe .money-cell-expense (rouge corail).

  * Recettes : classe .money-cell-income (vert émeraude).

## 4. Intégration de BootstrapFX

L'application associe BootstrapFX (`bootstrapfx-core`) pour les composants interactifs de base et `custom-theme.css` pour la spécialisation comptable.

### 4.1 Initialisation dans `MainApp.java`

La feuille de style globale de BootstrapFX doit être injectée à l'initialisation de chaque scène principale :
```java
import org.kordamp.bootstrapfx.BootstrapFX;

// Lors de la configuration du Stage
Scene scene = new Scene(rootLayout);
scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
scene.getStylesheets().add(getClass().getResource("/css/custom-theme.css").toExternalForm());
```

### 4.2 Utilisation dans les fichiers FXML

Utiliser l'attribut `styleClass` directement sur les nœuds JavaFX :
```XML
<!-- Bouton principal d'action -->
<Button onAction="#handleNewOperation" styleClass="btn, btn-primary" text="Ajouter une opération"/>

<!-- Bouton d'annulation ou action secondaire -->
<Button onAction="#handleCancel" styleClass="btn, btn-default" text="Annuler"/>

<!-- Bouton de suppression -->
<Button onAction="#handleDelete" styleClass="btn, btn-danger, btn-sm" text="Supprimer"/>

<!-- Cartes d'indicateurs (KPI Panels) -->
<VBox styleClass="panel, panel-default">
    <VBox styleClass="panel-body">
        <Label styleClass="text-muted" text="Solde effectif"/>
        <Label fx:id="currentBalanceLabel" styleClass="lbl, lbl-success" text="1 450,00 €"/>
    </VBox>
</VBox>
```

## 5. Intégration des Icônes Vectorielles (Ikonli Feather)

Pour garantir la netteté sur les écrans haute densité (Retina/HiDPI), les icônes sont déclarées en vectoriel avec le pack Feather d'Ikonli (`ikonli-feather-pack`).
### 5.1 Déclaration FXML d'un Bouton avec Icône

Nécessite l'import de la classe `org.kordamp.ikonli.javafx.FontIcon` :
```XML
<?import org.kordamp.ikonli.javafx.FontIcon?>

<Button onAction="#handleNewExpense" styleClass="btn, btn-primary" text=" Nouvelle dépense">
    <graphic>
        <FontIcon iconColor="white" iconLiteral="fea-plus-circle" iconSize="16"/>
    </graphic>
</Button>
```

### 5.2 Répertoire des Icônes Recommandées

| Action / Élément             | Identifiant Ikonli                 | Rendu attendu          |
|:-----------------------------|:-----------------------------------|:-----------------------|
| **Ajouter une ligne**        | `fea-plus` ou `fea-plus-circle`    | Signe plus             |
| **Modifier**                 | `fea-edit-2`                       | Crayon                 |
| **Supprimer**                | `fea-trash-2`                      | Poubelle               |
| **Valider / Régler**         | `fea-check ou fea-check-circle`    | Coche verte            |
| **Dépense**                  | `fea-arrow-down-left`              | Flèche descendante     |
| **Revenu**                   | `fea-arrow-up-right`               | Flèche montante        |
| **Virement entre livres**    | `fea-repeat`                       | Double flèche rotative |
| **Livre thématique**         | `fea-book ``ou ``fea-folder`       | Classeur / Dossier     |
| **Simulation / Projections** | `fea-trending-up `ou `fea-sliders` | Tendance / Curseur     |
| **Paramètres**               | `fea-settings`                     | Engrenage              |

## 6. Règles de Conception des Graphiques (`Charts`)
### 6.1 Courbe d'Évolution de Solde (`LineChart`)

   * **Animation :** Désactiver les animations (`animated="false"`) lors du rechargement de données dynamiques pour éviter les artefacts visuels et les scintillements.

   * **Marqueurs :** Activer `createSymbols="true"` pour matérialiser chaque jour avec un point cliquable ou survolable.

   * **Axe X (Dates) :** Format court `dd/MM` pour éviter le chevauchement des dates.

### 6.2 Répartition par Catégorie (`PieChart`)
  * **Lisibilité :** Si une catégorie représente moins de 2 % du budget total de la période, la fusionner dynamiquement côté contrôleur dans un groupe synthétique nommé « Autres » afin d'éviter la surcharge visuelle du camembert.
  * **Infobulles obligatoires :** Chaque tranche du graphique doit être associée à un composant `Tooltip` affichant :
    * Le libellé complet de la catégorie.
    * Le montant absolu formaté (`X XXX,XX €`).
    * La proportion exacte avec une décimale (`XX,X %`).

## 7. Gestion Standard des Boîtes de Dialogue d'Erreur

Lors d'un échec d'initialisation dans `MainApp.init()` (ex. base inaccessible, migration Flyway en échec) ou d'une anomalie SQL non recouvrable, l'application doit obligatoirement présenter un dialogue d'erreur standardisé avec déroulant technique :

```java
Alert alert = new Alert(Alert.AlertType.ERROR);
alert.setTitle("Erreur système - MonBudget");
alert.setHeaderText("Impossible d'initialiser l'application ou la base de données.");
alert.setContentText("Une erreur critique est survenue :\n" + error.getMessage());

// Extraction de la trace complète dans un panneau dépliable
StringWriter sw = new StringWriter();
error.printStackTrace(new PrintWriter(sw));

TextArea textArea = new TextArea(sw.toString());
textArea.setEditable(false);
textArea.setWrapText(true);
textArea.setMaxWidth(Double.MAX_VALUE);
textArea.setMaxHeight(Double.MAX_VALUE);

alert.getDialogPane().setExpandableContent(new VBox(textArea));
alert.showAndWait();

```

## 8. Structure d'un Écran Standard (Layout Guide)

Pour maintenir la cohérence de l'application, chaque écran principal est organisé selon la grille suivante :
```
┌──────────────────────────────────────────────────────────────────────────────┐
│  En-tête de vue : Titre + Sous-titre explicatif          [Boutons d'action]  │
├──────────────────────────────────────────────────────────────────────────────┤
│  Barre d'outils / Filtres : Sélecteur de livre, Période (Du/Au), Recherche   │
├──────────────────────────────────────────────────────────────────────────────┤
│  [ Carte KPI 1 ]       [ Carte KPI 2 ]       [ Carte KPI 3 (Projeté) ]       │
├──────────────────────────────────────────────┬───────────────────────────────┤
│                                              │                               │
│  Composant principal gauche (70 % largeur)   │  Composant latéral (30 %)     │
│  Exemple : TableView des écritures           │  Exemple : PieChart           │
│  ou LineChart de trésorerie                  │  ou Détail de l'opération     │
│                                              │                               │
├──────────────────────────────────────────────┴───────────────────────────────┤
│  Pied de page / Barre d'état : Nombre d'opérations listées, Dernière synchro │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Espacements et Marges**
  * **Marges extérieures de fenêtre :** 20 px (`padding: 20;`).
  * **Espacement entre composants de même rang :** 15 px (`spacing: 15;`).
  * **Marges intérieures des cartes (Panels) :** 14 px à 18 px (`padding: 14 18;`).
  * **Rayon de courbure des cartes :** 8 px (`-fx-background-radius: 8px;`).

---