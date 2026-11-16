package org.manu.wcpregnant.web;

import java.util.List;

import org.manu.wcpregnant.service.CustomerService;
import org.manu.wcpregnant.web.dto.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.manu.wcpregnant.web.api.CustomerApi;

/**
 * Implémentation de l'API "clients" (fonctionnalité encore en veille).
 *
 * <p>La classe n'est pas exposée comme contrôleur REST : les endpoints
 * {@code /customers} sont déclarés dans le contrat OpenAPI mais la persistance
 * des clients n'est pas encore implémentée. Les réponses renvoient donc une
 * liste vide ou {@code 501 Not Implemented}.</p>
 *
 * @see org.manu.wcpregnant.service.CustomerService
 */
//@RestController
//@RequestMapping("/wcpregnant/customers")
public class CustomerApiImpl implements  CustomerApi {
    private static final Logger log = LoggerFactory.getLogger(CustomerApiImpl.class);

    @Autowired
    CustomerService service;

    /**
     * POST /customers — réserve la création d'un client.
     *
     * <p>La gestion des comptes n'étant pas encore développée, l'opération
     * n'est volontairement pas traitée : elle renvoie systématiquement
     * {@code 501 Not Implemented}.</p>
     *
     * @param customer client à créer, ignoré dans l'état actuel du développement
     * @return une réponse {@code 501} sans corps
     */
    @Override
    public ResponseEntity<Void> createCustomer(Customer customer) {
        return new ResponseEntity<>( HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * GET /customers/{login} — retourne le client correspondant à un identifiant.
     *
     * <p>Le service renvoie un {@link Optional} : s'il est renseigné, la réponse
     * est {@code 200 OK} avec le client, sinon {@code 404 Not Found}.</p>
     *
     * @param login identifiant du client recherché
     * @return {@code 200} avec le client trouvé, {@code 404} sinon
     */
    @Override
    public ResponseEntity<Customer> getCustomerByLogin(String login) {
        // On récupère l'Optional depuis le service
        return service.getCustomerByLogin(login)
                // Si le client existe, on renvoie 200 OK avec le client
                .map(customer -> new ResponseEntity<>(customer, HttpStatus.OK))
                // Si l'Optional est vide, on renvoie 404 Not Found
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET /customers — liste l'ensemble des clients connus.
     *
     * @return une réponse {@code 200} contenant la liste des clients,
     *         potentiellement vide tant que la persistance n'est pas implémentée
     */
    @Override
    public ResponseEntity<List<Customer>> getCustomers() {
        return new ResponseEntity<>(service.getAllCustomers(), HttpStatus.OK);
    }
}
