#!/usr/bin/env zsh

./gradlew sortDependencies
./gradlew updateLintBaseline
./gradlew lint
