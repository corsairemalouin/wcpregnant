#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Conseil LLM (LLM Council) — orchestrateur local adapte a Cline.

Fork local de https://github.com/aiwithremy/claude-skills-llm-council
(skill originale d'Ole Lehmann, methodologie d'Andrej Karpathy).

Pipeline :
  1. cadrage de la question + enrichissement par le contexte du projet
  2. 5 conseillers independants (inférence locale, API compatible OpenAI)
  3. revue croisee anonymisee (A..E, mapping melange)
  4. verdict du president (synthese)
  5. restitution markdown + transcript optionnel

Inférence 100 % locale : aucune dependance externe (bibliotheque standard).
Variables d'environnement :
  AGENT_INFERENCE_BASE_URL  (defaut http://localhost:8000/v1)
  AGENT_MODEL               (defaut Qwen-3.8-Flash-Next)
  AGENT_TIMEOUT_SECONDS     (defaut 60)
"""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import random
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

ADVISORS = [
    ("Le Contrariant",
     "Cherche activement ce qui est faux, manquant ou fragile. Part du principe "
     "que l'idee a un defaut fatal et cherche lequel. Si tout semble solide, creuse "
     "plus loin. Pas du pessimisme : les questions que l'auteur evite."),
    ("Le Penseur First Principles",
     "Ignore la question de surface et demande ce que l'on cherche reellement a "
     "resoudre. Supprime les hypotheses, reconstruit le probleme depuis la base. "
     "Signale si la mauvaise question est posee."),
    ("L'Expansionniste",
     "Cherche le potentiel que les autres ne voient pas : ce qui pourrait etre plus "
     "grand, l'opportunite adjacente, ce qui est sous-evalue. Ne s'occupe pas du "
     "risque, seulement de ce qui se passe si cela fonctionne mieux que prevu."),
    ("L'Outsider",
     "Aucun contexte sur le projet, le domaine ou l'histoire de l'auteur. Repond "
     "uniquement a ce qui est ecrit. Repere la malédiction du savoir : ce qui est "
     "evident pour l'auteur et obscur pour tous les autres."),
    ("L'Executeur",
     "Une seule question : est-ce realisable, et quel est le chemin le plus court ? "
     "Ignore la théorie et la grande stratégie. Juge chaque idee par « que fait-on "
     "lundi matin ? »."),
]

ADVISOR_SYSTEM = (
    "Tu es {name}, conseiller d'un Conseil LLM.\n"
    "Style de pensee : {lens}\n"
    "Réponds depuis ton angle uniquement, sans nuance ni recherche d'équilibre : "
    "les autres conseillers couvrent les angles restants. 150 a 300 mots en francais, "
    "sans preambule, directement l'analyse."
)

REVIEWER_SYSTEM = (
    "Tu es un pair evaluateur du Conseil LLM. Cinq conseillers ont repondu "
    "independamment a la meme question ; leurs réponses sont anonymisees de A a E.\n"
    "Reponds en 200 mots maximum, en francais, en citant les lettres :\n"
    "1) Quelle reponse est la plus solide, et pourquoi ?\n"
    "2) Quelle reponse a le plus grand angle mort, et que manque-t-elle ?\n"
    "3) Que ont TOUTES les reponses oublie que le Conseil devrait considerer ?"
)

CHAIRMAN_SYSTEM = (
    "Tu es le president du Conseil LLM. Synthese les 5 analyses et les 5 revues "
    "croisees en un verdict actionnable, en francais, exactement dans ce format :\n"
    "## Ou le Conseil est d'accord\n"
    "## Ou le Conseil diverge\n"
    "## Angles morts detectes\n"
    "## La recommandation\n"
    "## La premiere action concrete\n"
    "Sois direct : une recommandation reelle, pas « cela depend ». Tu peux "
    "contredire la majorite si le raisonnement du dissident est le plus solide."
)


def _env(name: str, default: str) -> str:
    """Retourne une variable d'environnement nettoyee, ou sa valeur par defaut."""
    value = os.environ.get(name)
    return value.strip() if value and value.strip() else default


def _chat(base_url: str, model: str, timeout: int, temperature: float,
          system: str, prompt: str, mock: bool) -> str:
    """Appelle le serveur d'inférence local (API compatible OpenAI).

    En mode `mock`, renvoie une reponse de demonstration sans aucun appel reseau :
    cela permet de valider la pipeline du conseil hors ligne.
    """
    if mock:
        label = system.splitlines()[0].replace("Tu es ", "").split(",")[0]
        subject = prompt.splitlines()[0].strip()[:120]
        return f"[MODE MOCK] {label} - analyse de demonstration sur : {subject}"

    url = base_url.rstrip("/") + "/chat/completions"
    payload = {
        "model": model,
        "temperature": temperature,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
    }
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.URLError as exc:
        raise RuntimeError(
            f"Serveur d'inférence local injoignable ({url}) : {exc}\n"
            "Demarrer Strata AI (ex. http://localhost:8000/v1) ou relancer avec --mock."
        ) from exc

    choices = body.get("choices") or []
    if not choices:
        raise RuntimeError(f"Reponse du modele sans 'choices' : {body}")
    content = (choices[0].get("message") or {}).get("content", "")
    return content.strip()


def _read_context(paths: list[str], max_chars: int) -> str:
    """Concatenne les fichiers de contexte tronques, en ignorant les illisibles."""
    chunks: list[str] = []
    for path in paths:
        try:
            with open(path, "r", encoding="utf-8", errors="replace") as handle:
                text = handle.read(max_chars)
        except OSError as exc:
            print(f"avertissement : contexte '{path}' ignore ({exc})", file=sys.stderr)
            continue
        if text.strip():
            chunks.append(f"### {path}\n{text.strip()}")
    return "\n\n".join(chunks)


def frame_question(question: str, context: str, stakes: str) -> str:
    """Cadre la question de facon neutre pour les 5 conseillers."""
    framed = question.strip()
    if context:
        framed += "\n\nCONTEXTE DU PROJET :\n" + context
    if stakes:
        framed += "\n\nENJEUX :\n" + stakes
    return framed


def convene(cfg, framed: str) -> list[str]:
    """Interroge les 5 conseillers en parallele (independance reelle)."""
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(ADVISORS)) as pool:
        futures = [
            pool.submit(_chat, cfg.base_url, cfg.model, cfg.timeout, cfg.temperature,
                       ADVISOR_SYSTEM.format(name=name, lens=lens), framed, cfg.mock)
            for name, lens in ADVISORS
        ]
        return [future.result() for future in futures]


def anonymize(responses: list[str], seed: int) -> tuple[list[str], dict[str, str]]:
    """Melange conseiller -> lettre pour supprimer tout biais de position."""
    order = list(range(len(responses)))
    random.Random(seed).shuffle(order)
    letters = [chr(ord("A") + index) for index in range(len(responses))]
    mapping = {letter: responses[position] for letter, position in zip(letters, order)}
    return letters, mapping


def peer_review(cfg, framed: str, letters: list[str], mapping: dict[str, str]) -> list[str]:
    """Chaque conseiller revue les 5 reponses anonymisees."""
    block = "\n\n".join(f"**Reponse {letter} :**\n{mapping[letter]}" for letter in letters)
    prompt = f"Question du Conseil :\n---\n{framed}\n---\n\n{block}"
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(letters)) as pool:
        futures = [pool.submit(_chat, cfg.base_url, cfg.model, cfg.timeout, cfg.temperature,
                               REVIEWER_SYSTEM, prompt, cfg.mock) for _ in letters]
        return [future.result() for future in futures]


def chairman(cfg, framed: str, responses: list[str], reviews: list[str]) -> str:
    """Synthese finale du president : analyses de-anonymisees + revues croisees."""
    advisor_block = "\n\n".join(
        f"**{name} :**\n{answer}" for (name, _), answer in zip(ADVISORS, responses)
    )
    review_block = "\n\n".join(
        f"**Revue {index + 1} :**\n{review}" for index, review in enumerate(reviews)
    )
    prompt = (
        f"Question apportee au Conseil :\n---\n{framed}\n---\n\n"
        f"ANALYSES DES CONSEILLERS :\n{advisor_block}\n\n"
        f"REVUES CROISEES :\n{review_block}"
    )
    return _chat(cfg.base_url, cfg.model, cfg.timeout, cfg.temperature,
                 CHAIRMAN_SYSTEM, prompt, cfg.mock)


def write_transcript(path: str, framed: str, responses: list[str],
                     reviews: list[str], verdict: str, model: str) -> None:
    """Ecrit le transcript complet du conseil en Markdown."""
    directory = os.path.dirname(path)
    if directory:
        os.makedirs(directory, exist_ok=True)
    timestamp = datetime.now(timezone.utc).isoformat(timespec="seconds")
    lines = [
        "# Transcript du Conseil LLM",
        "",
        f"- Date : {timestamp}",
        f"- Modele : {model}",
        f"- Conseillers : {', '.join(name for name, _ in ADVISORS)}",
        "",
        "## Question cadre",
        "",
        framed,
        "",
        "## Analyses des conseillers",
        "",
    ]
    for (name, _), answer in zip(ADVISORS, responses):
        lines += [f"### {name}", "", answer, ""]
    lines += ["## Revues croisees (anonymisees)", ""]
    for index, review in enumerate(reviews, start=1):
        lines += [f"### Revue {index}", "", review, ""]
    lines += ["## Verdict du president", "", verdict, ""]
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Conseil LLM : 5 conseillers independants, revue croisee "
                    "anonymisee, verdict d'un president. Inférence locale uniquement.",
    )
    parser.add_argument("--question", required=True, help="decision a faire passer au conseil")
    parser.add_argument("--context", action="append", default=[],
                        help="fichier de contexte du projet (repetable)")
    parser.add_argument("--stakes", default="", help="ce qui est en jeu si la decision est mauvaise")
    parser.add_argument("--base-url", default=_env("AGENT_INFERENCE_BASE_URL", "http://localhost:8000/v1"))
    parser.add_argument("--model", default=_env("AGENT_MODEL", "Qwen-3.8-Flash-Next"))
    parser.add_argument("--timeout", type=int, default=int(_env("AGENT_TIMEOUT_SECONDS", "60")))
    parser.add_argument("--temperature", type=float, default=0.7)
    parser.add_argument("--seed", type=int, default=None, help="graine de l'anonymisation")
    parser.add_argument("--max-context-chars", type=int, default=4000)
    parser.add_argument("--out", default=None, help="chemin du transcript Markdown")
    parser.add_argument("--no-transcript", action="store_true", help="ne pas ecrire de transcript")
    parser.add_argument("--json", action="store_true", help="sortie JSON brute (conseillers, revues, verdict)")
    parser.add_argument("--mock", action="store_true",
                        help="valider la pipeline sans inférence (aucun appel reseau)")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    cfg = parse_args(argv)

    # Console Windows (cp1252) : forcer l'UTF-8 pour les accents et fleches.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")

    if len(cfg.question.strip()) < 15:
        print("La question est trop courte pour un conseil : formulez une decision "
              "avec plusieurs options et des enjeux.", file=sys.stderr)
        return 1

    seed = cfg.seed if cfg.seed is not None else random.randrange(10**6)
    context = _read_context(cfg.context, cfg.max_context_chars)
    framed = frame_question(cfg.question, context, cfg.stakes)

    print(f"Conseil LLM - modele {cfg.model} - {len(ADVISORS)} conseillers "
          f"({'mock' if cfg.mock else cfg.base_url})", file=sys.stderr)

    try:
        responses = convene(cfg, framed)
        letters, mapping = anonymize(responses, seed)
        reviews = peer_review(cfg, framed, letters, mapping)
        verdict = chairman(cfg, framed, responses, reviews)
    except RuntimeError as exc:
        print(f"echec du conseil : {exc}", file=sys.stderr)
        return 2

    if cfg.json:
        print(json.dumps({
            "framedQuestion": framed,
            "advisors": {name: answer for (name, _), answer in zip(ADVISORS, responses)},
            "anonymized": mapping,
            "peerReviews": reviews,
            "verdict": verdict,
        }, ensure_ascii=False, indent=2))
    else:
        print(verdict)

    if not cfg.no_transcript:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")
        out = cfg.out or os.path.join("council-transcripts", f"council-{stamp}.md")
        write_transcript(out, framed, responses, reviews, verdict, cfg.model)
        print(f"transcript : {out}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())



