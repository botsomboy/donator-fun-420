# Donator - Fun 420

A small cosmetic RuneLite plugin.

## Features

- **4:20 alarm** — at 16:20 and 04:20 local time a pulsing coloured border and a
  message appear over the game screen for 60 seconds. If you log in halfway
  through that minute, you still get the full 60 seconds.
- **April 20 banner** — on April 20 a green banner reading "Happy 420 today"
  sits at the top of the screen. Click the × to dismiss it for the rest of the
  day; it comes back next year.

Colours and texts are configurable, and both features can be switched off
independently. The Test section lets you trigger the alarm and preview the
banner on any day.

The plugin only draws on screen. It does not read account data, make network
requests, or interact with the game in any way.

## Building

    ./gradlew build
    ./gradlew run
