# Diagrammes d'architecture

> **Source** : [`assets/Diagrammes.pptx`](../assets/Diagrammes.pptx) — une seule diapositive contenant les étiquettes *présentation*, *IHM*, *Appli*, *Service*, *Service toilette*, *Persistance*, *admin*, *Google Geo*. Les diagrammes ci-dessous sont la **reconstruction Markdown** de cette esquisse, alignée sur le code réel.

---

## 1. Diagramme de composants (vue de l'esquisse d'origine)

```mermaid
graph TD
    IHM["IHM / Présentation<br/>(Angular + Leaflet)"] --> Appli["Appli<br/>(Spring Boot)"]
    Appli --> SVC["Service toilette"]
    SVC --> Persistance["Persistance<br/>(JPA / PostgreSQL)"]
    Admin["Admin"] --> Appli
    IHM --> GeoGeo["Google Geo<br/>(géocodage)"]
```

> Le géocodage externe n'est **pas** branché dans l'implémentation actuelle : la position est saisie ou fournie par le client. La distance est calculée en interne par la stratégie `DistanceCalculator` (Haversine).

---

## 2. Architecture réelle en couches

```mermaid
graph LR
    C["Angular client<br/>WcPregnantApp"] -->|HTTP /api/wcpregnant| A["web/<br/>ToilettesApiImpl<br/>CustomerApiImpl"]
    A --> S["service/<br/>ToiletteCrudService<br/>ToiletteProximityService"]
    S --> ST["service/strategy/<br/>DistanceCalculator<br/>(Haversine)"]
    S --> M["bdd/mapper/<br/>MapStruct"]
    M --> R["bdd/repository/<br/>Spring Data JPA"]
    R --> DB[("PostgreSQL")]
```

---

## 3. Séquence : recherche de proximité

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant F as Front Angular
    participant API as ToilettesApiImpl
    participant SVC as ToiletteServiceImpl
    participant DC as HaversineDistanceCalculator
    participant REPO as ToiletteRepository

    U->>F: Position GPS (lat, lon)
    F->>API: GET /toilets?latitude=..&longitude=..&page=0&size=20
    API->>SVC: getNearestToilets(currentPosition)
    SVC->>REPO: findAll()
    REPO-->>SVC: List<ToiletDAO>
    SVC->>DC: distance(positionA, positionB)
    DC-->>SVC: distance en km
    SVC-->>API: liste triée + filtrée (app.distance.max, app.result.max)
    API-->>F: JSON List<Toilet>
    F->>U: Carte + liste triée par distance
```

---

## 4. Diagramme de classes (noyau métier)

```mermaid
classDiagram
    class ToiletteCrudService {
        +addToilet(Toilet)
        +getAllToilets(Integer, Integer)
        +lireToilet(Long)
        +updateToilet(Long, Toilet)
        +deleteToilet(Long)
    }
    class ToiletteProximityService {
        +getNearestToilets(GeoPosition)
    }
    class ToiletteService {
        <<deprecated>>
    }
    class DistanceCalculator {
        +distance(GeoPosition, GeoPosition) double
    }
    class ToiletDAO {
        +Long id
        +String libelle
        +AdressDAO adress
        +GraduationDAO grade
        +GeoPositionDAO position
    }
    ToiletteService --|> ToiletteCrudService
    ToiletteService --|> ToiletteProximityService
    ToiletteProximityService ..> DistanceCalculator
    ToiletDAO --> AdressDAO
    ToiletDAO --> GraduationDAO
    ToiletDAO --> GeoPositionDAO
```

---

## 5. Déploiement cible

```mermaid
graph TD
    Ingress["Ingress nginx<br/>wcpregnant.example.com"] --> SVC["Service ClusterIP :80"]
    SVC --> P1["Pod app ×2 (HPA max 5)"]
    P1 --> PG["Pod PostgreSQL 15<br/>PVC 10 Gi"]
    P1 --> CM["ConfigMap"]
    P1 --> SEC["Secret"]
```

Détails : [kubernetes.md](../05-deploiement/kubernetes.md).
