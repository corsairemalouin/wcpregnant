package org.manu.wcpregnant.bdd.repository;

import org.manu.wcpregnant.bdd.dao.CustomerDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerDAO, Long> {
}