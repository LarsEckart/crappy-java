#!/usr/bin/env bash
# Runs the published plugin from a fresh Gradle project. It deliberately has no
# pluginManagement block, so Gradle uses its normal Plugin Portal resolution.
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <released-version>" >&2
  exit 2
fi

version=$1
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
project_dir=$(mktemp -d)
trap 'rm -rf "$project_dir"' EXIT

mkdir -p "$project_dir/src/main/java/example" "$project_dir/src/test/java/example"

cat > "$project_dir/settings.gradle.kts" <<'EOF'
rootProject.name = "crappy-java-published-plugin-smoke"
EOF

cat > "$project_dir/build.gradle.kts" <<EOF
plugins {
    java
    jacoco
    id("com.larseckart.crappy-java") version "$version"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

crappyJava {
    threshold = 1000.0
}
EOF

cat > "$project_dir/src/main/java/example/Value.java" <<'EOF'
package example;

public class Value {
    public int absolute(int value) {
        return value < 0 ? -value : value;
    }
}
EOF

cat > "$project_dir/src/test/java/example/ValueTest.java" <<'EOF'
package example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ValueTest {
    @Test
    void calculates_an_absolute_value() {
        assertEquals(3, new Value().absolute(-3));
    }
}
EOF

"$repo_root/gradlew" --project-dir "$project_dir" --no-daemon crap
