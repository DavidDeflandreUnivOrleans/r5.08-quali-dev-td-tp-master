# Rapport TP : Analyse Order Flow

## Tâche 1 : Ségrégation des responsabilités

### Question 1 : Principaux domaines métiers

Les deux domaines principaux identifiés sont :

    Product Registry (Gestion du catalogue produits).

    Store (Processus de vente et commande).

### Question 2 : Conception des services

L'architecture est de type Microservices orientés DDD (Domain Driven Design). Chaque domaine métier est isolé dans son propre contexte (Bounded Context) pour éviter le couplage fort.

### Question 3 : Responsabilités des modules

Analyse effectuée via la commande :

./gradlew <module_name>:dependencies | grep -oE '^\+\-\-\-\sproject\s\:.*$' | awk '!seen[$0]++'

    apps/store-front : Interface utilisateur (Frontend).

    apps/store-back : API Gateway / Backend For Frontend (BFF). Orchestre les appels vers les services métiers.

    libs/kernel : "Shared Kernel". Contient les objets communs (Value Objects, IDs) partagés par tous les services.

    apps/product-registry-domain-service : Service "Écriture" (Command side). Contient la logique métier pure et les règles de validation (Aggregats).

    apps/product-registry-read-service : Service "Lecture" (Query side). Contient les projections de données optimisées pour l'affichage.

    libs/bom-platform : Gestion des versions (Bill of Materials). Centralise les versions des dépendances pour tout le projet.

    libs/cqrs-support : Infrastructure technique implémentant les bus (CommandBus, QueryBus, EventBus).

    libs/sql : Abstraction technique pour la persistance des données relationnelles.

## Tâche 2 : Identifier les concepts principaux

### Question 1 : Concepts principaux

    Pattern CQRS : Séparation stricte entre les modèles d'écriture (Command) et de lecture (Query).

    Architecture EDA (Event-Driven) : Communication asynchrone entre services via des événements métiers.

    DDD : Utilisation d'Aggregats, Value Objects et Entités.

    Consistance à terme : Les données ne sont pas mises à jour instantanément partout, mais propagées via événements.

### Question 2 : Implémentation dans les modules

Les concepts sont implémentés via une séparation Métier vs Infrastructure :

    Le code métier pur réside dans les dossiers apps (Aggregats, Commandes).

    La "plomberie" technique est déléguée aux bibliothèques libs (Bus, SQL).

### Question 3 : Rôle de libs/cqrs-support

Cette bibliothèque fournit les interfaces techniques (Command, Query, Event, Dispatcher). Elle sert à découpler l'appelant (Controller) de l'exécutant (Handler), permettant une architecture modulaire.
### Question 4 : Rôle de libs/bom-platform

C'est un module de configuration pure (sans code). Il impose les versions des dépendances externes (Quarkus, Jackson, etc.) pour éviter les conflits de versions (Dependency Hell) entre les microservices.
### Question 5 : Fiabilité (CQRS et Kernel)

    Kernel : Assure la fiabilité des données unitaires via des Value Objects (validation forte à l'instanciation).

    CQRS : Protège l'intégrité des transactions. Seul le modèle d'écriture (Domain Service) peut modifier l'état, après validation des règles métiers. Le modèle de lecture est en lecture seule.

## Tâche 3 : Identifier les problèmes de qualité
Installation et Configuration de MegaLinter

### 1. Installation du runner :

    pnpm install mega-linter-runner -D -w

### 2. Configuration des dépendances : Ajout dans package.json :
JSON

"devDependencies": {
    "mega-linter-runner": "catalog:"
}

Ajout dans pnpm-workspace.yaml :
YAML

catalog:
  mega-linter-runner: "^9.0.0"

### 3. Initialisation de la configuration :

    pnpm mega-linter-runner --install

### 4. Modification de .mega-linter.yml : Configuration appliquée pour le projet Java :
YAML

APPLY_FIXES: none
CLEAR_REPORT_FOLDER: true
VALIDATE_ALL_CODEBASE: true
ENABLE:
  - JAVA
DISABLE:
  - REPOSITORY
  - COPYPASTE
  - SPELL

### 5. Exécution de l'analyse :

    docker run --rm \
    -v /var/run/docker.sock:/var/run/docker.sock:rw \
    -v "$(pwd)":/tmp/lint:rw \
    oxsecurity/megalinter:v9

Résultats de l'analyse (Problèmes identifiés)

Suite à l'exécution de MegaLinter, les types de problèmes suivants ont été relevés dans le code source :

    Code Style (Checkstyle) : Non-respect des conventions de nommage Java (CamelCase) et indentation incohérente.

    Complexité (PMD/Sonar) : Méthodes trop complexes (Complexité Cyclomatique élevée) avec trop de branchements conditionnels.

    Sécurité/Bonnes pratiques : Utilisation d'injections de dépendances par champ (Field Injection) au lieu de l'injection par constructeur.

# Rapport TP 2 : Qualité et Tests (Product Registry)

Ce module vise à nettoyer la dette technique identifiée et à sécuriser la logique métier via des tests automatisés.

## Tâche 1 & 2 : Documentation et Correction Qualité

### Procédure de correction
1.  **Identification des manques de Javadoc :**
    Utilisation de la commande pour localiser les marqueurs :
    ```bash
    grep -r "TODO: Complete Javadoc" .
    ```
    *Action réalisée :* Ajout des descriptions sur les classes et méthodes publiques, spécifiant les paramètres (`@param`) et les retours (`@return`).

2.  **Correction automatique et manuelle (MegaLinter) :**
    Lancement de l'analyseur sur la racine du workspace :
    ```bash
    pnpm mega-linter-runner -p $WORKSPACE_ROOT
    ```
    *Corrections appliquées :*
    * Formatage du code (indentation, accolades).
    * Suppression des imports inutilisés.
    * Renommage des variables locales pour respecter le `camelCase`.

## Tâche 3 : Tests Unitaires (Domain Kernel)

### Objectif
Validation de l'invariant de l'agrégat `Product`. Le domaine doit garantir qu'aucun produit ne peut exister dans un état invalide.

### Scénarios couverts
* **Création (Happy Path)** : Vérifie qu'un produit créé est bien à l'état `ACTIVE`.
* **Validation** : Vérifie que la création échoue (Exception) si le nom, la description ou le SKU sont nuls.
* **Cycle de vie (Update)** : Vérifie la mise à jour des propriétés.
* **Cycle de vie (Delete)** : Vérifie que la suppression passe le produit à l'état `RETIRED`.
* **Règles métier** : Interdiction de modifier ou supprimer un produit déjà `RETIRED`.

## Tâche 4 : Tests d'Intégration (API Resource)

### Objectif
Valider la couche "Application" et "Web" (Resource). Ces tests vérifient que le contrôleur REST (Quarkus Resource) reçoit correctement les requêtes HTTP, valide les DTOs et appelle le service sous-jacent.

### Scénarios couverts
* `POST /api/products` : Création (201) et erreurs de validation (400).
* `PATCH /api/products/{id}/name` : Renommage (204) et gestion des IDs inexistants (404) ou body invalides (400).
* `DELETE /api/products/{id}` : Suppression logique.
* `GET /api/products` : Liste paginée et filtrage.
* `GET /api/products/{id}` : Récupération unitaire.

*Note : L'utilisation de `@QuarkusTest` permet de charger le contexte CDI pour tester les injections réelles.*

## Tâche 5 : Questions théoriques

### 1. Différence entre Tests Unitaires et d'Intégration
* **Test Unitaire** : Isole une petite unité de code (une classe, une méthode) du reste du système. Toutes les dépendances externes (BDD, autres services) sont bouchonnées (mockées). Ils sont très rapides et testent la logique pure.
* **Test d'Intégration** : Vérifie que plusieurs composants fonctionnent correctement ensemble (ex: Resource + Service + Base de données). Ils sont plus lents mais garantissent que la chaîne de traitement est fonctionnelle.

### 2. Pertinence de la couverture à 100%
**Non**, viser systématiquement 100% n'est pas pertinent et peut être contre-productif.
* **Rendement décroissant** : Tester des getters/setters ou des configurations triviales coûte du temps pour peu de valeur ajoutée.
* **Fragilité** : Trop de tests couplés à l'implémentation rendent le refactoring difficile.
* **Focus** : Il faut prioriser le "Core Domain" (règles métiers complexes) où le risque de bug est élevé et l'impact critique.

### 3. Avantages de l'Architecture en Oignon (Onion Architecture) pour les tests
L'architecture en oignon place le **Domaine au centre**, sans aucune dépendance vers l'extérieur (ni framework, ni BDD).
* **Testabilité du Domaine** : Comme le domaine (ex: `Product`) ne dépend de rien, on peut écrire des tests unitaires purs (sans Mocks complexes, sans contexte Spring/Quarkus), comme réalisé dans la Tâche 3.
* **Isolation** : L'infrastructure (BDD, Web) dépend du domaine, pas l'inverse. Cela permet de tester le métier même si la base de données n'est pas prête.

### 4. Nomenclature des packages
* **model** : Cœur du métier. Contient les Entités, Value Objects et les règles métiers (le "Quoi").
* **application** : Orchestration. Contient les cas d'utilisation (Use Cases), appelle le domaine et utilise les ports (interfaces).
* **infra** : Implémentation technique. Contient les adaptateurs concrets (Repository SQL, Client HTTP, Kafka).
* **web** (ou api) : Point d'entrée. Contient les Contrôleurs REST (Resources) qui exposent l'application au monde extérieur.
* **client** : Si présent, contient les proxies ou SDK pour appeler d'autres microservices.
* **jpa** : Sous-dossier d'infra, spécifique à la persistance relationnelle (Entités Hibernate).