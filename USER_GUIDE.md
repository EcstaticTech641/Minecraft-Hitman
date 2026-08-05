# HitmanGame Companion Plugin - User & Administrator Guide

Welcome to the comprehensive User and Administrator Guide for **HitmanGame** (`com.ronlab.hitmengame`), a Micro-Companion Architecture (CPMK) plugin designed for **Ronlab Game Assistant (RGA)**.

---

## 1. Architectural Baseline

- **Target Runtime**: Java 25 (Bytecode `--release 25`)
- **Server Framework**: PaperMC 26.2 (or compatible Paper 1.21+ release)
- **Core Dependency**: `RonlabGameAssistant` (`rga-api:1.13.0-SNAPSHOT` / `rga-core`)
- **API Spec**: `api-version: '26.2'`

---

## 2. Minigame Mechanics & Game Flow

### 2.1 Role Allocation
Upon session initialization triggered by RGA Core (`MinigameStartEvent`):
- **Speedrunner**: Designated player (1st player in session roster). Objective: Defeat the Ender Dragon or survive until all Hitmen are eliminated.
- **Hitman / Hitmen**: Remaining players in the session. Objective: Track down and eliminate the Speedrunner(s).

### 2.2 Dynamic Compass Tracking
- Hitmen are provided with a live tracking compass in their inventory.
- Every 20 ticks (1.0 second), `CompassTrackerTask` updates each active Hitman's compass target to point to the exact coordinates of the nearest active, non-spectating Speedrunner in the same world.

### 2.3 JIT Spectator Delegation
- Upon death (`PlayerDeathEvent`), `HitmanGame` delegates inventory snapshots, game mode transitions, and spectator state management to RGA Core via `RGASessionControl.setSpectator(player, true)`.
- Eliminates manual inventory or game state management within the companion plugin.

### 2.4 Session Conclusion
- When all Speedrunners or all Hitmen are eliminated, `HitmanGame` evaluates the session outcome and triggers graceful teardown by calling `requestSessionConclude` on the RGA API.

---

## 3. CPMK Event Bus Integration

`HitmanGame` communicates with `rga-core` exclusively through the `rga-api` event bus:

```
                  +-----------------------+
                  |  RonlabGameAssistant  |
                  +-----------+-----------+
                              |
        MinigameStartEvent    |    MinigameConcludeEvent
               +--------------+--------------+
               |                             |
               v                             v
   +-----------------------+     +-----------------------+
   |   HitmanListener      |     |   HitmanListener      |
   | (onMinigameStart)     |     | (onMinigameConclude)   |
   +-----------+-----------+     +-----------+-----------+
               |                             |
               v                             v
    Initialize Session Roles        Teardown Session &
    & Register Active Map           Restore Main Scoreboard
```

- **`MinigameStartEvent`**:
  - Filters by minigame ID (`hitman` / `hitmengame`).
  - Reads player UUID roster and constructs `HitmanGameSession`.
  - Enforces thread-safe role assignment maps.

- **`MinigameConcludeEvent`**:
  - Removes active session from plugin state.
  - Clears compass tasks and scoreboards.
  - Reverts player scoreboards to main server scoreboard.

---

## 4. Scoreboard Management Specifications

To maintain PaperMC 26.2 compliance and prevent chunk-loading hangs:

1. **Margin Number Suppression**:
   - Local sidebar scoreboards apply `objective.numberFormat(NumberFormat.blank())` across all lines to remove unwanted score numbers on the right margin.

2. **Post-Teleport Assignment**:
   - `player.setScoreboard()` is called strictly during post-teleport spawn phases (after world chunk loading completes) to prevent client/server sync hangs.

3. **Teardown Restoration**:
   - On `MinigameConcludeEvent`, player scoreboards are reset via `player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard())` and sidebar objectives are unregistered.

---

## 5. Solo QA Developer Mode (`initialPlayerCount == 1`)

### 5.1 Purpose & Behavior
Solo QA Developer Mode is designed for single-developer testing without requiring multiple clients or test accounts.

- **Trigger**: Activated automatically whenever `initialPlayerCount == 1` during `MinigameStartEvent`.
- **Win Condition Freeze**: The session win/loss check (`checkAndConcludeIfFinished`) is bypassed. The session will **not** trigger `requestSessionConclude` when players die or when only 1 role exists.
- **Continuous Mechanics Testing**:
  - Tracking compass updates remain fully active.
  - Pedestal/spawn vectors and fall thresholds operate normally.
  - Map reset routines can be repeatedly executed without destroying the test session.

---

## 6. Administrative Commands & Permission Nodes

### Permission Nodes

| Permission Node | Description | Default Access |
|---|---|---|
| `hitman.admin` | Full administrative control over Hitman companion features | OP |
| `hitman.command.reload` | Permission to reload `config.yml` and `arena.yml` | OP |
| `hitman.command.status` | Permission to view active sessions and role debug details | OP |

### Commands

| Command | Usage | Description | Permission |
|---|---|---|---|
| `/hitman reload` | `/hitman reload` | Reloads plugin configurations from disk | `hitman.command.reload` |
| `/hitman status` | `/hitman status` | Displays active sessions, world names, and role count | `hitman.command.status` |

---

## 7. Configuration Reference

### 7.1 `config.yml`

```yaml
plugin:
  compass_update_interval_ticks: 20  # Compass update rate in ticks (20 ticks = 1 sec)
  debug_logging: false               # Toggle verbose console logging

permissions:
  admin: "hitman.admin"
  reload: "hitman.command.reload"
  status: "hitman.command.status"

scoreboard:
  margin_number_suppression: true     # Applies PaperMC NumberFormat.blank()
  post_teleport_assignment: true      # Defers scoreboard assignment to post-teleport
  teardown_restoration: true          # Restores main scoreboard on conclude

solo_qa_mode:
  enabled: true                       # Enables Solo QA Mode when initialPlayerCount == 1
  freeze_win_conditions_on_single_player: true
```

### 7.2 `arena.yml`

```yaml
arena:
  id: "hitman_default"
  name: "Standard Hitman Arena"
  time_limit_seconds: 1800            # Game duration limit (seconds)
  fall_threshold_y: -64.0             # Y-level void fall protection/elimination threshold

  speedrunner_spawns:
    - { x: 0.5, y: 64.0, z: 0.5, yaw: 0.0, pitch: 0.0 }

  hitman_spawns:
    - { x: 10.5, y: 64.0, z: 10.5, yaw: 180.0, pitch: 0.0 }
    - { x: -10.5, y: 64.0, z: -10.5, yaw: 90.0, pitch: 0.0 }

  world_border:
    enabled: true
    center_x: 0.0
    center_z: 0.0
    initial_diameter: 500.0
    shrink_time_seconds: 600
    warning_distance: 10
```
