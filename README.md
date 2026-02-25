# OreMiner

OreMiner is a desktop mining game built with Java Swing.

## Project Layout

- `Menu/main.py`: launcher UI for selecting a release build
- `Beta Snapshot/`: latest snapshot gameplay build
- `Full Release/`: stable gameplay build
- `Release Canidate/`: pre-release gameplay build

## Requirements

- JDK 17 or newer
- Python 3.10 or newer

## Quick Start

1. Open a terminal in this project root.
2. Run `python run.py`.
3. Choose a release in the launcher.

## Manual Start

1. `cd "Beta Snapshot"` or another release folder.
2. `javac -cp ".;json-simple-1.1.1.jar;gson-2.10.1.jar" oreminer.java`
3. `java -cp ".;json-simple-1.1.1.jar;gson-2.10.1.jar" oreminer`

## Notes

- The folder name is currently `Release Canidate` and kept as-is for compatibility.
- Save files are created next to each release build.