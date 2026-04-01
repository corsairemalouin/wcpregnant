package org.manu.wcpregnant.bdd.repository;

import org.manu.wcpregnant.bdd.dao.Users;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
}
