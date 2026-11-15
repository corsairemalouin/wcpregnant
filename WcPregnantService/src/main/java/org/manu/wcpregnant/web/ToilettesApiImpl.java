package org.manu.wcpregnant.web;

import lombok.RequiredArgsConstructor;
import org.manu.wcpregnant.service.ToiletteCrudService;
import org.manu.wcpregnant.service.ToiletteProximityService;
import org.manu.wcpregnant.service.ToiletteRatingService;
import org.manu.wcpregnant.web.api.ToiletApi;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Implémentation de l'API REST pour les toilettes.
 * <p>
 * Respecte le principe DIP (Dependency Inversion Principle) en dépendant
 * des interfaces {@link ToiletteCrudService} et {@link ToiletteProximityService}
 * plutôt que d'une implémentation concrète.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class ToilettesApiImpl implements ToiletApi {

    private final ToiletteCrudService crudService;
    private final ToiletteProximityService proximityService;
    private final ToiletteRatingService ratingService;

    /**
     * GET /toilets
     * Gère à la fois la liste simple, la pagination et la recherche par proximité.
     */
    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Toilet>> getToilets(Integer page, Integer size, Double latitude, Double longitude) {

        // Si on a des coordonnées, on cherche par proximité
        if (latitude != null && longitude != null) {
            GeoPosition position = GeoPosition.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();

            return ResponseEntity.ok(proximityService.getNearestToilets(position));
        }

        // Sinon, on renvoie la liste paginée classique
        return ResponseEntity.ok(crudService.getAllToilets(page, size));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Toilet> createToilet(Toilet toilet) {
        validateToilet(toilet);
        Toilet created = crudService.addToilet(toilet);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<Toilet> getToiletById(Long id) {
        return crudService.getToiletById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateToilet(Long id, Toilet toilet) {
        validateToilet(toilet);
        crudService.updateToilet(id, toilet);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteToilet(Long id) {
        crudService.deleteToilet(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /toilets/{id}/grade
     * Dépose une note (0-10 par critère) déposée par un visiteur sur une toilette.
     */
    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Toilet> gradeToilet(Long id, Graduation graduation) {
        Assert.notNull(id, "Toilet id cannot be null");
        Assert.notNull(graduation, "Graduation is mandatory");
        Toilet graded = ratingService.gradeToilet(id, graduation);
        return ResponseEntity.ok(graded);
    }


    // --- Validation ---

    private void validateToilet(Toilet toilet) {
        Assert.notNull(toilet, "Toilet object cannot be null");
        Assert.notNull(toilet.getAdress(), "Address is mandatory");
        Assert.hasText(toilet.getAdress().getCodePostal(), "Zip code is mandatory");
        Assert.notNull(toilet.getPosition(), "Geo-position is mandatory");
    }
}