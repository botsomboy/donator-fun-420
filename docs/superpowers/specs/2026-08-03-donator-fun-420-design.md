# Donator - Fun 420 — RuneLite plugin (design)

Datum: 2026-08-03
Status: ontwerp goedgekeurd, implementatie nog niet gestart

## Doel

Een RuneLite-plugin, publiceerbaar via de officiële Plugin Hub, met twee
cosmetische features:

1. Een visueel alarm om 16:20 en 04:20 lokale tijd, zolang je ingelogd bent.
2. Een groene banner bovenin de client op 20 april met de tekst
   "Happy 420 today".

De plugin leest geen accountgegevens, doet geen netwerkverkeer, automatiseert
niets in het spel en verandert niets aan de spelbeleving buiten het tekenen van
een overlay. Daarmee valt hij binnen Jagex' third-party-richtlijnen en de
review-eisen van de Plugin Hub.

## Gedrag

### Alarm

**Venster.** De minuut `16:20:00`–`16:20:59` en de minuut `04:20:00`–`04:20:59`
op de lokale systeemklok van de speler. Lokale tijd, dus zomer-/wintertijd
volgt automatisch mee.

**Start.** Het alarm start op één van twee momenten:

- de client is ingelogd en de klok gaat het venster binnen, of
- de client wordt ingelogd terwijl de klok al in het venster staat.

**Duur.** 60 seconden vanaf het startmoment. Inloggen om `16:20:40` levert dus
een alarm tot `16:21:40` — de duur hangt aan de trigger, niet aan de
minuutgrens. De duur is vast en niet instelbaar.

**Uitloggen.** Overlays renderen alleen in-game, dus uitloggen laat het alarm
verdwijnen. Log je binnen hetzelfde venster opnieuw in, dan start een nieuw
alarm van 60 seconden (regel 2 hierboven). Buiten het venster gebeurt er niets;
een onderbroken alarm wordt niet hervat.

**Weergave.** Een pulserende rand rondom het spelscherm plus grote tekst in het
midden. De puls is een rustige cyclus van ongeveer één seconde — nadrukkelijk
geen stroboscoop. Kleur en tekst zijn instelbaar; standaard groen met
"4:20 — blaze it".

### Banner

Op 20 april, van `00:00` tot `23:59` lokale tijd, staat er een groene balk
bovenaan het spelscherm met "Happy 420 today" en een ×-knop rechts.

**Wegklikken.** Een klik op de × verbergt de banner en slaat de datum van
vandaag op. De banner blijft die kalenderdag weg, ook na een herstart van de
client. Op 20 april van het volgende jaar verschijnt hij weer.

**Samenloop.** Op 20 april om 16:20 zijn banner en alarm allebei zichtbaar. Ze
delen geen state en beïnvloeden elkaar niet. Beide zijn los aan/uit te zetten.

## Architectuur

Vier onderdelen, met een eenrichtings-afhankelijkheid:
plugin → klok → state → overlay. Overlays kennen de state en de config; niets
kent de overlays. Daardoor is alle tijdlogica te testen zonder te renderen.

### `Fun420Clock` (pure logica)

Geen RuneLite-afhankelijkheden. Beantwoordt twee vragen:

- valt een gegeven tijdstip binnen een alarm-venster?
- is een gegeven datum 20 april?

Krijgt zijn tijd via een injecteerbare `java.time.Clock`. Tests injecteren een
vaste klok; de simulatie-schakelaar in de config injecteert een verschoven
klok. Beide gebruiken dus exact hetzelfde pad als productie.

### `AlarmState` (pure logica)

Houdt bij wanneer het alarm startte. Levert: is het nu actief, en hoe ver staat
de puls (een waarde tussen 0 en 1 die de overlay naar een alpha vertaalt).
Ook zonder client testbaar.

### `BannerState` (pure logica)

Beslist of de banner zichtbaar hoort te zijn, gegeven de datum van vandaag en
de eventueel opgeslagen datum waarop de banner is weggeklikt. Neemt zelf geen
klikken aan en slaat niets op — dat doet de plugin — maar de beslissing zelf is
pure logica en dus testbaar zonder client.

### `AlarmOverlay` en `BannerOverlay` (weergave)

Tekenen uitsluitend. Ze bevatten geen tijdlogica en nemen geen beslissingen
over wanneer iets zichtbaar is; ze lezen de state en de config. `BannerOverlay`
levert daarnaast de rechthoek van zijn ×-knop, zodat de plugin een klik erop
kan herkennen.

### `Fun420Plugin` en `Fun420Config` (lijmlaag)

`Fun420Plugin` registreert de overlays en een muis-listener bij het opstarten
en ruimt ze bij het afsluiten weer op. Eén keer per seconde raadpleegt hij de
klok en werkt de alarm-state bij. Klikken op de ×-rechthoek van de banner
worden hier opgevangen en de datum wordt hier opgeslagen; of de banner daarna
zichtbaar is, bepaalt `BannerState`.

`Fun420Config` beschrijft de instellingen (zie hieronder).

## Instellingen en state

Config-groep: `donatorfun420`. Drie secties:

**Alarm** — inschakelen (standaard aan), kleur (standaard groen), tekst
(standaard "4:20 — blaze it").

**Banner** — inschakelen (standaard aan), kleur (standaard groen), tekst
(standaard "Happy 420 today").

**Testen** — twee schakelaars, zichtbaar in de gepubliceerde versie:

- *Alarm nu*: start het alarm direct en zet zichzelf daarna terug op uit.
- *Simuleer 20 april*: zolang deze aan staat doet de klok alsof het 20 april
  is, zodat de banner te beoordelen is op een willekeurige dag.

**Verborgen state** — de datum waarop de banner is weggeklikt, als ISO-tekst
(bijvoorbeeld `2027-04-20`), opgeslagen via de RuneLite-configopslag. Bij het
opstarten wordt die gelezen; is de waarde leeg of onleesbaar, dan wordt hij
genegeerd en verschijnt de banner gewoon. Dat is het enige foutscenario in de
plugin: er is geen netwerk, geen bestandsopslag en geen externe dependency.

## Testen

**Unit-tests** (`./gradlew test`), geschreven vóór de implementatie:

- `16:19:59` valt buiten het venster, `16:20:00` erbinnen, `16:21:00` erbuiten
- `04:20:xx` gedraagt zich identiek aan `16:20:xx`
- start op `16:20:40` blijft actief tot `16:21:40` en stopt daarna
- de puls-waarde blijft binnen 0–1 over de volle 60 seconden
- 20 april wordt herkend om `00:00:00` en om `23:59:59`, 19 en 21 april niet
- schrikkeljaren veranderen niets aan de datumherkenning
- een weggeklikte banner blijft weg op dezelfde datum en verschijnt weer op de
  volgende 20 april

**Dev-client** (`./gradlew run`): een echte RuneLite met de plugin geladen, voor
het visuele oordeel over rand, puls, tekst en banner. Inloggen gebeurt met een
Jagex-account; dat vereist de eenmalige procedure uit de RuneLite-wiki om de
sessie van de Jagex Launcher aan de dev-client door te geven. Die stappen horen
in het implementatieplan.

**Handmatige eindcheck**: alarm via de testschakelaar, banner via de
simulatie-schakelaar, × wegklikken, client herstarten en bevestigen dat de
banner weg blijft.

## Bouwen en publiceren

**Lokaal.** JDK 11 (Eclipse Temurin) via `winget`; die staat nog niet op de
machine. De Gradle-wrapper komt uit de RuneLite-template mee, dus verder is er
niets te installeren. Werkmap: `C:\Users\2.0\projects\donator-fun-420`.

**Repo.** Public repo `botsomboy/donator-fun-420`, gegenereerd uit
`runelite/example-plugin`. Bevat een BSD-2-Clause-licentie, een README, een
optioneel `icon.png` (maximaal 48×72) en `runelite-plugin.properties` met
displayName, author, description, tags en de hoofdklasse. Build-type
`standard`, want er zijn geen externe dependencies — dat scheelt aanzienlijk in
de reviewtijd.

**Plugin Hub.** Fork van `runelite/plugin-hub`, een bestand in `plugins/` met
`repository=` en `commit=`, en een pull request. Bij latere wijzigingen wordt
alleen de commit-hash bijgewerkt.

**Accountdiscipline.** Alle git- en `gh`-acties voor dit project lopen onder het
account `botsomboy`. Vóór commits en PR's controleren met `gh auth status`.

## Risico's en aanvaarde keuzes

- **Naam.** "Donator - Fun 420" kan bij reviewers een vraag oproepen, omdat
  "donator" in OSRS-context naar private servers en betaalde perks verwijst
  terwijl de plugin daar niets mee doet. Bewust behouden op verzoek van de
  gebruiker; een naamswijziging is later mogelijk maar vereist een nieuwe PR.
- **Reviewtijd.** De Plugin Hub-review is mensenwerk en kan weken duren. Dat
  staat los van de werking van de plugin, die lokaal via de dev-client al
  volledig te gebruiken is.
- **Test-schakelaars in de release.** Bewuste keuze: ze blijven zichtbaar voor
  eindgebruikers. Ze veranderen alleen wat de plugin tekent.

## Buiten scope

- Geluid bij het alarm (alleen visueel).
- Alarm voor andere spelers dan jezelf, of vriendenlijst-integratie.
- Instelbare alarmtijden of een instelbare alarmduur.
- Een keuze tussen lokale tijd en Jagex-tijd (UTC).
- Publicatie of hosting op de cryptobot-VPS; die blijft buiten dit project.
