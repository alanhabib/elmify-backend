#!/bin/bash

# Script to run the AudibleClone backend with Java 21
# This ensures Maven and the application always use Java 21

export JAVA_HOME="/Users/alanhabib/Library/Java/JavaVirtualMachines/corretto-21.0.3/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java 21:"
java -version
echo ""

# Run the command passed to this script, or default to Maven
if [ $# -eq 0 ]; then
    echo "Running Maven with Java 21..."
    mvn clean spring-boot:run
else
    echo "Running command with Java 21: $@"
    "$@"
fi