#!/bin/bash
javac -d . $(find . -name "*.java")
java -cp . main.MainMenu
