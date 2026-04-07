package org.manu.wcpregnant.service;

import java.util.List;
import java.util.Optional;

import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;

public interface ToiletteService {
    Toilet addToilet(Toilet toiletteBO);

    List<Toilet> getNearsetToilettes(GeoPosition geoPositionBO);

    Optional<Toilet> lireToilet(Long id);

    List<Toilet> getAllToilets(Integer page, Integer size);

    void updateToilet(Long id, Toilet toilet);

    void deleteToilet(Long id) ;
}