#!/usr/bin/env sh
set -eu

# Build the runnable jar (if needed) and start the game, forwarding any args.
# Examples:
#   scripts/run.sh --bots 3 --games 5 --quiet
#   scripts/run.sh --human --bots 2 --games 1
mvn -q -B -DskipTests package
exec java -jar target/uno.jar "$@"
