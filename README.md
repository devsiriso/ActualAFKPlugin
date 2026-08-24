# Actual AFK RuneLite plugin

Actual AFK is a passive RuneLite plugin that tracks AFK time, streaks, and AFK XP.

## test locally

From this directory, run:

```bash
./gradlew test
```

This compiles the plugin and runs its unit tests. The first run downloads Gradle and the RuneLite dependencies.

## run RuneLite in developer mode

From this directory, run:

```bash
./gradlew runHotSwap
```

When RuneLite starts, enable **Actual AFK** in the plugin configuration. Select its clock toolbar button to open the RuneLite-owned side panel. Right-click the progress card and select **Add to canvas** to show the persistent, movable RuneLite overlay; use **Remove from canvas** to hide it again. Its placement is managed by RuneLite's standard overlay controls. Progress is stored in RuneLite and does not affect your OSRS account.

The `runHotSwap` task loads the plugin in developer mode and listens for a debugger on port `5005`. It does not install or publish anything to the Plugin Hub. The project also defines `./gradlew run`, but repository development uses `runHotSwap`; use `run` only when explicitly requested.

## use standard HotSwap

If RuneLite is not already running, start it with a debugger socket:

```bash
./gradlew runHotSwap
```

Then attach IntelliJ:

1. Open **Run → Edit Configurations**.
2. Add **Remote JVM Debug**.
3. Set host to `localhost` and port to `5005`.
4. Start that debug configuration.
5. After changing a method body, select **Build → Build Project**. IntelliJ reloads the changed class into the running client.
6. Disable and re-enable **Actual AFK** when its startup state or panel values need refreshing.

Standard HotSwap handles method-body changes. Restart RuneLite after changing fields, method signatures, annotations, resources, dependencies, or class structure.

The project is distributed under the BSD 2-Clause License in the repository root.
