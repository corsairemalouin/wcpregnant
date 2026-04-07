package org.manu.wcpregnant.web;

import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.manu.wcpregnant.web.api.ToiletApi;

@RestController
@RequestMapping("/api/wcpregnant/toilets")
@EnableMethodSecurity
public class ToilettesApiImpl implements ToiletApi {

    @Autowired
    private ToiletteService service;

    /**
     * GET /toilet
     * Mappé via l'interface ToiletApi avec @RequestParam latitude et longitude
     */
    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Toilet>> getNearsetToilettes(Double latitude, Double longitude) {
        GeoPosition position = GeoPosition.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();

        assertNotEmpty(position);
        List<Toilet> toilets = service.getNearsetToilettes(position);
        return new ResponseEntity<>(toilets, HttpStatus.OK);
    }

    /**
     * GET /toilet/{id}
     * Mappé via l'interface ToiletApi
     */
    @Override
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Toilet> lireToilette(@PathVariable("id") Long id) {
        Optional<Toilet> val = service.lireToilet(id);
        return val.map(toilet -> new ResponseEntity<>(toilet, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    /**
     * POST /toilet
     * Note: Si cette méthode n'est pas dans l'interface ToiletApi,
     * on garde l'annotation explicite.
     */
    @PostMapping("/toilet")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toiletPost(@RequestBody Toilet val) {
        assertNotEmpty(val);
        service.addToilet(val);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // --- Méthodes de validation privées ---

    private void assertNotEmpty(Toilet toilet) {
        Assert.notNull(toilet, "missing mandatory parameter toilet");
        assertNotEmpty(toilet.getAdress());
        assertNotEmpty(toilet.getGrade());
        assertNotEmpty(toilet.getPosition());
    }

    private void assertNotEmpty(Adress adress) {
        Assert.notNull(adress, "missing mandatory parameter adress");
        Assert.notNull(adress.getCodePostal(), "missing mandatory parameter code postal");
    }

    private void assertNotEmpty(Graduation grade) {
        Assert.notNull(grade, "missing mandatory parameter grade");
    }

    private void assertNotEmpty(GeoPosition position) {
        Assert.notNull(position, "missing mandatory parameter position");
        Assert.notNull(position.getLongitude(), "missing mandatory parameter longitude");
        Assert.notNull(position.getLatitude(), "missing mandatory parameter latitude");
    }
}