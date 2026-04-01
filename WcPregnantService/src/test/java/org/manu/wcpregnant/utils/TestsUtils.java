package org.manu.wcpregnant.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.manu.wcpregnant.bdd.dao.AdressDAO;
import org.manu.wcpregnant.bdd.dao.GeoPositionDAO;
import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;

public class TestsUtils {

    private static GeoPosition buildGeoPosition() {
        GeoPosition result = new GeoPosition();
        result.setLatitude(48.607348);
        result.setLongitude(-1.8898742);
        return result;
    }

    private static GeoPositionDAO buildBddPosition(GeoPosition currentPosition) {
        double offset = ThreadLocalRandom.current().nextDouble() / 1000;
        GeoPositionDAO result = new GeoPositionDAO();
        result.setLatitude((currentPosition.getLatitude() + offset));
        result.setLongitude((currentPosition.getLongitude() + offset));
        return result;
    }

    public static ToiletDAO buildToiletDAO() {
        return buildToiletDAO("default value", buildBddPosition(buildGeoPosition()));
    }

    private static ToiletDAO buildToiletDAO(String name, GeoPositionDAO currentPosition) {
        ToiletDAO result = new ToiletDAO();
        result.setLibelle(name);
        result.setGrade(buildGraduationDAO());
        result.setPosition(currentPosition);
        result.setAdress(buildAdressDAO());
        return result;
    }

    private static GraduationDAO buildGraduationDAO() {
        GraduationDAO result = new GraduationDAO();
        result.setProprete(2);
        result.setAccueil(2);
        result.setEspace(2);
        return result;
    }

    private static AdressDAO buildAdressDAO() {
        AdressDAO result = new AdressDAO();
        result.setCodePostal("35350");
        return result;
    }

    public static Optional<Toilet> buildEmptyToilette() {
        return Optional.empty();
    }

    private static Adress buildAdress() {
        return Adress.builder().numero("19 bis").voie("rue de main").codePostal("35400").ville("st malo").pays("france")
                .build();
    }

    public static Toilet buildToilet() {
        return Toilet.builder().adress(buildAdress()).position(buildGeoPosition()).grade(buildGraduation())
                .libelle("default test value").distance(10.1).build();
    }

    private static Graduation buildGraduation() {
        return Graduation.builder().accueil(1).espace(2).proprete(3).build();
    }

    public static String buildToiletteJson() {
        return """
                {
                  "libelle": "tout pourri",
                  "adress": {
                    "numero": "7",
                    "voie": "Rue henry",
                    "codePostal": "35350",
                    "ville": " La Gouesniere",
                    "ligne4": "france"
                  },
                  "grade": {
                    "clean": 1,
                    "welcome": 1,
                    "space": 1
                  },
                  "position": {
                    "latitude": 48.0566,
                    "longitude": -2.001
                  }
                }\
                """;

    }

    public static List<ToiletDAO> buildBddContent(GeoPosition currentPosition) {
        List<ToiletDAO> result = new ArrayList<>();
        result.add(buildToiletDAO("a", buildBddPosition(currentPosition)));
        result.add(buildToiletDAO("b", buildBddPosition(currentPosition)));
        result.add(buildToiletDAO("c", buildBddPosition(currentPosition)));
        result.add(buildToiletDAO("d", buildBddPosition(currentPosition)));
        result.add(buildToiletDAO("e", buildBddPosition(currentPosition)));
        result.add(buildToiletDAO("f", buildBddPosition(currentPosition)));
        return result;
    }

}
