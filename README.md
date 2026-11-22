# SMLS — Stoneworks Mod for Lazy Staff

SMLS is a client-only Fabric mod for Stoneworks staff workflows on Minecraft 26.3. It validates and coordinates staff commands, while the server remains authoritative.

## Install and configure

1. Install Fabric Loader, Fabric API, and the SMLS JAR on the client. Mod Menu is optional.
2. Start Minecraft once to create `config/SMLS.properties`. `play.stoneworks.gg` is allowed by default.
3. Join through `play.stoneworks.gg`. SMLS reads and stores your staff rank from the hub scoreboard; an unrecognized rank locks the module menu.
4. Add alert words, one per line, to `config/SMLS-alerts.txt`, then run `/SMLS alerts reload`.

## Canonical commands ( its recommended that you use the GUI )

The supported command namespace is `/SMLS`:

- `/SMLS` — open the module menu.
- `/SMLS help` — show in-game command help.
- `/SMLS rank` — show the staff rank last detected in the Stoneworks hub.
- `/SMLS dryrun [on|off]` — show or toggle session-only dry-run mode. Direct actions show one exact local preview; multi-command modules show one concise summary. Changing it requires a detected Admin rank.
- `/SMLS cancel` — cancel the active response-tracked operation.
- `/SMLS lands <ign...>`
- `/SMLS members <land>`
- `/SMLS alts <ign>`
- `/SMLS uuid <username...>` — accept up to ten comma/space-separated names.
- `/SMLS xray <ign>` — stage an exact rollback plan.
- `/SMLS xray confirm`
- `/SMLS xray cancel`
- `/SMLS prefix <ign> <limit> <prefixid> <prefixtext>`
- `/SMLS donorpet <ign> <pet>`
- `/SMLS promowave <rank> <ign...>` — stage a promotion batch.
- `/SMLS promowave confirm`
- `/SMLS promowave cancel`
- `/SMLS demowave <rank> <ign...>` — stage a demotion batch.
- `/SMLS demowave confirm`
- `/SMLS demowave cancel`
- `/SMLS activity <ign...>`
- `/SMLS activity cancel`
- `/SMLS event protect ["event name" <land>]` — open Event Protect, or broadcast directly with arguments.
- `/SMLS event randomtp` — vanish when needed and teleport to a random online player.
- `/SMLS event powertool` — open the saved PowerTool library.
- `/SMLS co` — open the CoreProtect request builder.
- `/SMLS containers <ign|*> <time> <radius>`
- `/SMLS containers cancel`
- `/SMLS griefs <ign|*> <time> <radius>`
- `/SMLS griefs cancel`
- `/SMLS punish` — open the rulebook and ban-log workflow.
- `/SMLS alerts [on|off|reload]`
- `/SMLS chatlog [filter]`
- `/SMLS greet <ign>`
- `/SMLS greeter [on|off]` — show or change the persisted automatic-greeter toggle.
- `/SMLS loc [list]`
- `/SMLS loc save <name>`
- `/SMLS loc tp <name>`
- `/SMLS loc del <name>`
- `/SMLS brb [duration|off]` — durations accept forms such as `30s`, `5m`, or `1h30m`.
- `/SMLS dnd [on|off]`
- `/SMLS say [reply]`

## Development

Java 25 is required.

```text
./gradlew test --rerun-tasks
./gradlew build
```

CI validates the Gradle wrapper, builds and tests the project, and uploads test reports on failure. The produced mod is client-only and packages `LICENSE.txt`.
