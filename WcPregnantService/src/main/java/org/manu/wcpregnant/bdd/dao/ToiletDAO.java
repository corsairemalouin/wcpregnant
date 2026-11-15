package org.manu.wcpregnant.bdd.dao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "toilet")
public class ToiletDAO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "libelle")
    private String libelle;

    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "adress_id")
    private AdressDAO adress;

    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "grade_id")
    private GraduationDAO grade;

    /**
     * Historique des notes déposées par les visiteurs, de la plus récente à la
     * plus ancienne. {@code grade} contient la moyenne de ces entrées.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "toilet_id", referencedColumnName = "id")
    @OrderBy("id DESC")
    private List<GraduationDAO> graduationHistory = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "position_id")
    private GeoPositionDAO position;

}