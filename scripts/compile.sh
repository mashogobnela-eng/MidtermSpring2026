#!/usr/bin/env sh
set -eu

# Compile through Maven (Assignment 4). Equivalent to: mvn -q -B compile
exec mvn -q -B compile
