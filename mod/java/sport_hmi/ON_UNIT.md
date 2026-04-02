# Sport HMI classpath patch — on-unit verification

## SD card layout

Place the built JAR on the M.I.B. SD card as:

`mod/java/SportHmiMIB.jar`

(same `mod/java/` folder as `navignore_audi.jar` / `navignore_vw.jar`.)

Build on a PC: from repo root, run `mod/java/sport_hmi/build_sport_hmi.sh` (see `TARGETS.txt` for train-matched `lsd.jar`).

## Install / remove

- **Install:** run `installjava` with **`-sporthmion`** (from the toolbox / M.I.B. menu as for other `installjava` options).
- **Remove:** **`-sporthmioff`** deletes `SportHmiMIB.jar`. If the unit fails to boot, edit `/net/mmx/mnt/app/eso/hmi/lsd/lsd.sh` and remove the line that references `SportHmiMIB.jar`, then restore from `lsd.sh.bu` if needed.

## Ordering with NavActiveIgnore

`installjava` adds the Sport JAR **after** `NavActiveIgnore.jar` when both patches are present (AU57x: extra `BOOTCLASSPATH` line; other trains: `-Xbootclasspath/p` line after the Nav line). If you hit class-loader oddities, try installing in a fixed order (e.g. Nav first, then Sport) and retest.

## Logging (wiki-style)

To trace Sport / menu routing, raise log levels for the relevant loggers (exact file depends on your image; often `run_lsd.sh` or the J9 logging properties the wiki names for your train):

- **`App.Car.Sport=5`**
- **`App.Car.MER=5`**

Use together with normal LSD logging when confirming that the Sport entry appears and how MER builds child states.

## Adaptation / FEC / DSI (not replaced by this JAR)

The JAR replaces **`CarFuncAdapImpl`**, **`ViewOptionMapper`** (Sport two-arg visibility mapping), and **`CarDrivingCharacteristicsComponent`** (adds **52** to driving SDIS `CODING` so car-state registration runs for Sport). See `TARGETS.txt`. You may still need:

- **Adaptation** channel **52** / `[LO]_menu_display_SportHMI` in `Car_Function_Adaptations_Gen2` where your dataset uses it.
- **FEC / SWaP** and gateway dataset so BC/DSI supplies torque, oil temperature, boost, etc.; otherwise gauges can stay empty or hidden even when the menu opens.

References:

- [SPORT HMI for Audi (MIB-Helper Wiki)](https://wiki.mib-helper.com/index.php?title=SPORT_HMI_for_Audi)
- [SPORT HMI Performance Monitor — Adaptation](https://wiki.mib-helper.com/index.php?title=SPORT_HMI_Performance_Monitor#Adaptation)

## Train match

Rebuild `SportHmiMIB.jar` against **your** `lsd.jar` (same train as the head unit). Package or obfuscation differences break the override. Use `tools/discover_sport_hmi.py` on that jar if class names move.
