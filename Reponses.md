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