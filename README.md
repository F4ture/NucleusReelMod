# Nucleus Reel

A client-side Fabric mod that turns **Hypixel SkyBlock Crystal Nucleus** rewards into a mining-themed three-reel animation.

**Minecraft 26.1.2 · Fabric · English interface · Public Edition 0.10.3**

## Features

- Three animated reels with gradual stops, suspenseful near misses and item-colored result highlights.
- Special animations and sounds for Divan's Alloy, Quick Claw and Jaderald.
- An interactive bonus cave with three tunnels, the player's skin, mining effects and ambient sounds.
- SkyBlock item appearances from the active resource pack, even when the items are not in the player's inventory.
- Custom reel music using local OGG Vorbis or PCM WAV tracks.
- Configurable size, duration, sounds and effects.

The animations are cosmetic. They display actual server rewards and do not create items or alter Hypixel drop chances. The public edition has no commands for previewing or forcing scenes.

Animation chances use fixed defaults: 40% for a second-reel miss, 50% for a third-reel miss after the second matches, and 50% for each eligible bonus scenario. Bonus scenes are enabled. Chance overrides and bonus-disable settings from older configuration files are ignored; music, size, duration and other ordinary settings are preserved.

## Installation

1. Install **Minecraft 26.1.2**, **Java 25**, **Fabric Loader 0.19.3 or newer**, and **Fabric API 0.155.2+26.1.2 or newer for Minecraft 26.1.2**.
2. Download `nucleus-reel-0.10.3+26.1.2-public.jar` from [Releases](https://github.com/F4ture/NucleusReelMod/releases).
3. Remove older Nucleus Reel JARs from your profile's `mods` folder, add the new JAR, and restart Minecraft.
4. Use `/nr help` for settings. Reels start automatically after a supported Crystal Nucleus loot bundle on Hypixel SkyBlock.

Existing settings in `config/nucleus-reel.json` and custom music files are preserved when updating. Keep only one Nucleus Reel version installed.

## Commands

`/nucleusreel` is the full alias for `/nr`.

| Command | Purpose |
| --- | --- |
| `/nr help` | Show available commands |
| `/nr status` | Show mod status |
| `/nr on`, `/nr off`, `/nr toggle` | Enable or disable the mod |
| `/nr skip` | Close the current animation and release deferred reward messages |
| `/nr reload` | Reload saved settings |
| `/nr duration <1–15>` | Base reel duration in seconds |
| `/nr scale <0.5–1.5>` | Reel size |
| `/nr sound on\|off` | Animation sounds |
| `/nr effects on\|off` | Optional visual effects |
| `/nr coins on\|off` | Coin sounds |
| `/nr coinvolume <0–1>` | Coin volume |
| `/nr hidechat on\|off` | Defer reward messages until the reveal |
| `/nr music folder`, `/nr music list` | Open the music folder or list tracks |
| `/nr music track <filename>` | Select an OGG Vorbis or PCM WAV track |
| `/nr music on\|off`, `/nr music volume <0–1>` | Music playback and volume |

Filenames may contain spaces. Minecraft's Music volume must be above zero for custom reel music.

Boolean forms such as `sound true` remain available. Chance-changing commands, their legacy aliases and the bonus toggle are not available in the public edition.

## Resource packs

Items use the active resource pack and cached SkyBlock item data. Packs such as FurfSky Reborn require their own compatible item-texture dependencies. This project does not bundle FurfSky or Minecraft textures. When a custom item model is unavailable, the renderer uses the available fallback appearance.

## Build from source

Use **JDK 25**. The included wrapper downloads the required Gradle version.

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```sh
./gradlew build
```

Output: `build/libs/nucleus-reel-0.10.3+26.1.2-public.jar`.

The build runs reward parsing, item appearance, audio, reel outcome, animation timing and command checks. Checks cover rejection of preview, chance-changing and bonus-toggle commands under both aliases and in Tab suggestions, plus migration of old settings to fixed public-edition rules. Test runners are not included in the mod JAR.

## Credits and license

- Reel concept inspired by [Scatha-Pro](https://github.com/NamelessJu/Scatha-Pro). Its code and textures are not included.
- Item appearance data comes from the [Hypixel item resource](https://api.hypixel.net/v2/resources/skyblock/items).
- Resource-pack compatibility was checked against [Catharsis](https://github.com/meowdding/catharsis) and [FurfSky Reborn](https://github.com/furfsky/reborn).
- Build setup is based on the [Fabric example mod](https://github.com/FabricMC/fabric-example-mod/tree/26.1.2). The Gradle wrapper retains its upstream license notices.

Nucleus Reel is licensed under [MIT](LICENSE).
