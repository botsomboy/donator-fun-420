# Donator - Fun 420

A small cosmetic RuneLite plugin.

## Features

- **4:20 alarm** — at 16:20 and 04:20 local time a pulsing coloured border and a
  message appear over the game screen for 60 seconds. If you log in halfway
  through that minute, you still get the full 60 seconds.
- **April 20 banner** — on April 20 a full width green banner reading "Happy 420
  today" sits at the top of the screen. Click the × to dismiss it for the rest of
  the day; it comes back next year.
- **April 20 login message** — logging in on April 20 also puts that text large
  in the middle of the screen for five seconds, after which it fades out. It
  returns on every login, including after a world hop.

Colours and texts are configurable, and every feature can be switched off on its
own; the banner and the login message share their colour and text. The Test
section lets you trigger the alarm and preview both on any day.

The plugin only draws on screen. It does not read account data, make network
requests, or interact with the game in any way.

## Building

    ./gradlew build
    ./gradlew run
