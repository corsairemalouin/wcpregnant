package org.manu.wcpregnant.bdd.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité de notation d'une toilette.
 *
 * <p>Utilisée à la fois comme note agrégée affichée sur la fiche (relation
 * {@code grade} de {@link ToiletDAO}) et comme entrée de l'historique des
 * visites ({@code graduationHistory}).</p>
 */
@Getter
@Setter
@Entity
@Table(name = "graduation")
public class GraduationDAO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "accueil")
    private int accueil;

    @Column(name = "espace")
    private int espace;

    @Column(name = "proprete")
    private int proprete;

    /**
     * Horodatage de la note, renseigné automatiquement à la création de l'entrée.
     */
    @Column(name = "graded_at")
    private Instant gradedAt;

    /**
     * Constructeur par défaut : horodate la note à l'instant de création de l'entité.
     *
     * <p>Le champ {@code gradedAt} n'est jamais laissé {@code null}, ce qui garantit
     * que chaque entrée de l'historique des visites est datée, même lorsque la note
     * provient d'un seed de démonstration.</p>
     */
    public GraduationDAO() {
        this.gradedAt = Instant.now();
    }
}
