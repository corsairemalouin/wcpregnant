package org.manu.wcpregnant.agent.tool;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Résultat normalisé d'un appel d'outil, format unique renvoyé au LLM quelque
 * soit l'outil exécuté.
 *
 * <p>Le champ {@code warning} permet d'informer le modèle d'une limite du
 * système (par exemple un filtre non encore supporté par le modèle de données)
 * sans transformer l'appel en erreur.</p>
 *
 * @see ToolExecutor
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolResult(
        String tool,
        String status,
        int count,
        Object data,
        String warning
) {

    /**
     * Constructeur validant : l'outil et le statut sont obligatoires.
     *
     * @param tool    nom de l'outil ayant produit le résultat
     * @param status  statut logique de l'opération ({@code OK}, {@code CREATED}, ...)
     * @param count   nombre d'éléments contenus dans {@code data}
     * @param data    payload de la réponse, peut être {@code null}
     * @param warning avertissement destiné au LLM, {@code null} si aucune
     * @throws IllegalArgumentException si {@code tool} ou {@code status} est vide
     */
    public ToolResult {
        Objects.requireNonNull(tool, "tool must not be null");
        if (tool.isBlank()) {
            throw new IllegalArgumentException("tool must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        if (status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
    }

    /**
     * Résultat portant une collection : le {@code count} est déduit de la taille
     * de la liste.
     *
     * @param tool    nom de l'outil
     * @param status  statut de l'opération
     * @param elements éléments retournés, ne doit pas être {@code null}
     * @param warning avertissement éventuel, peut être {@code null}
     * @return un {@link ToolResult} dont {@code count} est la taille de la liste
     */
    public static ToolResult ofList(String tool, String status, List<?> elements, String warning) {
        Objects.requireNonNull(elements, "elements must not be null");
        return new ToolResult(tool, status, elements.size(), elements, warning);
    }

    /**
     * Résultat portant un élément unique.
     *
     * @param tool    nom de l'outil
     * @param status  statut de l'opération
     * @param data    l'élément retourné, ne doit pas être {@code null}
     * @param warning avertissement éventuel, peut être {@code null}
     * @return un {@link ToolResult} avec {@code count} égal à 1
     */
    public static ToolResult ofOne(String tool, String status, Object data, String warning) {
        Objects.requireNonNull(data, "data must not be null");
        return new ToolResult(tool, status, 1, data, warning);
    }
}
