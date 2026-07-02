# WC-W Spot-Check Record - weightByMaster Retrofit (PD-D)

Date: 2026-07-02. Author: backend-engineer (data). Task: WC-W (`docs/full-review/plan.md`).

## Method

Weights for the 43 existing task families were authored provisionally from
`docs/full-review/masters-coverage.md` section 3 (the nine per-master wiki tables, fetched
2026-07-01 via a summarizer - flagged for spot-check by the audit itself, section 4.3). Per the
PD-D disposition, a sample of task/master weight pairs was re-verified against the LIVE wiki
master pages (WebFetch, 2026-07-02) before authoring. Rule: on any discrepancy the wiki wins and
the discrepancy is recorded here. The verified pairs are pinned hard in
`TaskWeightRetrofitTest.spotCheckedWeightsMatchTheLiveWiki`; all other pairs stay soft
(present + positive) per PD-D's "no hard assertions on fragile absolute numbers".

## Checked pairs (19 pairs, 4 masters - all MATCH section 3, zero discrepancies)

| # | Master (live page fetched) | Task | Live wiki weight | Section 3 | Outcome |
|---|---|---|---|---|---|
| 1 | Duradel | Metal dragons | 14 | 14 | MATCH |
| 2 | Duradel | Abyssal demons | 12 | 12 | MATCH |
| 3 | Duradel | Boss tasks | 12 | 12 | MATCH |
| 4 | Duradel | Hellhounds | 10 | 10 | MATCH |
| 5 | Duradel | Mutated zygomites | 2 | 2 | MATCH |
| 6 | Duradel | Araxytes | 10 | 10 | MATCH |
| 7 | Konar quo Maten | Metal dragons | 15 | 15 | MATCH |
| 8 | Konar quo Maten | Wyrms | 10 | 10 | MATCH |
| 9 | Konar quo Maten | Waterfiends | 2 | 2 | MATCH |
| 10 | Konar quo Maten | Kurask | 3 | 3 | MATCH |
| 11 | Konar quo Maten | Mutated zygomites | 2 | 2 | MATCH |
| 12 | Krystilia | Greater demons | 8 | 8 | MATCH |
| 13 | Krystilia | Black dragons | 4 | 4 | MATCH |
| 14 | Krystilia | Spiritual creatures | 6 | 6 | MATCH |
| 15 | Krystilia | Ankou | 6 | 6 | MATCH |
| 16 | Vannaka | Gargoyles | 5 | 5 | MATCH |
| 17 | Vannaka | Gryphons | 10 | 10 | MATCH |
| 18 | Vannaka | Nechryael | 5 | 5 | MATCH |
| 19 | Vannaka | Kalphite | 7 | 7 | MATCH |

Corroborating signal: the Duradel fetch also reported total task weight 327, matching section
3.9's header exactly.

URLs fetched: https://oldschool.runescape.wiki/w/Duradel ·
https://oldschool.runescape.wiki/w/Konar_quo_Maten ·
https://oldschool.runescape.wiki/w/Krystilia · https://oldschool.runescape.wiki/w/Vannaka

## Honest UNKNOWN (weights deliberately NOT authored)

- **araxytes x turael** and **araxytes x spria**: the Turael and Spria section-3 fetches BOTH
  omitted the Araxytes row (the audit's verified summarizer failure mode, section 3 caveat /
  4.3). The Araxytes page confirms the assignment relationship (masterIds keeps both) but gives
  no per-master weight, so `araxytes.json` carries NO weight entry for either. Pinned absent by
  `TaskWeightRetrofitTest.documentedUnknownsStayHonestlyAbsent`. Authoring them requires a fresh
  verification against the live Turael/Spria pages first.

Every other (task, master) pair across the 43 files had a section-3 weight; 184 weight entries
authored total, none fabricated.
