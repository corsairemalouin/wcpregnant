package org.manu.wcpregnant.bdd.repository;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToiletteRepository extends JpaRepository<ToiletDAO, Long> {
}