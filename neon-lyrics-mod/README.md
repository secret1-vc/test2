# Neon Lyrics - Fabric 1.21.11 (client only)

Karaoke bar at the top, floating words, animated sky effects, and a turntable
settings screen. Needs Fabric Loader 0.19+ and Fabric API for 1.21.11.

## Get the .jar
**Easiest (no setup): GitHub Actions**
1. Create a new GitHub repo and upload everything in this folder (keep the .github folder).
2. Open the repo's Actions tab -> "Build jar" -> wait ~3 minutes.
3. Open the finished run, download the `neon-lyrics-jar` artifact, unzip it. The .jar inside goes in your `mods` folder.

**Locally:** install JDK 21, run `./gradlew build` (Windows: `gradlew.bat build`), jar is in `build/libs/`.

## Use
- `/lyrics search <artist song>`  fetches synced lyrics from lrclib.net and starts them
- `/lyrics gui`                   opens the turntable settings screen
- `/lyrics auto`                  Windows: follows whatever Spotify/browser is playing
- `/lyrics play <file>`           plays a .lrc from `.minecraft/config/neonlyrics/`
- `/lyrics pause | resume | stop | offset <ms>`
