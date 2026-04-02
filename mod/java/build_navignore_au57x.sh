#!/usr/bin/env bash
# Compile mod/java/src/**/*.java against lsd.jar and emit navignore_audi.jar
# (installjava copies this to NavActiveIgnore.jar on the unit).
#
# Compiler order (see env.sh.inc compile_navignore_sources):
#   1) Host IBM javac if it runs (often broken on modern Linux + J9 execstack)
#   2) IBM javac in i386 Docker (same instruction bytecode as reference; see verify_navignore_structure.sh)
#   3) ECJ — same logic, different pool/switch shape
#
# Bit-identical .class files are not guaranteed: constant pool order and LineNumberTable differ.
# Instruction-level match: ./verify_navignore_structure.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

SRC_DIR="${SRC_DIR:-$MIB_JAVA_ROOT/src}"
BUILD_DIR="${BUILD_DIR:-$MIB_JAVA_ROOT/build}"
CLASSES_DIR="$BUILD_DIR/classes"
OUT_JAR="${OUT_JAR:-$MIB_JAVA_ROOT/navignore_audi.jar}"

if [[ ! -f "$LSD_JAR" ]]; then
	echo "Missing lsd.jar: $LSD_JAR" >&2
	exit 1
fi

mapfile -t JAVA_FILES < <(find "$SRC_DIR" -name '*.java' -type f 2>/dev/null | sort)
if ((${#JAVA_FILES[@]} == 0)); then
	echo "No sources under $SRC_DIR" >&2
	exit 1
fi

rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

echo "Compiling ${#JAVA_FILES[@]} file(s) ..."
# shellcheck disable=SC2068
compile_navignore_sources "$CLASSES_DIR" ${JAVA_FILES[@]}

# Avoid IBM jdk/bin/jar on PATH when J9 is broken on host — prefer system jar.
JAR_CMD=""
for _c in /usr/bin/jar /usr/lib/jvm/default/bin/jar; do
	if [[ -x "$_c" ]]; then
		JAR_CMD="$_c"
		break
	fi
done
[[ -n "$JAR_CMD" ]] || JAR_CMD="$(command -v jar 2>/dev/null || true)"
[[ -n "$JAR_CMD" ]] || JAR_CMD="jar"
if host_ibm_javac_runs 2>/dev/null && [[ -x "${IBM_JDK_DIR:-}/bin/jar" ]]; then
	JAR_CMD="$IBM_JDK_DIR/bin/jar"
fi

mkdir -p "$(dirname "$OUT_JAR")"
rm -f "$OUT_JAR"
OUT_ABS="$(cd "$(dirname "$OUT_JAR")" && pwd)/$(basename "$OUT_JAR")"
(
	cd "$CLASSES_DIR" || exit 1
	"$JAR_CMD" cvf "$OUT_ABS" \
		de/audi/app/terminalmode/smartphone/androidauto2/nav/AndroidAuto2NavHandler.class \
		org/dsi/ifc/carplay/AppState.class
)
echo "Wrote $OUT_ABS ($(wc -c <"$OUT_ABS" | tr -d ' ') bytes)"
echo "SD: mod/java/navignore_audi.jar for installjava -navignoreon"
