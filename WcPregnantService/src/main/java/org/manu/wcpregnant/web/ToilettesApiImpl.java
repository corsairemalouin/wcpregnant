package org.manu.wcpregnant.web;

import lombok.RequiredArgsConstructor;
import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.web.api.ToiletApi;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor // Génère le constructeur pour l'injection du service
public class ToilettesApiImpl implements ToiletApi {

    private final ToiletteService service;

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

            // On délègue au service qui devrait idéalement prendre en compte page/size
            return ResponseEntity.ok(service.getNearsetToilettes(position));
        }

        // Sinon, on renvoie la liste classique (à implémenter dans votre service)
        return ResponseEntity.ok(service.getAllToilets(page, size));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Toilet> createToilet(Toilet toilet) {
        validateToilet(toilet);
        Toilet created = service.addToilet(toilet);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<Toilet> getToiletById(Long id) {
        return service.lireToilet(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 404 est plus correct que 204 si non trouvé
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateToilet(Long id, Toilet toilet) {
        validateToilet(toilet);
        service.updateToilet(id, toilet);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteToilet(Long id) {
        service.deleteToilet(id);
        return ResponseEntity.noContent().build();
    }

    // --- Validation ---

    private void validateToilet(Toilet toilet) {
        Assert.notNull(toilet, "Toilet object cannot be null");
        Assert.notNull(toilet.getAdress(), "Address is mandatory");
        Assert.hasText(toilet.getAdress().getCodePostal(), "Zip code is mandatory");
        Assert.notNull(toilet.getPosition(), "Geo-position is mandatory");
    }
}