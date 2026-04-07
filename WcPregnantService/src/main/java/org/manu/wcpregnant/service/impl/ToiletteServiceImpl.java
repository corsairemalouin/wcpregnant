package org.manu.wcpregnant.service.impl;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.ToiletteService;
import org.manu.wcpregnant.service.utils.GeoPositionUtils;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@Transactional
public class ToiletteServiceImpl implements ToiletteService {
    private static final Logger log = LoggerFactory.getLogger(ToiletteServiceImpl.class);

    @Autowired
    ToiletteServiceConfig config;

    @Autowired
    private ToiletteRepository toiletteRepository;

    @Autowired
    private ToiletteDAOMapper toiletteMapper;

    public List<Toilet> getAllToilettes() {
        return toiletteRepository.findAll().stream().map(toiletteMapper::daoToBo).toList();
    }

    public Toilet addToilet(Toilet toiletteBO) {
        ToiletDAO toiletteDAO = toiletteMapper.boToDao(toiletteBO);

        ToiletDAO saveBOilette = toiletteRepository.save(toiletteDAO);
        return toiletteMapper.daoToBo(saveBOilette);
    }

    @Override
    public List<Toilet> getNearsetToilettes(GeoPosition currentPosition) {
        return toiletteRepository.findAll().stream().map(t -> toiletteMapper.daoToBo(t))
                .peek(p -> GeoPositionUtils.setDistance(p, currentPosition))
                .filter(t -> t.getDistance() < config.getMaxDistance()).sorted(GeoPositionUtils::compareWithDistance)
                .limit(config.getMaxResult()).toList();
    }

    private void calcDistance(GeoPosition currentToilet, GeoPosition currentPosition) {
    }

    @Override
    public Optional<Toilet> lireToilet(Long id) {
        return toiletteRepository.findById(id).stream().map(t -> toiletteMapper.daoToBo(t)).findFirst();
    }

    public Toilet updateToilette(Long id, Toilet toiletteBO) throws ResourceNotFoundException {
        ToiletDAO existingToiletteDAO = toiletteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found"));
        toiletteMapper.updateToiletteDAOFromBO(toiletteBO, existingToiletteDAO);
        toiletteRepository.save(existingToiletteDAO);
        return toiletteMapper.daoToBo(existingToiletteDAO);
    }

    public Toilet updateToilet(Toilet existing, Toilet newData) {
        existing.setAdress(isEmpty(newData.getAdress()) ? existing.getAdress() : newData.getAdress());
        existing.setGrade(isEmpty(newData.getGrade()) ? existing.getGrade() : newData.getGrade());
        return existing;
    }

    public void deleteToilette(Long id) {
        toiletteRepository.deleteById(id);
    }


}