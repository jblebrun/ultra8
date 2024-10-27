#bin/bash

set -o pipefail

# shellcheck disable=SC2016
rm -f seed.db
sqlite3 seed.db "CREATE TABLE CatalogProgram (name TEXT NOT NULL, data BLOB, PRIMARY KEY(name));"

for file in chip8/*
do
    name=$(basename "$file")
    echo $name
    sqlite3 seed.db "INSERT INTO CatalogProgram(name, data) VALUES(\"$name\", readfile(\"$file\"))"
done
