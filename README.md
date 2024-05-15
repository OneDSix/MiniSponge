# MiniSponge Loader

MiniSponge Loader is a near universal mod loader based off Minicraft+'s mod loader, [MiniMods](https://github.com/AnvilloyDevStudio/MiniMods).\
This project was originally built for only 1D6, but I though others might get some use out of it as well.

If you're interested about Minicraft+, go to the [Minicraft GitHub repository](https://github.com/MinicraftPlus/minicraft-plus-revived),\
I highly recommend the game, even if development is slow these days. Drop a PR or two if you can!

## Setting up MSL

### Adding MSL to a project

#### Pre-Compile

TODO

tldr;
- import MSL using jitpack+gradle
- set your project's entrypoint to MSL's entrypoint
- add an MSL config with the project's entrypoint class
- build the project
- assuming all goes well, it should work immediately

#### Post-Compile

TODO

tldr;
- unzip the jar
- add the MSL config with the project's entrypoint class, along with MSL's class files
- change the entrypoint in `META-INF/MANIFEST.mf` to MSL's entrypoint
- assuming all goes well, it should work immediately

### MSL Config

TODO

tldr;
```json
{
  "entrypoint": "org.example.Entrypoint"
}
```

## Writing mods for MSL

Quite a few things have changed between MML and MSL,
everything from the `mod.json` file, originally based on Fabric Loader's `*.mod.json`,
being turned into a `mods.toml` file based on NeoForge Loader's `*.mods.toml` instead.

TODO

## Building

As there are several batch files in this project, it is recommended to use Windows.

To build this project, the most recommended way is to use the [local batch file](build.bat). Use `.\build build` for Windows.

Since there are some potential problem when executing general `.\gradlew build`. Please execute `.\gradlew :build` instead.

## License

This repository is licensed with LGPL 2.1 and GPL 3,\
you should be able to find the corresponding license documents in the same directory of this source code.\
The overall license is named [`LICENSE` in the project's root directory](/LICENSE).

`SPDX-License-Identifier: LGPL-2.1-only AND GPL-3-only`
