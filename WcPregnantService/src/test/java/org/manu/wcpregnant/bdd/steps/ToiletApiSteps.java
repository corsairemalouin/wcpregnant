package org.manu.wcpregnant.bdd.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.manu.wcpregnant.bdd.ApiTestContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Étapes Cucumber de la suite de non-régression de l'API toilettes.
 *
 * <p>Chaque étape exercise l'application Spring Boot réelle via {@code MockMvc}
 * (authentification Basic, validation, services, repository H2) : les scénarios
 * décrivent donc le comportement fonctionnel observable par un client HTTP.</p>
 */
public class ToiletApiSteps {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private int status;
    private String body;
    private Long lastCreatedId;

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /**
     * Envoie une requête GET sans aucun identifiant.
     *
     * @param path chemin de l'API, ex. {@code /toilets}
     */
    @When("I call GET {string} without authentication")
    public void getWithoutAuthentication(String path) throws Exception {
        send("GET", path, null, null, null);
    }

    /**
     * Envoie une requête GET authentifiée par Basic.
     *
     * @param path     chemin de l'API, éventuellement avec query string
     * @param username identifiant
     * @param password mot de passe
     */
    @When("I call GET {string} as {string} with password {string}")
    public void getWithAuthentication(String path, String username, String password) throws Exception {
        send("GET", path, username, password, null);
    }

    /**
     * Envoie une requête POST authentifiée avec le payload fourni en doc string.
     *
     * @param path     chemin de l'API
     * @param username identifiant
     * @param password mot de passe
     * @param payload  corps JSON de la requête
     */
    @When("I call POST {string} as {string} with password {string} and body:")
    public void postWithAuthentication(String path, String username, String password, String payload) throws Exception {
        send("POST", path, username, password, payload);
    }

    /**
     * Met à jour la dernière toilette créée par le scénario.
     *
     * @param username identifiant
     * @param password mot de passe
     * @param payload  corps JSON de la requête
     */
    @When("I call PUT the last toilet as {string} with password {string} and body:")
    public void updateLastToilet(String username, String password, String payload) throws Exception {
        send("PUT", "/toilets/" + lastCreatedId, username, password, payload);
    }

    /**
     * Supprime la dernière toilette créée par le scénario.
     *
     * @param username identifiant
     * @param password mot de passe
     */
    @When("I call DELETE the last toilet as {string} with password {string}")
    public void deleteLastToilet(String username, String password) throws Exception {
        send("DELETE", "/toilets/" + lastCreatedId, username, password, null);
    }

    /**
     * Lit la dernière toilette créée par le scénario.
     *
     * @param username identifiant
     * @param password mot de passe
     */
    @When("I call GET the last toilet as {string} with password {string}")
    public void readLastToilet(String username, String password) throws Exception {
        send("GET", "/toilets/" + lastCreatedId, username, password, null);
    }

    // -----------------------------------------------------------------------
    // Vérifications
    // -----------------------------------------------------------------------

    /**
     * Vérifie le code HTTP de la dernière réponse.
     *
     * @param expected code HTTP attendu
     */
    @Then("the response status is {int}")
    public void checkStatus(int expected) {
        if (status != expected) {
            throw new AssertionError("Expected status " + expected + " but got " + status + " — body: " + body);
        }
    }

    /**
     * Vérifie le nombre exact de toilettes retournées.
     *
     * @param expected nombre attendu
     */
    @Then("the response contains {int} toilets")
    public void checkToiletCount(int expected) {
        int actual = toilets().size();
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " toilets but got " + actual + " — body: " + body);
        }
    }

    /**
     * Vérifie que le nombre de toilettes ne dépasse pas le plafond attendu.
     *
     * @param maximum borne haute attendue
     */
    @Then("the response contains at most {int} toilets")
    public void checkToiletLimit(int maximum) {
        int actual = toilets().size();
        if (actual > maximum) {
            throw new AssertionError("Expected at most " + maximum + " toilets but got " + actual);
        }
    }

    /**
     * Vérifie que la liste est triée par distance croissante.
     */
    @Then("toilets are sorted by ascending distance")
    public void checkDistanceOrder() {
        List<Double> distances = new ArrayList<>();
        for (Map<String, Object> toilet : toilets()) {
            distances.add(toDouble(toilet.get("distance")));
        }
        for (int i = 1; i < distances.size(); i++) {
            if (distances.get(i) < distances.get(i - 1)) {
                throw new AssertionError("Toilets are not sorted by ascending distance: " + distances);
            }
        }
    }

    /**
     * Vérifie le libellé de la première toilette de la réponse.
     *
     * @param libelle libellé attendu
     */
    @Then("the first toilet is {string}")
    public void checkFirstToilet(String libelle) {
        List<Map<String, Object>> toilets = toilets();
        if (toilets.isEmpty()) {
            throw new AssertionError("Expected first toilet '" + libelle + "' but the response is empty");
        }
        String actual = String.valueOf(toilets.get(0).get("libelle"));
        if (!actual.equals(libelle)) {
            throw new AssertionError("Expected first toilet '" + libelle + "' but got '" + actual + "'");
        }
    }

    /**
     * Vérifie la présence d'un texte dans le corps de la réponse.
     *
     * @param fragment fragment recherché
     */
    @Then("the response body contains {string}")
    public void checkBodyContains(String fragment) {
        if (body == null || !body.contains(fragment)) {
            throw new AssertionError("Expected body to contain '" + fragment + "' but got: " + body);
        }
    }

    /**
     * Vérifie que la dernière création a bien renvoyé un identifiant.
     */
    @Then("the created toilet has an identifier")
    public void checkCreatedIdentifier() {
        if (lastCreatedId == null) {
            throw new AssertionError("Created toilet did not return an identifier — body: " + body);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Exécute la requête HTTP et mémorise le statut et le corps de la réponse.
     *
     * @param method  méthode HTTP (GET, POST, PUT, DELETE)
     * @param path    chemin de l'API
     * @param username identifiant Basic, {@code null} pour une requête anonyme
     * @param password mot de passe Basic
     * @param payload corps JSON, {@code null} si la requête n'en contient pas
     * @throws Exception si l'exécution de la requête échoue
     */
    private void send(String method, String path, String username, String password, String payload) throws Exception {
        var request = switch (method) {
            case "GET" -> MockMvcRequestBuilders.get(path);
            case "POST" -> MockMvcRequestBuilders.post(path);
            case "PUT" -> MockMvcRequestBuilders.put(path);
            case "DELETE" -> MockMvcRequestBuilders.delete(path);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        if (username != null) {
            RequestPostProcessor basic = SecurityMockMvcRequestPostProcessors.httpBasic(username, password);
            request = request.with(basic);
        }
        if (payload != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(payload);
        }

        MvcResult result = ApiTestContext.mockMvc().perform(request).andReturn();
        status = result.getResponse().getStatus();
        body = result.getResponse().getContentAsString();

        if ("POST".equals(method) && status == 201) {
            lastCreatedId = extractId(body);
        }
    }

    /**
     * Convertit le corps JSON de liste en liste de maps.
     *
     * @param json corps JSON
     * @return la liste des objets JSON, vide si le corps est vide
     */
    private List<Map<String, Object>> toilets() {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        List<Object> raw = MAPPER.readValue(body, List.class);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            result.add((Map<String, Object>) item);
        }
        return result;
    }

    /**
     * Extrait l'identifiant d'une toilette créée.
     *
     * @param json corps JSON de la réponse de création
     * @return l'identifiant, ou {@code null} s'il est absent
     */
    private Long extractId(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Map<String, Object> toilet = MAPPER.readValue(json, Map.class);
        Object id = toilet.get("id");
        return id instanceof Number number ? number.longValue() : null;
    }

    /**
     * Convertit une valeur JSON en double.
     *
     * @param value valeur brute
     * @return la valeur numérique, {@code 0.0} si elle est absente
     */
    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}


