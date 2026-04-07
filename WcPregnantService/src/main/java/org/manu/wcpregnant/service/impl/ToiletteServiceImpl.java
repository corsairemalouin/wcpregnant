package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.service.utils.GeoPositionUtils;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ToiletteServiceImpl implements ToiletteService {

    private final ToiletteServiceConfig config;
    private final ToiletteRepository toiletteRepository;
    private final ToiletteDAOMapper toiletteMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Toilet> getAllToilets(Integer page, Integer size) {
        log.debug("Fetching toilets page {} with size {}", page, size);
        return toiletteRepository.findAll(PageRequest.of(page, size))
                .map(toiletteMapper::daoToBo)
                .getContent();
    }

    @Override
    public Toilet addToilet(Toilet toiletteBO) {
        log.info("Adding new toilet: {}", toiletteBO.getLibelle());
        ToiletDAO toiletteDAO = toiletteMapper.boToDao(toiletteBO);
        ToiletDAO saved = toiletteRepository.save(toiletteDAO);
        return toiletteMapper.daoToBo(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Toilet> getNearsetToilettes(GeoPosition currentPosition) {
        log.debug("Searching nearest toilets for position: {}", currentPosition);
        return toiletteRepository.findAll().stream()
                .map(toiletteMapper::daoToBo)
                // On calcule la distance pour chaque toilette
                .peek(t -> GeoPositionUtils.setDistance(t, currentPosition))
                // On filtre selon la config
                .filter(t -> t.getDistance() != null && t.getDistance() < config.getMaxDistance())
                // On trie du plus proche au plus loin
                .sorted(GeoPositionUtils::compareWithDistance)
                // On limite le nombre de résultats
                .limit(config.getMaxResult())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Toilet> lireToilet(Long id) {
        return toiletteRepository.findById(id)
                .map(toiletteMapper::daoToBo);
    }

    @Override
    public void updateToilet(Long id, Toilet toiletBO) {
        log.info("Updating toilet id: {}", id);
        toiletteRepository.findById(id)
                .map(existingDAO -> {
                    // Utilisation du mapper pour mettre à jour l'entité existante (évite de perdre l'ID)
                    toiletteMapper.updateToiletteDAOFromBO(toiletBO, existingDAO);
                    return toiletteRepository.save(existingDAO);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found with id: " + id));
    }

    @Override
    public void deleteToilet(Long id) {
        log.warn("Deleting toilet id: {}", id);
        if (!toiletteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete: Toilette not found with id: " + id);
        }
        toiletteRepository.deleteById(id);
    }
}