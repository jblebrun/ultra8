#!/usr/bin/env python3
import os
import sqlite3
import shutil

db_path = "seed.db"
if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute("""
CREATE TABLE CatalogProgram (
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    cyclesPerSecond INTEGER NOT NULL,
    quirks TEXT NOT NULL,
    category TEXT NOT NULL,
    data BLOB,
    PRIMARY KEY(name)
);
""")

chip8_dir = "chip8"
for filename in sorted(os.listdir(chip8_dir)):
    if not filename.endswith(".ch8"):
        continue
    noext = os.path.splitext(filename)[0]
    ch8_path = os.path.join(chip8_dir, filename)
    yaml_path = os.path.join(chip8_dir, noext + ".yaml")

    name = noext.replace("_", " ").title()
    description = ""
    cyclesPerSecond = 15
    quirks = ""
    category = "octo"

    if os.path.exists(yaml_path):
        with open(yaml_path, "r", encoding="utf-8") as yf:
            lines = yf.readlines()

        is_multiline_desc = False
        desc_lines = []

        for line in lines:
            line_str = line.rstrip("\n")
            if is_multiline_desc:
                if line.startswith("  ") or line == "":
                    desc_lines.append(line_str.strip())
                    continue
                else:
                    is_multiline_desc = False
                    description = "\n".join(desc_lines).strip()

            if ":" in line_str:
                parts = line_str.split(":", 1)
                key = parts[0].strip()
                val = parts[1].strip()
                if key == "name":
                    name = val.strip("\"'\x27")
                elif key == "description":
                    if val == "|":
                        is_multiline_desc = True
                        desc_lines = []
                    else:
                        description = val.strip("\"'\x27")
                elif key == "cyclesPerSecond":
                    try:
                        cyclesPerSecond = int(val)
                    except:
                        pass
                elif key == "quirks":
                    quirks = val.strip("\"'\x27")
                elif key == "category":
                    category = val.strip("\"'\x27")

        if is_multiline_desc and desc_lines:
            description = "\n".join(desc_lines).strip()

    with open(ch8_path, "rb") as bf:
        data_blob = bf.read()

    print(f"Processed: {filename} -> {name} ({category})")
    cursor.execute(
        "INSERT OR REPLACE INTO CatalogProgram (name, description, cyclesPerSecond, quirks, category, data) VALUES (?, ?, ?, ?, ?, ?)",
        (name, description, cyclesPerSecond, quirks, category, data_blob)
    )

conn.commit()
conn.close()

os.makedirs("../app/src/main/assets", exist_ok=True)
shutil.copy("seed.db", "../app/src/main/assets/seed.db")
print("Successfully generated seed.db and copied to app/src/main/assets/seed.db!")
