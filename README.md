# HitmanGame Companion Plugin (`com.ronlab.hitmengame`)

`HitmanGame` is an official companion plugin for **Ronlab Game Assistant (RGA)** (`com.ronlab:rga-api:1.13.0-SNAPSHOT`), built following the **Micro-Companion Architecture (CPMK)** standard. Inspired by classic Minecraft Hitman minigames, it seamlessly integrates with RGA's core lifecycle, session management, and Just-In-Time (JIT) spectator controls.

---

## Technical & Architectural Baseline

- **Java Baseline**: Java 25 (Bytecode target `--release 25`)
- **Server Platform**: PaperMC 26.2 (or compatible 1.21+ Paper server)
- **Primary Framework Engine**: `RonlabGameAssistant` (`rga-core` / `rga-api`)
- **API Version**: `26.2`

---

## CPMK 5-Pillar Architectural Alignment

`HitmanGame` strictly adheres to the 5 core CPMK pillars established in `rga-core`:

1. **Core Gameplay Function Retention:**
   - Preserves 100% of native Hitman minigame mechanics: Speedrunner survival vs Hitman tracking.
   - Preserves active compass tracking loops, role allocation routines, and team handling without altering core minigame rules.

2. **Ronlab Integration Standard:**
   - Listens strictly for CPMK event payloads over the `rga-api` event bus: `MinigameStartEvent` and `MinigameConcludeEvent`.
   - Descriptor files (`paper-plugin.yml` / `plugin.yml`) specify `api-version: '26.2'` and declare `rga-core` under `dependencies.server.RonlabGameAssistant` (`required: true`, `join-classpath: true`) with NO invalid `load: BEFORE` directives.

3. **Baseline Structure & Rules Provision:**
   - Scoreboard Formatting: Uses PaperMC's `objective.numberFormat(NumberFormat.blank())` across sidebar lines to suppress default margin numbers.
   - Chunk-Loading Safety: Ensures scoreboard assignment (`player.setScoreboard()`) occurs strictly during post-teleport spawn phases to prevent chunk-loading hangs.
   - Teardown Routine: Calls `player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard())` and unregisters sidebar objectives upon receiving `MinigameConcludeEvent`.

4. **Companion-Type Agnostic Design:**
   - Built as a self-contained module that operates independently of internal `rga-core` implementation details, communicating solely via public `rga-api` contracts.

5. **Feature Implementation & Modification Specs:**
   - Fully documents administrative commands, permission nodes, configuration schemas (`config.yml` / `arena.yml`), and **Solo QA Developer Mode**.

---

## Solo QA Developer Mode (`initialPlayerCount == 1`)

`HitmanGame` includes built-in support for **Solo QA Developer Mode**:

- **Activation Trigger**: Detected when `initialPlayerCount == 1` at session start (`MinigameStartEvent`).
- **Testing Behavior**: Win/loss condition checking freezes automatically. Instead of concluding the game session immediately upon death or single-player initialization, the game loop remains active.
- **Developer Benefits**: Allows a single developer to continuously test world resets, compass tracking mechanics, spawn vectors, and arena boundaries without triggering session teardown.

---

## Administrative Commands & Permission Nodes

### Permission Nodes
- `hitman.admin`: Full administrative access to all Hitman companion commands and configurations.
- `hitman.command.reload`: Allows reloading `config.yml` and `arena.yml` configuration files.
- `hitman.command.status`: Allows inspecting active Hitman sessions and role assignments.

### Commands
- `/hitman reload`: Reloads plugin configurations (`config.yml` and `arena.yml`).
- `/hitman status`: Displays active session count, registered worlds, and role assignments.

---

## Default Configurations

The plugin includes two primary configuration schemas in `src/main/resources`:

- **`config.yml`**: Controls compass tracking update intervals, debug logging, permission mappings, CPMK scoreboard optimization flags, and Solo QA Mode overrides.
- **`arena.yml`**: Defines spawn vectors (`speedrunner_spawns`, `hitman_spawns`), time limits (`time_limit_seconds`), fall thresholds (`fall_threshold_y`), and world border parameters.

---

## Building & Installation

### Build from Source
Compile using Maven:

```bash
mvn clean package
```

The compiled plugin jar will be generated at `target/hitmengame-1.0-SNAPSHOT.jar`.

### Installation
1. Place `RonlabGameAssistant.jar` in your Paper 26.2 server's `plugins/` folder.
2. Place `hitmengame-1.0-SNAPSHOT.jar` in your Paper 26.2 server's `plugins/` folder.
3. Start or restart your server.
