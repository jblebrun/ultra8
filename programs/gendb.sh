#bin/bash

set -o pipefail

# shellcheck disable=SC2016
sqlite3 seed.db "CREATE TABLE Program (name TEXT NOT NULL, builtIn INTEGER NOT NULL, cyclesPerTick INTEGER NOT NULL, data BLOB, PRIMARY KEY(name));"
sqlite3 seed.db "CREATE TABLE Chip8ProgramState (name TEXT NOT NULL, halt TEXT, vRegisters BLOB NOT NULL, hpRegisters BLOB NOT NULL, stack BLOB NOT NULL, mem BLOB NOT NULL, i INTEGER NOT NULL, sp INTEGER NOT NULL, pc INTEGER NOT NULL, hires INTEGER NOT NULL, targetPlane INTEGER NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, plane1Data BLOB NOT NULL, plane2Data BLOB NOT NULL, PRIMARY KEY(name));"

for file in chip8/*
do
    name=$(basename "$file")
    echo $name
    sqlite3 seed.db "INSERT INTO Program(name, builtIn, cyclesPerTick, data) VALUES(\"$name\", true, 10, readfile(\"$file\"))"
done
