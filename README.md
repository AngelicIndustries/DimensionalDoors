# Dimensional Doors: Adventura Arcana Edition

A 1.7.10 fork of [Dimensional Doors](https://github.com/DimensionalDevelopment/DimDoors) for the Adventura Arcana modpack, with content from the mod's later versions.

![logo](images/logo_small.png)

[![hub](images/badges/github.png)](https://github.com/AngelicIndustries/DimensionalDoors/releases)
[![67](images/badges/67.png)](https://67minecraft.com/mod/dimensional-doors-adventura-arcana-edition)
[![maven](images/badges/maven.png)](https://maven.angelic.industries/#/releases/org/dimdev/dimdoors/DimensionalDoors)
![forge](images/badges/forge.png)
[![cord](images/badges/cord.png)](https://discord.gg/xAWCqGrguG)

<!-- Not published yet
[![curse](images/badges/curse.png)](https://www.curseforge.com/minecraft/mc-mods/<slug>)
[![modrinth](images/badges/modrinth.png)](https://modrinth.com/mod/<slug>)
[![mcmodcn](images/badges/mcmodcn.png)](https://www.mcmod.cn/class/<id>.html)
-->

## Features

* Doors that lead into pocket dimensions: empty pockets, dungeons and a personal pocket of your own.
* Rifts, gateways to dungeons out in the world, and Limbo for those who die in a pocket.

## Changes from the original

* Pockets, dungeons and personal pockets share three dimensions instead of one dimension each, ported from the 1.12 rewrite.
* Open rifts grow and eat the land around them until a Rift Stabilizer stops them, drawn as black fractal cracks.
* Gateways are built in the style of the land they stand on, from 6.x's structures, and also appear in Nether fortresses.
* 68 dungeon rooms from 6.x, next to the original's own.
* Limbo is darker, decays blocks into Solid Static, and its exit lands you on the surface.
* Limbo materials, the Transcendent and Trepidation enchantments, three music discs, and dimensional versions of other mods' doors.

## Dependencies
* [UniMixins](https://modrinth.com/mod/unimixins) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/unimixins) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/unimixins/versions) [![git](images/icons/git.png)](https://github.com/LegacyModdingMC/UniMixins/releases)
* Optional: [Et Futurum Requiem](https://modrinth.com/mod/etfuturum) [![curse](images/icons/curse.png)](https://www.curseforge.com/minecraft/mc-mods/et-futurum-requiem) [![modrinth](images/icons/modrinth.png)](https://modrinth.com/mod/etfuturum/versions) [![git](images/icons/git.png)](https://github.com/Roadhog360/Et-Futurum-Requiem/releases). Without it, the 6.x rooms that need its blocks are left out.

## For other mods

When Dimensional Doors is about to turn a player's death into a trip to Limbo, it fires the cancellable
`org.dimdev.dimdoors.api.event.LimboDeathEvent` on `MinecraftForge.EVENT_BUS`, before it touches the inventory.
Cancel it to postpone Limbo, for example to offer a resurrection: the death then happens as an ordinary one, and the
player goes to Limbo when they respawn. A respawn of such a player fires the cancellable `LimboRespawnEvent`; cancel
it to keep them out of Limbo, for example because the respawn is a resurrection.

Another mod can add its own door by extending `org.dimdev.dimdoors.blocks.BaseDimDoor`. Any such door is immune to
rift regeneration. Through `IDimDoor.placeExitDoor` and `IDimDoor.placeExitPlatformBlock` it can build its own kind of
door and platform at the far end of a safe exit, and with `IDimDoor.getPocketDoor` it can give a pocket built through it
a different door inside. `org.dimdev.dimdoors.api.DimDoorsLinks` tells what link starts at a block and closes a link
without leaving a rift. It also does what the Rift Remover, Rift Stabilizer and Rift Signature do: close a rift,
stabilize one and open a linked pair. `DimDoorsClient.highlightRiftCores` shows nearby rift cores on the client. Depend
on the `dev` jar with `compileOnly`.

## Building

`./gradlew build`.

## Credits
* StevenRS11, SenseiKiwi and Dimensional Development for Dimensional Doors, its 1.12 rewrite and 6.x.
* Music discs by Stevenrs11, Firel and Lachney; Limbo and Monolith sounds from Freesound. The `CREDITS.txt` files next to the assets list each source.
* [GT:NH buildscript](https://github.com/GTNewHorizons/ExampleMod1.7.10).

## License

This project is a fork of [Dimensional Doors](https://github.com/DimensionalDevelopment/DimDoors/tree/1.7.10) by Dimensional Development.

- **Original 1.7.10 code and the parts ported from the 1.12 rewrite**: [MIT License](LICENSE-MIT) (Copyright 2018 Dimensional Development)
- **Rooms, gateways, textures and discs from 6.x**: GPL-3.0 (Dimensional Development)
- **New contributions**: [GPL-3.0](LICENSE) (Copyright 2026 jack)

The combined work is distributed under GPL-3.0 terms. The original MIT-licensed portions remain available under MIT terms.
