package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.ToiletConverter;
import org.manu.wcpregnant.service.ToiletteCrudService;
import org.manu.wcpregnant.service.ToiletteProximityService;
import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.service.strategy.DistanceCalculator;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implémentation complète des services liés aux toilettes.
 *
 * <p>Cette classe respecte les principes SOLID suivants :</p>
 * <ul>
 *   <li><strong>SRP</strong> (Single Responsibility Principle) : chaque méthode a une
 *       responsabilité unique et bien définie. La méthode privée {@code enrichWithDistance}
 *       extrait le calcul de distance du flux principal.</li>
 *   <li><strong>OCP</strong> (Open/Closed Principle) : extensible via l'injection de
 *       {@link DistanceCalculator} sans modification du code existant.</li>
 *   <li><strong>ISP</strong> (Interface Segregation Principle) : implémente
 *       {@link ToiletteCrudService} et {@link ToiletteProximityService} séparément.</li>
 *   <li><strong>DIP</strong> (Dependency Inversion Principle) : dépend des abstractions
 *       ({@link DistanceCalculator}, {@link ToiletConverter}) plutôt que des
 *       implémentations concrètes.</li>
 * </ul>
 *
 * @see ToiletteCrudService
 * @see ToiletteProximityService
 * @see DistanceCalculator
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ToiletteServiceImpl implements ToiletteService {

    private final ToiletteServiceConfig config;
    private final ToiletteRepository toiletteRepository;
    private final ToiletConverter toiletteMapper;
    private final DistanceCalculator distanceCalculator;

    // ========================================================================
    // Opérations CRUD (ToiletteCrudService)
    // ========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Interroge la base de données via {@link ToiletteRepository#findAll(org.springframework.data.domain.Pageable)}
     * et convertit chaque entité {@link ToiletDAO} en objet métier {@link Toilet}
     * via le mapper {@link ToiletConverter#daoToBo(ToiletDAO)}.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<Toilet> getAllToilets(Integer page, Integer size) {
        Objects.requireNonNull(page, "page must not be null");
        Objects.requireNonNull(size, "size must not be null");
        log.debug("Fetching toilets page {} with size {}", page, size);
        return toiletteRepository.findAll(PageRequest.of(page, size))
                .map(toiletteMapper::daoToBo)
                .getContent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Convertit l'objet métier {@link Toilet} reçu en entité {@link ToiletDAO}
     * via le mapper {@link ToiletConverter#boToDao(Toilet)}, persiste l'entité
     * via le repository, puis reconvertit l'entité sauvegardée en objet métier.</p>
     */
    @Override
    public Toilet addToilet(Toilet toilet) {
        Objects.requireNonNull(toilet, "toilet must not be null");
        log.info("Adding new toilet: {}", toilet.getLibelle());
        ToiletDAO toiletteDAO = toiletteMapper.boToDao(toilet);
        ToiletDAO saved = toiletteRepository.save(toiletteDAO);
        return toiletteMapper.daoToBo(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Interroge le repository via {@link ToiletteRepository#findById(Object)}
     * et convertit l'entité {@link ToiletDAO} trouvée en objet métier {@link Toilet}.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Toilet> getToiletById(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        log.debug("Fetching toilet by id: {}", id);
        return toiletteRepository.findById(id)
                .map(toiletteMapper::daoToBo);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Récupère l'entité existante par son identifiant, applique les modifications
     * via {@link ToiletConverter#updateToiletteDAOFromBO(Toilet, ToiletDAO)},
     * puis sauvegarde l'entité mise à jour.</p>
     */
    @Override
    public void updateToilet(Long id, Toilet toilet) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(toilet, "toilet must not be null");
        log.info("Updating toilet id: {}", id);
        toiletteRepository.findById(id)
                .map(existingDAO -> {
                    toiletteMapper.updateToiletteDAOFromBO(toilet, existingDAO);
                    return toiletteRepository.save(existingDAO);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found with id: " + id));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Vérifie d'abord l'existence de la toilette via
     * {@link ToiletteRepository#existsById(Object)}, puis la supprime
     * via {@link ToiletteRepository#deleteById(Object)}.</p>
     */
    @Override
    public void deleteToilet(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        log.warn("Deleting toilet id: {}", id);
        if (!toiletteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete: Toilette not found with id: " + id);
        }
        toiletteRepository.deleteById(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Charge l'entité {@link ToiletDAO} depuis le repository, ajoute la note
     * à son historique via {@link ToiletConverter#gradeToilet(ToiletDAO, Graduation)},
     * puis persiste la toilette et renvoie l'objet métier converti.</p>
     */
    @Override
    public Toilet gradeToilet(Long id, Graduation graduation) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(graduation, "graduation must not be null");
        log.debug("Grading toilet id {} with graduation {}", id, graduation);

        ToiletDAO existingDAO = toiletteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found with id: " + id));

        ToiletDAO graded = toiletteMapper.gradeToilet(existingDAO, graduation);
        log.debug("Toilet {} average graduation is now {}", id, graded.getGrade());

        return toiletteMapper.daoToBo(toiletteRepository.save(existingDAO));
    }


    // ========================================================================
    // Recherche par proximité (ToiletteProximityService)
    // ========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Charge toutes les toilettes depuis la base de données, calcule la distance
     * pour chacune par rapport à la position donnée via {@link DistanceCalculator#calculate},
     * filtre selon la distance maximale configurée ({@link ToiletteServiceConfig#getMaxDistance()}),
     * trie par distance croissante et limite le nombre de résultats
     * selon {@link ToiletteServiceConfig#getMaxResult()}.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<Toilet> getNearestToilets(GeoPosition currentPosition) {
        Objects.requireNonNull(currentPosition, "currentPosition must not be null");
        log.debug("Searching nearest toilets for position: {}", currentPosition);
        return toiletteRepository.findAll().stream()
                .map(toiletteMapper::daoToBo)
                .map(toilet -> enrichWithDistance(toilet, currentPosition))
                .filter(toilet -> toilet.getDistance() != null && toilet.getDistance() < config.getMaxDistance())
                .sorted(Comparator.comparing(Toilet::getDistance, Comparator.nullsLast(Double::compareTo)))
                .limit(config.getMaxResult())
                .toList();
    }

    // ========================================================================
    // Méthodes privées
    // ========================================================================

    /**
     * Calcule et associe la distance d'une toilette par rapport à une position donnée.
     *
     * <p>Utilise le {@link DistanceCalculator} injecté pour calculer la distance
     * entre la position de la toilette et la position de référence. Si la toilette
     * ou sa position est {@code null}, la distance n'est pas calculée.</p>
     *
     * @param toilet           la toilette à enrichir avec sa distance. Peut être {@code null}.
     * @param currentPosition  la position de référence (latitude, longitude)
     * @return la toilette avec sa distance calculée, ou la toilette inchangée si
     *         {@code toilet} ou {@code toilet.getPosition()} est {@code null}
     */
    private Toilet enrichWithDistance(Toilet toilet, GeoPosition currentPosition) {
        if (toilet == null || toilet.getPosition() == null) {
            return toilet;
        }
        double distance = distanceCalculator.calculate(toilet.getPosition(), currentPosition);
        toilet.setDistance(distance == Double.MAX_VALUE ? null : distance);
        return toilet;
    }
}