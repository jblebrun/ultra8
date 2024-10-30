#bin/bash

set -o pipefail

# shellcheck disable=SC2016
rm -f seed.db
sqlite3 seed.db "CREATE TABLE CatalogProgram (name TEXT NOT NULL, cyclesPerSecond INTEGER NOT NULL, quirks TEXT NOT NULL, data BLOB, PRIMARY KEY(name));"

for file in chip8/*.ch8
do
    basename=$(basename $file)
    noext=${basename%.*}
    name=$(yq '.name' "chip8/$noext.yaml")
    cyclesPerSecond=$(yq '.cyclesPerSecond' "chip8/$noext.yaml")
    quirks=$(yq '.quirks' "chip8/$noext.yaml")
    echo $basename $noext $name $cyclesPerSecond
    sqlite3 seed.db "INSERT INTO CatalogProgram(name, cyclesPerSecond, quirks, data) VALUES(\"$name\", $cyclesPerSecond, \"$quirks\", readfile(\"$file\"))"
done
