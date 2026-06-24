#!/usr/bin/env sh
set -eu

# Run the JUnit 5 test suite through Maven. Equivalent to: mvn test
exec mvn -B test
