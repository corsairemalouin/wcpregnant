package org.manu.wcpregnant.service.impl;

import java.util.List;
import java.util.Optional;

import org.manu.wcpregnant.service.CustomerService;
import org.manu.wcpregnant.web.dto.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    /**
     * Récupère la liste de tous les clients.
     * <p>
     * Pour l'instant, retourne une liste vide en attendant l'implémentation
     * de la persistence.
     * </p>
     *
     * @return une liste (potentiellement vide) des objets {@link Customer}
     */
    @Override
    public List<Customer> getAllCustomers() {
        log.debug("Fetching all customers");
        return List.of();
    }

    /**
     * Récupère un client par son login.
     * <p>
     * Pour l'instant, retourne {@link Optional#empty()} en attendant
     * l'implémentation de la persistence et de l'authentification.
     * </p>
     *
     * @param login le nom d'utilisateur du client recherché. Ne doit pas être {@code null}.
     * @return un {@link Optional} contenant le client si trouvé, {@link Optional#empty()} sinon
     */
    @Override
    public Optional<Customer> getCustomerByLogin(String login) {
        log.debug("Fetching customer by login: {}", login);
        return Optional.empty();
    }
}