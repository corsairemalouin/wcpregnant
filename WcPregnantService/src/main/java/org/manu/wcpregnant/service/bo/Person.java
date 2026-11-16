package org.manu.wcpregnant.service.bo;

/**
 * Personne du domaine métier, utilisée pour les échanges simples de type
 * "qui consulte la carte".
 *
 * @param name nom de la personne, peut être {@code null}
 * @param age  âge en années, peut être {@code null}
 */
public record Person(String name, Integer age) {
}
