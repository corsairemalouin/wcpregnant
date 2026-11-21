package org.manu.wcpregnant.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptateur de présentation de l'interface web.
 *
 * <p>Le service REST n'est pas seulement une API : il sert aussi le build Angular
 * (SPA) afin que {@code http://localhost:14007/} affiche directement le menu de
 * l'application au lieu de la page de connexion Spring. Ce contrôleur ne contient
 * aucune logique métier : il se contente de lire les fichiers statiques produits
 * par {@code npm run build}.</p>
 *
 * <p>Le dossier du build est résolu dans cet ordre :</p>
 * <ol>
 *   <li>{@code app.frontend.dist} (variable d'environnement {@code FRONTEND_DIST_PATH}),
 *       s'il est renseigné : c'est le chemin utilisé dans l'image Docker
 *       ({@code /app/frontend-dist}) ;</li>
 *   <li>les emplacements de développement locaux
 *       ({@code WcPregnantApp/dist/WcPregnantApp/browser} et ses variantes).</li>
 * </ol>
 */
@RestController
@Slf4j
public class FrontendController {

    /** Fichier d'entrée du SPA Angular. */
    private static final String INDEX_FILE = "index.html";

    /** Types MIME des artefacts du build, par extension. */
    private static final Map<String, MediaType> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", MediaType.TEXT_HTML),
            Map.entry("htm", MediaType.TEXT_HTML),
            Map.entry("js", new MediaType("application", "javascript")),
            Map.entry("css", new MediaType("text", "css")),
            Map.entry("json", MediaType.APPLICATION_JSON),
            Map.entry("map", MediaType.APPLICATION_JSON),
            Map.entry("png", MediaType.IMAGE_PNG),
            Map.entry("jpg", MediaType.IMAGE_JPEG),
            Map.entry("jpeg", MediaType.IMAGE_JPEG),
            Map.entry("gif", MediaType.IMAGE_GIF),
            Map.entry("svg", new MediaType("image", "svg+xml")),
            Map.entry("ico", new MediaType("image", "x-icon")),
            Map.entry("woff", new MediaType("font", "woff")),
            Map.entry("woff2", new MediaType("font", "woff2"))
    );

    /**
     * Emplacements testés lorsque {@code FRONTEND_DIST_PATH} n'est pas défini.
     * Le premier dossier contenant {@code index.html} est retenu.
     */
    private static final List<Path> DEFAULT_CANDIDATES = List.of(
            Path.of("frontend-dist"),
            Path.of("WcPregnantApp", "dist", "WcPregnantApp", "browser"),
            Path.of("..", "WcPregnantApp", "dist", "WcPregnantApp", "browser"),
            Path.of("src", "main", "resources", "static")
    );

    /** Dossier racine du build Angular, ou {@code null} si le SPA n'est pas disponible. */
    private final Path distRoot;

    /**
     * Résout le dossier du build Angular au démarrage de l'application.
     *
     * <p>Si aucun dossier contenant {@code index.html} n'est trouvé, le contrôleur
     * reste actif mais répond {@code 404} avec un message explicite : le comportement
     * de l'API REST n'est pas modifié.</p>
     *
     * @param configuredDist dossier du build indiqué par {@code app.frontend.dist}
     *                       / {@code FRONTEND_DIST_PATH} ; peut être vide
     */
    public FrontendController(@Value("${app.frontend.dist:}") String configuredDist) {
        this.distRoot = resolveDistRoot(configuredDist);

        if (this.distRoot == null) {
            log.warn("Build Angular introuvable : l'interface web ne sera pas servie sur / "
                    + "(FRONTEND_DIST_PATH='{}', candidats={})", configuredDist, DEFAULT_CANDIDATES);
        } else {
            log.info("Interface web Angular servie depuis {}", this.distRoot);
        }
    }

    /**
     * Sert la page d'accueil du SPA Angular.
     *
     * @return le contenu de {@code index.html}, ou un {@code 404} explicite si le
     *         build n'est pas présent
     */
    @GetMapping("/")
    public ResponseEntity<byte[]> index() {
        return serveFile(INDEX_FILE);
    }

    /**
     * Sert un asset du build situé à la racine du SPA
     * (ex. {@code /main-OK3D7S4E.js}, {@code /styles-B6V7BVVK.css}).
     *
     * <p>Le motif n'accepte que les noms de fichiers comportant une extension, ce
     * qui évite toute collision avec les routes REST ({@code /toilets/{id}}).</p>
     *
     * @param file nom du fichier demandé
     * @return le contenu du fichier, ou {@code 404} s'il est absent
     */
    @GetMapping("/{file:[A-Za-z0-9][A-Za-z0-9._-]*\\.[A-Za-z0-9]{1,5}}")
    public ResponseEntity<byte[]> asset(@PathVariable String file) {
        return serveFile(file);
    }

    /**
     * Sert un asset situé dans un sous-dossier du build
     * (ex. {@code /media/marker-icon-2V3QKKVC.png} référencé par la CSS Leaflet).
     *
     * @param dir  nom du sous-dossier
     * @param file nom du fichier
     * @return le contenu du fichier, ou {@code 404} s'il est absent
     */
    @GetMapping("/{dir:[A-Za-z0-9_-]+}/{file:[A-Za-z0-9][A-Za-z0-9._-]*\\.[A-Za-z0-9]{1,5}}")
    public ResponseEntity<byte[]> nestedAsset(@PathVariable String dir, @PathVariable String file) {
        return serveFile(dir + "/" + file);
    }

    /**
     * Lit un fichier du build Angular en le protégeant contre les remontées de chemin.
     *
     * @param relativePath chemin relatif au dossier du build, ex. {@code media/logo.png}
     * @return la réponse HTTP contenant le fichier, ou {@code 404} / {@code 500}
     */
    ResponseEntity<byte[]> serveFile(String relativePath) {
        if (distRoot == null) {
            log.debug("Interface web non construite : demande '{}' ignorée", relativePath);
            return frontendNotBuilt();
        }

        Path file = distRoot.resolve(relativePath).normalize();
        if (!file.startsWith(distRoot)) {
            log.warn("Demande d'asset hors du build Angular rejetée : {}", relativePath);
            return ResponseEntity.notFound().build();
        }

        if (!Files.isRegularFile(file)) {
            log.debug("Asset '{}' introuvable dans le build Angular", relativePath);
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] content = Files.readAllBytes(file);
            log.debug("Asset '{}' servi ({} octets)", relativePath, content.length);
            return ResponseEntity.ok().contentType(contentType(file)).body(content);
        } catch (IOException e) {
            log.error("Lecture de l'asset '{}' du build Angular impossible", file, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Réponse explicite lorsque le SPA n'a pas été construit.
     *
     * @return un {@code 404} texte invitant à lancer {@code npm run build}
     */
    private ResponseEntity<byte[]> frontendNotBuilt() {
        String message = "L'interface web n'est pas construite. Lancez 'npm run build' dans "
                + "WcPregnantApp ou définissez FRONTEND_DIST_PATH sur le dossier du build Angular.";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Détermine le type MIME d'un artefact à partir de son extension.
     *
     * @param file chemin du fichier servi
     * @return le type MIME connu, sinon {@code application/octet-stream}
     */
    private static MediaType contentType(Path file) {
        String name = Objects.requireNonNull(file).getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return CONTENT_TYPES.getOrDefault(name.substring(dot + 1), MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Choisit le dossier du build Angular.
     *
     * <p>Une valeur explicite fait foi : si elle ne contient pas {@code index.html},
     * aucune recherche de repli n'est effectuée, ce qui rend le comportement
     * prévisible en conteneur.</p>
     *
     * @param configuredDist valeur de {@code app.frontend.dist}
     * @return le dossier résolu, ou {@code null} si le SPA est indisponible
     */
    private static Path resolveDistRoot(String configuredDist) {
        if (configuredDist != null && !configuredDist.isBlank()) {
            Path explicit = Path.of(configuredDist.trim());
            return isSpaRoot(explicit) ? explicit : null;
        }
        return DEFAULT_CANDIDATES.stream()
                .filter(FrontendController::isSpaRoot)
                .findFirst()
                .orElse(null);
    }

    /**
     * Détecte un dossier de SPA valide, c'est-à-dire contenant {@code index.html}.
     *
     * @param dir dossier candidat
     * @return {@code true} si le dossier est un build Angular exploitable
     */
    private static boolean isSpaRoot(Path dir) {
        return dir != null && Files.isRegularFile(dir.resolve(INDEX_FILE));
    }
}



