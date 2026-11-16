# Conseil LLM (LLM Council) — garde-fous

Le skill `.cline/skills/llm-council/` (fork adapte de
[aiwithremy/claude-skills-llm-council](https://github.com/aiwithremy/claude-skills-llm-council),
methode d'Andrej Karpathy) permet de faire passer une decision par 5 conseillers
independants, une revue croisee anonymisee, puis un verdict de president.

## Quand l'activer

- **Uniquement** pour une decision genuine avec des enjeux, plusieurs options et un
  vrai compromis : architecture, arbitrage de technologie, priorisation de backlog,
  choix de deployement, scope d'un endpoint.
- **Jamais** pour une question factuelle, une tache de creation (« ecris un tweet »),
  un simple oui/non, ou une demande de validation d'une evidence.

## Contraintes non negociables

1. **Inférence 100 % locale** : le script `scripts/council.py` ne parle qu'a une API
   compatible OpenAI sur `localhost` / réseau privé (`AGENT_INFERENCE_BASE_URL`).
   Aucun appel a un LLM distant. Si le serveur local n'est pas disponible, utiliser le
   mode fallback interne (Cline incarne les 5 angles) — jamais un fournisseur cloud.
2. **Aucun secret dans la question** : ne pas injecter de mot de passe, token ou
   credential PostgreSQL dans le contexte transmis aux conseillers.
3. **Le conseil ne remplace pas la validation** : `mvn -q test` (backend) et
   `npm run build` + `npm run test` (frontend) restent obligatoires avant toute
   conclusion. Un verdict du conseil n'autorise pas de contourner les guardrails
   (`Waste/**`, `developpement/**`, code genere OpenAPI, seuil JaCoCo).
4. **RBAC et contrat d'API inchanges** : une recommandation du conseil ne peut pas
   proposer de contourner `@PreAuthorize` ni d'éditer les classes generees
   `web/api/**` / `web/dto/**`.
5. **Transcripts** : écrits dans `council-transcripts/` (ignoré par Git). Ne jamais
   les committer.

## Restitution

Le verdict est rendu en conversation, en français, dans le format
`templates/verdict.md` : accord / divergence / angles morts / recommandation /
premiere action.
