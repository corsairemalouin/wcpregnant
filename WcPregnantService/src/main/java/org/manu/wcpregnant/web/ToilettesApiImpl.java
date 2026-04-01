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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/wcpregnant/toilets")
@EnableMethodSecurity
public class ToilettesApiImpl implements ToiletApi {

    @Autowired
    ToiletteService service;

    @GetMapping("/toilet")
    @Override
    @PreAuthorize("asRole('USER')")
    public ResponseEntity<Toilet> lireToilette(Long id) {
        Optional<Toilet> val = service.lireToilet(id);
        return val.isEmpty() ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(val.get(), HttpStatus.OK);
    }

    @PostMapping("/toilet")
    @PreAuthorize("asRole('ADMIN')")
    @Override
    public ResponseEntity<Void> toiletPost(Toilet val) {
        assertNotEmpty(val);
        service.addToilet(val);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/nearby")
    @Override
    @PreAuthorize("asRole('USER')")
    public ResponseEntity<List<Toilet>> getNearsetToilettes(GeoPosition position) {
        assertNotEmpty(position);
        return new ResponseEntity<>(service.getNearsetToilettes(position), HttpStatus.OK);
    }

    private void assertNotEmpty(Toilet toilet) {
        Assert.notNull(toilet, "missing mandatory parameter toilet");
        assertNotEmpty(toilet.getAdress());
        assertNotEmpty(toilet.getGrade());
        assertNotEmpty(toilet.getPosition());
    }

    private void assertNotEmpty(Adress adress) {
        Assert.notNull(adress, "missing mandatory parameter adress");
        Assert.notNull(adress.getCodePostal(), "missing mandatory parametercode postal");
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
