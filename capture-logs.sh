#!/usr/bin/env bash
# Records what IntelliJ's own JUnit runner prints (service messages and test output) for the classes in example/, as
# the plugin receives it. The plugin tests replay these logs from src/test/resources/logs.
#
#   IDEA_HOME=<IntelliJ IDEA installation> ./capture-logs.sh
#
# Needs JAVA_HOME (JDK 21), Maven (mvn on PATH or MVN=...) and the runner jars of the IDE (junit-rt, junit5-rt,
# junit6-rt).
set -euo pipefail
cd "$(dirname "$0")"

IDEA_HOME=${IDEA_HOME:?set IDEA_HOME to an IntelliJ IDEA installation}
JAVA=${JAVA_HOME:+$JAVA_HOME/bin/}java
MVN=${MVN:-mvn}
LOGS=src/test/resources/logs
WORK=build/capture
mkdir -p "$LOGS" "$WORK"

./gradlew -q :comparison-annotations:publishToMavenLocal
"$MVN" -q -f example/pom.xml test-compile dependency:build-classpath -Dmdep.outputFile="$PWD/$WORK/classpath.txt"
SEP=$([[ "$OSTYPE" == msys* || "$OSTYPE" == cygwin* ]] && echo ";" || echo ":")
CP="example/target/test-classes${SEP}$(cat "$WORK/classpath.txt")"
for jar in lib/idea_rt.jar plugins/junit/lib/junit-rt.jar plugins/junit/lib/junit5-rt.jar plugins/junit/lib/junit6-rt.jar; do
    CP="$CP${SEP}$IDEA_HOME/$jar"
done

# one class, as the gutter icon of a class runs it
run() {
    "$JAVA" -cp "$CP" com.intellij.rt.junit.JUnitStarter -ideVersion5 -junit6 "$@" > "$LOGS/$name.txt" 2>&1 || true
}
for class in examples.MulTest examples.MarkedTestTest fixtures.NestedFixture fixtures.NestedParameterizedFixture \
             fixtures.InvocationFailureFixture fixtures.DynamicFixture fixtures.DisabledFixture; do
    name=class-${class#*.}
    run "$class"
done

# several classes, as "Run tests in 'examples'" does: the runner sends the static tree first
printf 'examples\n\n\nexamples.MulTest\nexamples.LifecycleTest\n' > "$WORK/package.txt"
name=package-examples
run "@$WORK/package.txt"

ls -l "$LOGS"
