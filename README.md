# Actual AFK

Actual AFK is a passive RuneLite plugin that tracks time spent inactive while logged in.

This plugin is still in a beta phase, I'm collecting information and feedback about the tracking and persistence before potentialy expanding on it further. If there is interest I want to build out highscores for this skill, just for fun.

The plugin records:

- Current and longest inactive streaks
- Session and total inactive time
- Session and total active and inactive percentages
- Client-local AFK XP and levels
- Session AFK XP per hour

A game tick counts as inactive only when the local player is logged in, is not moving, is not animating, is not interacting, and has no recently observed activity. The minimum number of inactive ticks and the activity grace period can be configured.

Progress is shown in a RuneLite side panel. An optional movable RuneLite canvas overlay can show the current level, AFK XP, activity state, and inactive streak.

Individual statistics can be hidden from the side panel. Tracking and the canvas overlay can also be enabled or disabled in the plugin configuration.

Progress is stored locally in the active RuneLite profile. It is not OSRS experience, does not affect the player's Jagex account, and is not an official skill.

All tracking is observational. The plugin does not perform game actions or send data to external services.

The project is distributed under the BSD 2-Clause License.
