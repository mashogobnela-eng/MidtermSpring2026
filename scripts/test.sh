#!/usr/bin/env sh
set -eu

rm -rf out
mkdir -p out
javac -d out src/*.java tests/*.java
java -cp out CharacterizationTest
