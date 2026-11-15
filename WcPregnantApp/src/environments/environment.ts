/**
 * Configuration de l'environnement de développement.
 * L'URL de l'API passe par le proxy Angular (src/proxy.conf.json) afin
 * d'éviter toute contrainte CORS et tout codage en dur du host.
 */
export const environment = {
  apiUrl: '/toilets',
  /** Base des endpoints agentiques (Tool Calling) du service Spring Boot. */
  agentApiUrl: '/agent',
  defaultPosition: {
    latitude: 48.1119,
    longitude: -1.6745
  }
};
