package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.bdd.dao.AdressDAO;
import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.bdd.dao.GeoPositionDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Alimente la base avec un jeu de données de démonstration autour de Rennes,
 * afin de pouvoir tester l'interface sans base PostgreSQL externe.
 *
 * <p>Le seed n'est exécuté que lorsque la base est vide : il ne modifie jamais
 * des données déjà présentes. Il est réservé au profil {@code dev}.</p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    /** Repository des toilettes à alimenter. */
    private final ToiletteRepository repository;

    /**
     * Insère le jeu de données de démonstration si la base ne contient encore
     * aucune toilette.
     *
     * @throws IllegalStateException si la persistance des données échoue
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedDemoData() {
        long alreadyPresent = repository.count();
        if (alreadyPresent > 0) {
            log.info("Database already contains {} toilets, demo data skipped", alreadyPresent);
            return;
        }

        List<ToiletDAO> demoToilets = buildDemoToilets();
        repository.saveAll(demoToilets);
        log.info("Seeded {} demo toilets around Rennes", demoToilets.size());
    }

    /**
     * Construit le jeu de toilettes publiques de démonstration.
     *
     * @return liste de {@link ToiletDAO} non persistées
     */
    private List<ToiletDAO> buildDemoToilets() {
        return List.of(
                toilet("Toilettes du Parc de Cesson", "2", "Rue de la Motte", "35130", "Cesson-Sévigné",
                        48.1234, -1.6612, 8, 7, 6),
                toilet("Toilettes Place de la République", "1", "Place de la République", "35000", "Rennes",
                        48.1119, -1.6745, 6, 8, 5),
                toilet("Toilette du Champ de Mars", "3", "Avenue du Champ de Mars", "35000", "Rennes",
                        48.1085, -1.6702, 7, 6, 7),
                toilet("Toilettes du Musée des Beaux-Arts", "4", "Rue de l'Herbier", "35000", "Rennes",
                        48.1063, -1.6723, 9, 9, 4),
                toilet("Toilette du Jardin Anglais", "1", "Quai Alphonse Le Guini", "35000", "Rennes",
                        48.1096, -1.6769, 5, 7, 6),
                toilet("Toilettes de la Gare de Rennes", "1", "Rue de la Gare", "35000", "Rennes",
                        48.1048, -1.6731, 6, 5, 8),
                toilet("Toilette du Stade Rennais", "1", "Route de l'Île de France", "35000", "Rennes",
                        48.0931, -1.6612, 8, 8, 9),
                toilet("Toilettes du Centre Commercial Atlantic", "2", "Rue de l'Île de France", "35000", "Rennes",
                        48.0911, -1.6598, 7, 7, 5),
                toilet("Toilette du Bois de la Motte", "5", "Chemin de la Motte", "35130", "Cesson-Sévigné",
                        48.1289, -1.6523, 4, 6, 7),
                toilet("Toilettes de l'Université Rennes 2", "3", "Place du Recteur Le Menn", "35014", "Rennes",
                        48.1189, -1.6778, 8, 7, 6)
        );
    }

    /**
     * Fabrique une {@link ToiletDAO} complète à partir des valeurs d'entrée.
     *
     * @param libelle   nom affiché de la toilette
     * @param numero    numéro de voie
     * @param voie      nom de la voie
     * @param codePostal code postal
     * @param ville     ville
     * @param latitude  latitude WGS84
     * @param longitude longitude WGS84
     * @param proprete  note de propreté (0-10)
     * @param accueil   note d'accueil (0-10)
     * @param espace    note d'espacement (0-10)
     * @return une {@link ToiletDAO} prête à être persistée
     */
    private ToiletDAO toilet(String libelle, String numero, String voie, String codePostal, String ville,
                             double latitude, double longitude, int proprete, int accueil, int espace) {
        AdressDAO adress = new AdressDAO();
        adress.setNumero(numero);
        adress.setVoie(voie);
        adress.setCodePostal(codePostal);
        adress.setVille(ville);
        adress.setPays("France");

        GraduationDAO graduation = new GraduationDAO();
        graduation.setProprete(proprete);
        graduation.setAccueil(accueil);
        graduation.setEspace(espace);

        GeoPositionDAO position = new GeoPositionDAO();
        position.setLatitude(latitude);
        position.setLongitude(longitude);

        ToiletDAO toilet = new ToiletDAO();
        toilet.setLibelle(libelle);
        toilet.setAdress(adress);
        toilet.setGrade(graduation);
        toilet.setPosition(position);
        return toilet;
    }
}
