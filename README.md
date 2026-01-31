# hlTech
hlTech is a technology mod for Hytale. It for now includes only item transfer cables and a controller for the cables, for chests and other inventories placed in the world.

It uses the Hytale plugin template created by Up and modified by Kaupenjoe.
It also uses a modified version of the code of Hypipes, and LGPL V3 mod created by blake21 (WanMine) accessible on curseforge here :https://www.curseforge.com/hytale/mods/hypipes




## Hytale Plugin Template

A template for Hytale java plugins. Created by [Up](https://github.com/UpcraftLP), and slightly modified by Kaupenjoe. 

### Configuring the Template
If you for example installed the game in a non-standard location, you will need to tell the project about that.
The recommended way is to create a file at `%USERPROFILE%/.gradle/gradle.properties` to set these properties globally.

```properties
# Set a custom game install location
hytale.install_dir=path/to/Hytale

# Speed up the decompilation process significantly, by only including the core hytale packages.
# Recommended if decompiling the game takes a very long time on your PC.
hytale.decompile_partial=true
```
