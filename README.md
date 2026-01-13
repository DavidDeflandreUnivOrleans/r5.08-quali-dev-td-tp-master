# Order Flow - Projet de Qualité et Tests

## Contexte et Environnement de Développement

Ce projet a été développé sur un environnement **Windows** avec **WSL2 (Windows Subsystem for Linux)** pour garantir la compatibilité avec l'environnement Linux requis pour le rendu final. Cette configuration permet de développer dans un environnement Linux natif tout en conservant l'accès aux outils Windows.

### Prérequis

- **WSL2** avec Ubuntu (ou autre distribution Linux)
- **Java 21** (installé dans WSL)
- **Gradle 8.14+** (via wrapper)
- **Node.js 24+** (installé dans WSL)
- **pnpm 10.15+** (géré via Corepack)
- **Docker Desktop** (avec intégration WSL activée)

## Problèmes Rencontrés et Solutions

### 1. Limitations du Système de Fichiers Windows (/mnt/)

**Problème** : Quand le projet est situé sur le système de fichiers Windows (via `/mnt/l/...`), plusieurs limitations apparaissent :
- Impossible de modifier les permissions Unix avec `chmod`
- Gradle ne peut pas définir les permissions des fichiers générés
- `pnpm install` échoue avec des erreurs de permissions

**Solution** : Copier le projet dans le système de fichiers Linux natif de WSL :

```bash
mkdir -p ~/projects
cp -r /mnt/l/Documents/r5.08-quali-dev-td-tp-master ~/projects/
cd ~/projects/r5.08-quali-dev-td-tp-master
chmod +x gradlew
```

**Alternative** : Si vous devez travailler sur `/mnt/`, utiliser `bash ./gradlew --no-daemon` pour éviter les problèmes de permissions.

### 2. Installation de pnpm

**Problème** : L'installation globale de pnpm avec `npm install -g pnpm` échoue avec des erreurs de permissions.

**Solution** : Utilisation de `corepack`, la méthode recommandée par Node.js :

```bash
corepack enable
corepack prepare pnpm@latest --activate
```

### 3. Fichier de Test Mal Placé

**Problème** : Un fichier de test `ProductTest.java` était situé dans `src/main/java/.../test/` au lieu de `src/test/java/`, causant des erreurs de compilation car les dépendances de test (JUnit, Quarkus Test) ne sont pas disponibles dans `src/main/`.

**Solution** : 
- Suppression du fichier mal placé
- Création des tests d'intégration corrects dans `src/test/java/` :
  - `ProductRegistryCommandResourceTest.java` pour les tests d'intégration du service de commande
  - `ProductRegistryQueryResourceTest.java` pour les tests d'intégration du service de lecture

### 4. Dépendance quarkus-rest-assured Non Résolue

**Problème** : Gradle ne pouvait pas résoudre la version de `quarkus-rest-assured` lors de l'exécution des tests.

**Solution** : Modification de `build.gradle` pour utiliser `platform()` au lieu de `enforcedPlatform()` pour les dépendances de test, permettant à Gradle de résoudre correctement les versions via le BOM Quarkus.

## Modifications Apportées au Code

### Tests Unitaires (libs/kernel)

**Fichier** : `libs/kernel/src/test/java/org/ormi/priv/tfa/orderflow/kernel/product/ProductTest.java`

- Correction du package et des imports
- Correction des appels de méthode pour correspondre à l'API réelle de `Product`
- Utilisation de `ProductLifecycle` au lieu de `ProductStatus`
- Ajout de tests pour les cas invalides (nom vide, description null, etc.)

**Tests couverts** :
- Création d'un produit valide
- Création d'un produit invalide (nom null/vide, description null, skuId null)
- Mise à jour d'un produit valide
- Mise à jour d'un produit avec entrées invalides
- Mise à jour d'un produit dans un état invalide (retiré)
- Suppression d'un produit valide
- Suppression d'un produit dans un état invalide (déjà retiré)

### Tests d'Intégration (apps/product-registry-domain-service)

**Fichier** : `apps/product-registry-domain-service/src/test/java/org/ormi/priv/tfa/orderflow/productregistry/ProductRegistryCommandResourceTest.java`

**Tests couverts** :
- `POST /api/products` : création valide (201), invalide (400), sans corps (400)
- `PATCH /api/products/{id}/name` : mise à jour valide (204), invalide (400), sans corps (400)
- `DELETE /api/products/{id}` : suppression valide (204), produit inexistant (400)

### Tests d'Intégration (apps/product-registry-read-service)

**Fichier** : `apps/product-registry-read-service/src/test/java/org/ormi/priv/tfa/orderflow/productregistry/read/ProductRegistryQueryResourceTest.java`

**Tests couverts** :
- `GET /api/products` : recherche avec filtre correspondant (200), sans correspondance (200), sans filtre (200)
- `GET /api/products/{id}` : produit existant (200), produit inexistant (404)

### Modifications des Fichiers build.gradle

**Fichiers modifiés** :
- `apps/product-registry-domain-service/build.gradle`
- `apps/product-registry-read-service/build.gradle`

**Modifications** :
- Ajout de `testImplementation platform(project(":libs:bom-platform"))` pour résoudre les versions via le BOM
- Ajout de `testImplementation 'io.quarkus:quarkus-rest-assured'` pour les tests d'intégration
- Ajout de `testImplementation 'org.hamcrest:hamcrest'` pour les assertions

## Installation et Lancement

### 1. Préparation de l'Environnement

```bash
# Depuis WSL, aller dans le projet (si copié dans ~/projects)
cd ~/projects/r5.08-quali-dev-td-tp-master

# Ou depuis /mnt/ (Windows)
cd /mnt/l/Documents/r5.08-quali-dev-td-tp-master
```

### 2. Installation des Dépendances Node.js

```bash
# Activer Corepack
corepack enable
corepack prepare pnpm@latest --activate

# Installer les dépendances
pnpm install
```

### 3. Compilation du Projet Java

```bash
# Depuis le système de fichiers Linux (recommandé)
./gradlew build

# Depuis /mnt/ (Windows)
bash ./gradlew build --no-daemon
```

### 4. Exécution des Tests

```bash
# Tous les tests
./gradlew test

# Tests d'un module spécifique
./gradlew :libs:kernel:test
./gradlew :apps:product-registry-domain-service:test
./gradlew :apps:product-registry-read-service:test
```

### 5. Lancement des Services

#### Base de Données PostgreSQL

```bash
cd .devcontainer
docker-compose up -d postgresql liquibase
cd ..
```

#### Services Backend (dans des terminaux séparés)

```bash
# Service Product Registry Domain (écriture)
./gradlew :apps:product-registry-domain-service:quarkusDev

# Service Product Registry Read (lecture)
./gradlew :apps:product-registry-read-service:quarkusDev

# Service Store Back (Backend For Frontend)
./gradlew :apps:store-back:quarkusDev
```

#### Frontend Angular

```bash
pnpm run --filter apps-store-front start
```

## Structure du Projet

Le projet suit une architecture en couches (Onion Architecture) avec séparation CQRS :

- **Domain (Kernel)** : Logique métier pure, sans dépendances externes
- **Application** : Cas d'utilisation, orchestration
- **Infrastructure** : Implémentations techniques (JPA, REST clients)
- **Web/API** : Contrôleurs REST

### Modules Principaux

- `apps/product-registry-domain-service` : Service de commande (écriture)
- `apps/product-registry-read-service` : Service de lecture (requêtes)
- `apps/store-back` : Backend For Frontend
- `apps/store-front` : Frontend Angular
- `libs/kernel` : Logique métier du domaine
- `libs/cqrs-support` : Support CQRS (Command, Query, Event, Dispatcher)
- `libs/bom-platform` : Bill of Materials centralisé

## Documentation Complémentaire

- **Réponses aux TPs** : Voir `Reponses.md` pour les réponses détaillées aux exercices TP1 à TP4
- **Documentation technique** : Voir `doc/index.md` pour la documentation complète du projet

## Auteurs

- Étudiant : [Votre nom]
- Projet réalisé dans le cadre du cours de Qualité et Tests