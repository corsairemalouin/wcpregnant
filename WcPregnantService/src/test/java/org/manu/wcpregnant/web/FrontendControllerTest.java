package org.manu.wcpregnant.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du service de l'interface web (SPA Angular).
 *
 * <p>Un faux build est fabriqué dans un dossier temporaire : aucun artefact réel
 * de {@code WcPregnantApp} n'est nécessaire, ce qui rend les tests déterminants
 * y compris en CI.</p>
 */
@DisplayName("FrontendController - service du SPA Angular")
class FrontendControllerTest {

    @TempDir
    Path dist;

    /**
     * Fabrique un build Angular minimal (index.html + assets) et le contrôleur
     * pointant dessus.
     *
     * @return un contrôleur dont le dossier de build est garanti présent
     * @throws IOException si l'écriture des fichiers temporaires échoue
     */
    private FrontendController controllerWithBuild() throws IOException {
        Files.writeString(dist.resolve("index.html"), "<html><body><app-root></app-root></body></html>");
        Files.writeString(dist.resolve("main-ABC123.js"), "console.log('spa');");
        Files.writeString(dist.resolve("styles-DEF456.css"), "body { margin: 0; }");
        Files.createDirectories(dist.resolve("media"));
        Files.write(dist.resolve("media/marker-icon.png"), new byte[]{1, 2, 3});
        return new FrontendController(dist.toString());
    }

    @Test
    @DisplayName("index - build present : 200 et le HTML du SPA")
    void index_buildPresent_returnsIndexHtml() throws IOException {
        ResponseEntity<byte[]> response = controllerWithBuild().index();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        assertThat(new String(response.getBody())).contains("<app-root></app-root>");
    }

    @Test
    @DisplayName("index - dossier de build absent : 404 avec un message explicite")
    void index_buildMissing_returns404WithHint() {
        ResponseEntity<byte[]> response = new FrontendController(dist.resolve("no-build").toString()).index();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new String(response.getBody())).contains("npm run build");
    }

    @Test
    @DisplayName("index - FRONTEND_DIST_PATH explicite invalide : 404, sans repli")
    void index_explicitPathWithoutIndex_returns404() {
        // dist existe (TempDir) mais ne contient aucun index.html : le chemin explicite
        // fait foi et aucun repli sur les candidats par defaut n'est tente.
        ResponseEntity<byte[]> response = new FrontendController(dist.toString()).index();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("asset - fichier JS present : 200 et type application/javascript")
    void asset_jsFile_returnsContentWithJavascriptMime() throws IOException {
        ResponseEntity<byte[]> response = controllerWithBuild().asset("main-ABC123.js");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(new MediaType("application", "javascript"));
        assertThat(new String(response.getBody())).isEqualTo("console.log('spa');");
    }

    @Test
    @DisplayName("asset - fichier absent : 404")
    void asset_missingFile_returns404() throws IOException {
        ResponseEntity<byte[]> response = controllerWithBuild().asset("chunk-MISSING.js");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("nestedAsset - image du dossier media : 200 et type image/png")
    void nestedAsset_mediaImage_returnsPng() throws IOException {
        ResponseEntity<byte[]> response = controllerWithBuild().nestedAsset("media", "marker-icon.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("serveFile - remontee de chemin hors du build : 404")
    void serveFile_pathTraversal_isRejected() throws IOException {
        FrontendController controller = controllerWithBuild();
        Path outside = dist.getParent().resolve("leaked.txt");
        Files.writeString(outside, "secret");

        ResponseEntity<byte[]> response = controller.serveFile("../leaked.txt");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Files.delete(outside);
    }

    @Test
    @DisplayName("serveFile - extension inconnue : application/octet-stream")
    void serveFile_unknownExtension_returnsOctetStream() throws IOException {
        Files.writeString(dist.resolve("notes.unknown"), "texte");

        ResponseEntity<byte[]> response = controllerWithBuild().serveFile("notes.unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}
