# HitmanGame Companion Plugin (`com.ronlab.hitmengame`)

`HitmanGame` is an official companion plugin for **Ronlab Game Assistant (RGA)** (`com.ronlab:rga-api:1.13.0-SNAPSHOT`). Inspired by Minecraft Hitman minigames, it integrates directly with RGA's core lifecycle, session management, and JIT spectator controls.

## Features

- **Lifecycle Integration**: Intercepts `MinigameStartEvent` for minigame sessions (`hitman` / `hitmengame`) to allocate speedrunner and hitman roles.
- **Dynamic Tracking Compass**: Periodically updates active hitmen compasses to target the nearest non-spectating speedrunner (`RGASessionControl.isSpectator`).
- **JIT Spectator Delegation**: Delegates inventory clearing, snapshot management, and game mode changes to RGA Core via `RGASessionControl.setSpectator`.
- **Session Conclusion**: Automatically evaluates win/loss conditions and requests session conclusion and world teardown via RGA API (`requestSessionConclude`).

## Prerequisites

- **Java**: Java 25 (Bytecode target)
- **Server**: Paper 26.2 (or compatible 1.21+ Paper server)
- **Core Dependency**: Ronlab Game Assistant (`RonlabGameAssistant.jar` v1.13.0+) loaded before companion plugins (`load: BEFORE`).

## Quick Setup & How-To

### 1. Build the Plugin

Compile the jar using Maven:

```bash
mvn clean package
```

The compiled plugin jar will be generated at `target/hitmengame-1.0-SNAPSHOT.jar`.

### 2. Server Installation

1. Copy `RonlabGameAssistant.jar` to your Paper server's `plugins/` directory.
2. Copy `hitmengame-1.0-SNAPSHOT.jar` to your Paper server's `plugins/` directory.
3. Start or restart your Paper server.

### 3. Usage & Game Flow

1. Players form a party and select the Hitman minigame using RGA hub interface or commands.
2. When RGA provisions the minigame world, `HitmanGame` receives the `MinigameStartEvent` and assigns 1 player as Speedrunner and remaining players as Hitmen.
3. Hitmen receive live compass updates pointing to the active speedrunner.
4. When a speedrunner or hitman is eliminated, `HitmanGame` calls `RGASessionControl.setSpectator(victim, true)`.
5. Once all speedrunners (or hitmen) are eliminated, `HitmanGame` triggers `requestSessionConclude` to allow RGA Core to gracefully restore player inventories and clean up the session world.


