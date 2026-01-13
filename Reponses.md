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

---

# Rapport TP 3 : Collaboration sur le Monorepo

## Tâche 1 : Modèle de Contrôle de Version et Stratégie de Branchement

### Modèle choisi : Git Flow adapté pour Monorepo

**Justification** :
- **Branche principale** (`main`) : Code stable, prêt pour la production
- **Branche de développement** (`develop`) : Intégration continue des fonctionnalités
- **Branches de fonctionnalité** (`feature/nom-fonctionnalite`) : Développement isolé par équipe
- **Branches de release** (`release/0.x.x`) : Préparation des versions
- **Branches de hotfix** (`hotfix/nom`) : Corrections urgentes

**Adaptation pour Monorepo** :
- Préfixage des branches par le module concerné : `feature/product-registry/nom-fonctionnalite`
- Communication obligatoire avant fusion pour éviter les conflits entre microservices
- Validation croisée entre équipes avant merge dans `develop`

## Tâche 2 : Responsabilités des Équipes

### Équipe 1 : Product Registry (Domaine Produit)
**Responsabilités** :
- `apps/product-registry-domain-service` : Service de commande (écriture)
- `apps/product-registry-read-service` : Service de lecture (projections)
- `libs/kernel` : Logique métier partagée (agrégat Product)
- `libs/contracts/product-registry-contract` : Contrats API

### Équipe 2 : Store (Domaine Vente)
**Responsabilités** :
- `apps/store-back` : Backend For Frontend
- `apps/store-front` : Interface utilisateur Angular
- Intégration avec Product Registry via les contrats

**Points de communication** :
- Modification des contrats (`libs/contracts`) : validation croisée obligatoire
- Modification du Kernel : impact sur les deux équipes, discussion préalable
- Événements partagés : coordination nécessaire

## Tâche 3 : README et Règles de Collaboration

### Fichier CONTRIBUTING.md créé avec les règles suivantes :

1. **Avant de commencer** :
   - Créer une issue/branche pour la fonctionnalité
   - Vérifier les dépendances avec l'autre équipe
   - S'assurer que `develop` est à jour

2. **Pendant le développement** :
   - Commits atomiques et messages clairs
   - Tests obligatoires pour toute nouvelle fonctionnalité
   - Respect des conventions de code (MegaLinter)

3. **Avant de fusionner** :
   - Tous les tests doivent passer
   - Code review par un membre de l'autre équipe si impact cross-module
   - Documentation mise à jour si nécessaire

4. **Communication** :
   - Slack/Trello pour les discussions
   - Daily standup pour synchronisation
   - Merge Request avec description détaillée

## Tâche 4 : Organisation des Équipes

**Équipe Product Registry** :
- Développeur 1 : Domain Service (Command side)
- Développeur 2 : Read Service (Query side, projections)
- Rôle : Maintenir la cohérence du domaine produit

**Équipe Store** :
- Développeur 1 : Backend (BFF, intégrations)
- Développeur 2 : Frontend (UI/UX)
- Rôle : Expérience utilisateur et orchestration

**Canaux de communication** :
- Slack : Discussions quotidiennes
- Trello : Suivi des tâches et dépendances
- GitLab Issues : Suivi des bugs et fonctionnalités

## Tâche 5 : Version 0.1.0

### Processus de release :
1. **Préparation** :
   ```bash
   git checkout develop
   git pull origin develop
   ./gradlew build
   ./gradlew test
   ```

2. **Création de la branche release** :
   ```bash
   git checkout -b release/0.1.0
   ```

3. **Finalisation** :
   - Mise à jour des versions dans `build.gradle`
   - Mise à jour du CHANGELOG.md
   - Vérification que tous les tests passent

4. **Tag et merge** :
   ```bash
   git tag -a v0.1.0 -m "Version 0.1.0 - Base initiale"
   git checkout main
   git merge release/0.1.0
   git push origin main --tags
   ```

### Règles pour les Merge Requests :
- **Titre clair** : `[Module] Description courte`
- **Description** : Contexte, changements, tests, impact
- **Labels** : `feature`, `bugfix`, `refactor`, etc.
- **Assignation** : Au moins un reviewer de l'autre équipe si impact cross-module

## Tâche 6 : Journal de Bord

### Structure créée : `doc/journal/`

**Sections** :
1. **Décisions Architecturales (ADRs)** :
   - Choix techniques et justifications
   - Alternatives considérées
   - Impact sur les équipes

2. **Réponses aux Questions TP** :
   - Documentation des réponses pour évaluation
   - Références au code

**Exemple d'ADR** :
```
## ADR-001 : Utilisation de CQRS pour Product Registry

**Contexte** : Séparation lecture/écriture nécessaire pour performance
**Décision** : Deux services distincts (domain-service et read-service)
**Conséquences** : Complexité accrue mais meilleure scalabilité
**Date** : [Date]
```

---

# Rapport TP 4 : Projection des Événements dans des Vues Matérialisées

## Tâche 1 : Questions sur la Base de Code

### 1. Rôle de l'interface Projector

L'interface `Projector<S, E>` est le contrat pour transformer des événements métier (`E`) en vues matérialisées de type `S`. Elle encapsule la logique de projection qui permet de reconstruire l'état d'une vue à partir d'un flux d'événements.

**Fonctionnement** :
- Prend un état courant (optionnel) et un événement
- Retourne un `ProjectionResult<S>` contenant le nouvel état projeté
- Supporte la projection de plusieurs événements séquentiellement via `projectAll()`

### 2. Rôle du type S dans l'interface Projector

Le type `S` représente le **type de la vue matérialisée** (State) qui sera construite à partir des événements. C'est un paramètre générique qui permet à l'interface d'être réutilisable pour différents types de vues.

**Exemple concret** : Dans `ProductViewProjector implements Projector<ProductView, ProductEventV1Envelope<?>>`, `S = ProductView` représente la vue optimisée pour la lecture des produits.

### 3. Javadoc complétée pour S

```java
/**
 * Projector interface for projecting events onto a state.
 * 
 * @param <S> the type of the materialized view (state) that will be built from events
 * @param <E> the type of events to project
 * @author Thibaud FAURIE
 */
public interface Projector<S, E extends EventEnvelope<? extends DomainEvent>> {
    // ...
}
```

### 4. Intérêt d'une interface plutôt qu'une classe concrète

**Avantages** :
- **Polymorphisme** : Permet d'avoir plusieurs implémentations pour différents types de vues
- **Testabilité** : Facilite le mocking dans les tests
- **Découplage** : Le code qui utilise le Projector ne dépend pas de l'implémentation
- **Extensibilité** : Facile d'ajouter de nouveaux types de projections sans modifier le code existant
- **Inversion de dépendance** : Respect du principe SOLID (Dependency Inversion)

**Exemple** : `ProductViewProjector` et potentiellement `ProductCatalogProjector` peuvent coexister et être utilisés de manière interchangeable.

### 5. Rôle de ProjectionResult dans l'interface Projector

`ProjectionResult<S>` est une **monade** qui encapsule le résultat d'une projection avec trois états possibles :
- **Success** : La projection a réussi, contient le nouvel état
- **Failure** : La projection a échoué, contient un message d'erreur
- **NoOp** : La projection n'a rien changé (événement déjà traité, séquence invalide, etc.)

**Avantages de cette approche** :
- Gestion explicite des erreurs sans exceptions
- Chaînage fonctionnel via `flatMap()` et `map()`
- Évite les `null` et les exceptions non gérées

### 6. Intérêt de la Monade par rapport à la gestion d'erreur traditionnelle

**Gestion traditionnelle Java** :
```java
// Approche traditionnelle avec exceptions
public ProductView project(Optional<ProductView> current, Event ev) {
    if (current.isPresent() && current.get().isActive()) {
        throw new IllegalStateException("Product already exists");
    }
    // ... logique
    return newView;
}
```

**Problèmes** :
- Les exceptions interrompent le flux d'exécution
- Difficile de chaîner les opérations
- Pas de distinction entre erreur et "pas de changement"
- Performance : création de stack trace

**Approche Monade (ProjectionResult)** :
```java
// Approche monadique
public ProjectionResult<ProductView> project(Optional<ProductView> current, Event ev) {
    if (current.isPresent() && current.get().isActive()) {
        return ProjectionResult.failed("Product already exists");
    }
    // ... logique
    return ProjectionResult.projected(newView);
}
```

**Avantages concrets** :
1. **Chaînage fonctionnel** : `result.flatMap(state -> autreProjection(state))`
2. **Composition** : Plusieurs projections peuvent être combinées sans try/catch
3. **Performance** : Pas de création de stack trace pour les erreurs métier
4. **Explicite** : Le type de retour indique clairement qu'une erreur est possible
5. **Gestion fine** : Distinction entre erreur, succès et "pas de changement"
6. **Testabilité** : Plus facile à tester car pas d'exceptions à mocker

## Tâche 2 : Questions concernant l'Outboxing

### 1. Rôle de l'interface OutboxRepository

L'interface `OutboxRepository` gère le **pattern Outbox** qui garantit la livraison fiable des événements dans un système distribué. Elle fournit les opérations pour :
- **publish()** : Enregistrer un événement dans l'outbox
- **fetchReadyByAggregateTypeOrderByAggregateVersion()** : Récupérer les événements prêts à être publiés
- **delete()** : Supprimer un événement après publication réussie
- **markFailed()** : Marquer un événement comme échoué pour retry ultérieur

### 2. Comment l'Outbox Pattern garantit la livraison

L'Outbox Pattern résout le problème de **double écriture** (écrire dans la BDD métier ET publier l'événement) en utilisant une **transaction atomique** :

1. **Écriture atomique** : L'événement est écrit dans la même transaction que la modification métier
2. **Polling asynchrone** : Un processus séparé lit l'outbox et publie les événements
3. **Idempotence** : Les événements peuvent être republiés sans duplication
4. **Retry automatique** : Les échecs sont marqués et retentés

**Garantie** : Si la transaction métier réussit, l'événement sera forcément publié (éventuellement après retry).

### 3. Fonctionnement concret dans l'application

**Flux des événements** :

```
[Domain Service] 
    ↓ (1) Transaction commence
    ↓ (2) Modifie l'agrégat Product
    ↓ (3) Génère un événement ProductRegistered
    ↓ (4) Écrit dans EventLog (journal d'événements)
    ↓ (5) Écrit dans Outbox (même transaction)
    ↓ (6) Transaction commit
    ↓
[Outbox Poller] (processus séparé)
    ↓ (7) Poll l'outbox toutes les X secondes
    ↓ (8) Récupère les événements prêts (status = READY)
    ↓ (9) Publie vers le bus d'événements
    ↓ (10a) Succès → Supprime de l'outbox
    ↓ (10b) Échec → Marque comme FAILED avec retry
```

**Diagramme de séquence** :

```
DomainService    EventLog    Outbox    OutboxPoller    EventBus
     |              |          |            |              |
     |--transaction-|          |            |              |
     |  start       |          |            |              |
     |              |          |            |              |
     |--append------>          |            |              |
     |  (event)     |          |            |              |
     |              |          |            |              |
     |--publish--------------->|            |              |
     |  (outbox)    |          |            |              |
     |              |          |            |              |
     |--commit------|          |            |              |
     |              |          |            |              |
     |              |          |<--fetch----|              |
     |              |          |  (ready)   |              |
     |              |          |            |              |
     |              |          |--publish-->|              |
     |              |          |            |              |
     |              |          |            |--event------>|
     |              |          |            |              |
     |              |          |<--delete---|              |
     |              |          |  (success) |              |
```

**Interactions transactionnelles** :
- **Transaction 1** (Domain Service) : Écriture atomique EventLog + Outbox
- **Transaction 2** (Outbox Poller) : Lecture Outbox + Publication + Suppression (si succès)

### 4. Gestion des erreurs de livraison

D'après le schéma Liquibase et le code :

**Table Outbox** (`domain-changelog.xml`) :
- `status` : État de l'événement (READY, FAILED, PROCESSING)
- `retry_count` : Nombre de tentatives
- `max_retries` : Nombre maximum de tentatives
- `retry_after` : Timestamp pour retry différé

**Mécanisme de retry** :
1. **Échec initial** : `markFailed(entity, error, retryAfter)` avec timestamp futur
2. **Polling intelligent** : Le poller ignore les événements avec `retry_after > now()`
3. **Limite de retry** : Si `retry_count >= max_retries`, l'événement reste en FAILED (nécessite intervention manuelle)
4. **Logging** : Les erreurs sont loggées pour monitoring

**Exemple de code** :
```java
// Dans OutboxPartitionedPoller
List<OutboxEntity> ready = outboxRepository
    .fetchReadyByAggregateTypeOrderByAggregateVersion(
        aggregateType, limit, maxRetries);
    
try {
    publishToEventBus(entity);
    outboxRepository.delete(entity); // Succès
} catch (Exception e) {
    outboxRepository.markFailed(entity, e.getMessage(), 
        calculateRetryAfter(retryCount)); // Retry plus tard
}
```

## Tâche 3 : Questions concernant le Journal d'Événements

### 1. Rôle du journal d'événements

Le journal d'événements (Event Log) est une **source de vérité immuable** qui enregistre tous les événements métier dans l'ordre chronologique. Il sert de :
- **Audit trail** : Historique complet des changements
- **Source pour reconstruire l'état** : Replay des événements pour recréer les vues
- **Backup** : En cas de perte de données, possibilité de tout reconstruire
- **Debugging** : Compréhension de l'évolution du système

### 2. Pourquoi seulement append() et pas de méthodes de récupération/suppression ?

**Principe d'immutabilité** :
- Les événements sont **immutables** : une fois écrits, ils ne changent jamais
- Pas de modification possible : garantit l'intégrité de l'historique
- Pas de suppression : l'historique doit être complet pour audit et replay

**Conséquence** : Le journal est une structure **append-only** (écriture seule), similaire à un WAL (Write-Ahead Log) dans les bases de données.

**Avantages** :
- **Performance** : Écriture séquentielle très rapide
- **Fiabilité** : Pas de corruption possible par modification
- **Simplicité** : Interface minimale, moins de bugs

### 3. Implications de cette conception

**Gestion des événements** :
- **Ordre garanti** : Les événements sont dans l'ordre d'écriture (séquence)
- **Replay possible** : On peut reconstruire n'importe quel état en rejouant les événements
- **Versioning** : Chaque événement a un numéro de séquence pour détecter les incohérences

**Autres usages possibles** :
1. **Event Sourcing** : Reconstruire l'état complet d'un agrégat depuis le début
2. **Time Travel** : Voir l'état du système à un moment donné
3. **Analytics** : Analyser l'historique pour comprendre les patterns
4. **Compliance** : Audit réglementaire (qui a fait quoi et quand)
5. **Debugging distribué** : Comprendre les interactions entre services
6. **Nouvelles projections** : Créer de nouvelles vues en rejouant les événements

**Exemple concret** :
```java
// Reconstruire une vue depuis le début
List<EventEnvelope> allEvents = eventLogRepository.findAll();
ProductView view = projector.projectAll(Optional.empty(), allEvents);
```

## Tâche 4 : Limites de CQRS

### 1. Principales limites de CQRS

**Complexité accrue** :
- Deux modèles à maintenir (écriture et lecture)
- Synchronisation nécessaire entre les deux
- Plus de code à écrire et maintenir

**Consistance à terme (Eventual Consistency)** :
- Les données de lecture peuvent être temporairement obsolètes
- Difficile de garantir une lecture immédiatement après écriture
- Nécessite une gestion des incohérences temporaires

**Latence de projection** :
- Délai entre l'écriture et la disponibilité en lecture
- Peut être problématique pour certaines opérations

**Gestion des erreurs de projection** :
- Si une projection échoue, la vue peut être incohérente
- Nécessite un mécanisme de re-projection

**Duplication de logique** :
- Validation dans le modèle d'écriture
- Transformation dans les projections
- Risque d'incohérence si les règles changent

### 2. Limites déjà compensées par l'implémentation actuelle

**Outbox Pattern** :
- ✅ Compense la perte d'événements (garantit la livraison)
- ✅ Gère les retry automatiques

**Event Log** :
- ✅ Permet le replay en cas d'erreur de projection
- ✅ Source de vérité pour reconstruire les vues

**ProjectionResult (Monade)** :
- ✅ Gestion explicite des erreurs de projection
- ✅ Distinction entre erreur, succès et no-op

**Séquencement des événements** :
- ✅ Numéro de séquence pour garantir l'ordre
- ✅ Détection des événements dupliqués ou manquants

### 3. Autres limites introduites par cette mise en œuvre

**Performance du polling** :
- Le poller interroge périodiquement l'outbox (latence)
- Charge supplémentaire sur la base de données

**Gestion manuelle des échecs** :
- Les événements en FAILED nécessitent une intervention manuelle
- Pas de mécanisme automatique de récupération après max_retries

**Pas de garantie de délai** :
- Impossible de garantir que la vue sera à jour dans X secondes
- Dépend de la fréquence du polling

**Complexité opérationnelle** :
- Monitoring nécessaire pour détecter les problèmes
- Maintenance du poller et de l'outbox

### 4. Projection multiple (un événement → plusieurs actions)

**Problème** :
Si un événement `ProductRegistered` doit déclencher :
1. Création de la vue `ProductView`
2. Mise à jour du `ProductCatalog`
3. Envoi d'une notification

**Limites actuelles** :
- Un seul `Projector` par type d'événement dans `ProjectionDispatcher`
- Pas de mécanisme pour plusieurs projections parallèles
- Risque d'incohérence si une projection échoue et pas les autres

**Solutions possibles** :
1. **Saga Pattern** : Orchestrer plusieurs projections avec compensation
2. **Event Router** : Router l'événement vers plusieurs handlers
3. **Choreography** : Chaque projection écoute l'événement indépendamment
4. **Transaction distribuée** : Toutes les projections dans une transaction (complexe)

**Question Bonus : Solutions pour atténuer les limites**

1. **Pour la latence** :
   - Réduire l'intervalle de polling
   - Utiliser des triggers de base de données (CDC - Change Data Capture)
   - Webhooks pour notification immédiate

2. **Pour les projections multiples** :
   - Implémenter un `EventRouter` qui dispatch vers plusieurs projectors
   - Utiliser un pattern Observer pour les projections secondaires
   - Saga Pattern pour orchestrer les projections complexes

3. **Pour la gestion des erreurs** :
   - Dead Letter Queue pour les événements définitivement échoués
   - Dashboard de monitoring pour visualiser les problèmes
   - Alertes automatiques sur les échecs répétés

4. **Pour la consistance** :
   - Versioning des projections pour détecter les incohérences
   - Endpoints de vérification de cohérence
   - Re-projection automatique périodique

---

# Erreurs de Build et Corrections

## Problèmes Identifiés lors de la Configuration WSL

### 1. Fichier de Test Mal Placé

**Erreur** :
```
error: package org.junit.jupiter.api does not exist
error: cannot find symbol: class QuarkusTest
```

**Cause** : Le fichier `ProductTest.java` était dans `src/main/java/.../test/` au lieu de `src/test/java/`

**Correction** :
- Déplacement vers `src/test/java/org/ormi/priv/tfa/orderflow/productregistry/ProductRegistryResourceTest.java`
- Correction du package
- Ajout des imports REST Assured
- Ajout de la dépendance `quarkus-rest-assured` dans `build.gradle`

### 2. Test Unitaires avec API Incorrecte

**Erreur** : Tests dans `libs/kernel` utilisaient une API incorrecte de la classe `Product`

**Correction** :
- Package corrigé
- Signature de méthode `Product.create()` corrigée (3 paramètres au lieu de 4)
- Utilisation de `ProductLifecycle` au lieu de `ProductStatus`
- Méthodes `updateName()` et `updateDescription()` au lieu de `update()`

### 3. Problèmes de Permissions Gradle sur /mnt/

**Erreur** :
```
Could not set file mode 777 on '.../application.yaml'
```

**Cause** : WSL ne peut pas modifier les permissions sur le système de fichiers Windows

**Solution** : Utilisation de `--no-daemon` ou copie du projet dans le système de fichiers Linux

Ces corrections garantissent que le projet compile et fonctionne correctement dans l'environnement WSL, prêt pour le développement et les tests.