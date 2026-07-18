package org.manu.wcpregnant.service;

import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;

import java.util.List;
import java.util.Optional;

/**
 * Interface de service pour les toilettes (compatibilité ascendante).
 * <p>
 * Cette interface étend {@link ToiletteCrudService} et {@link ToiletteProximityService}
 * pour assurer la rétrocompatibilité avec le code existant qui dépendait de
 * l'interface unique {@code ToiletteService}.
 * </p>
 * <p>
 * Les nouvelles implémentations devraient préférer implémenter directement
 * {@link ToiletteCrudService} et/ou {@link ToiletteProximityService} pour
 * respecter le principe ISP (Interface Segregation Principle).
 * </p>
 *
 * @deprecated Utilisez {@link ToiletteCrudService} et {@link ToiletteProximityService} séparément
 */
@Deprecated(since = "1.0", forRemoval = false)
public interface ToiletteService extends ToiletteCrudService, ToiletteProximityService {
    // Méthodes héritées de ToiletteCrudService :
    //   Toilet addToilet(Toilet toiletteBO);
    //   List<Toilet> getAllToilets(Integer page, Integer size);
    //   Optional<Toilet> lireToilet(Long id);
    //   void updateToilet(Long id, Toilet toilet);
    //   void deleteToilet(Long id);

    // Méthodes héritées de ToiletteProximityService :
    //   List<Toilet> getNearestToilets(GeoPosition currentPosition);
}