#bin/bash

set -o pipefail

# shellcheck disable=SC2016
sqlite3 seed.db "CREATE TABLE Program (name TEXT NOT NULL, builtIn INTEGER NOT NULL, data BLOB, PRIMARY KEY(name));"
for file in chip8/*
do
    name=$(basename "$file")
    echo $name
    sqlite3 seed.db "INSERT INTO Program(name, builtIn, data) VALUES(\"$name\", true, readfile(\"$file\"))"
done
