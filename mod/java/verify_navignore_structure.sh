#!/usr/bin/env bash
# Verify that compiled .class files are structurally equivalent to a reference JAR:
# for each class, `javap -c -private` bytecode listing must match exactly (pool indices may differ
# in the raw .class file, but instruction sequences are identical when disassembled).
#
# Usage:
#   ./verify_navignore_structure.sh [reference.jar] [classes_dir_or_jar]
# Defaults: REF_NAVIGNORE_JAR or git HEAD jar, then MIB_JAVA_ROOT/build/classes
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

REF="${1:-}"
if [[ -z "$REF" ]]; then
	if [[ -f "$REF_NAVIGNORE_JAR" ]]; then
		REF="$REF_NAVIGNORE_JAR"
	else
		REF="$(mktemp)"
		trap 'rm -f "$REF"' EXIT
		git -C "$MIB_REPO_ROOT" show HEAD:mod/java/navignore_audi.jar >"$REF" 2>/dev/null || {
			echo "No reference jar; set REF_NAVIGNORE_JAR or commit mod/java/navignore_audi.jar" >&2
			exit 1
		}
	fi
fi

ALT="${2:-$MIB_JAVA_ROOT/build/classes}"
if [[ -f "$ALT" ]]; then
	BUILT_CP="$ALT"
else
	BUILT_CP="$ALT"
fi

CLASSES=(
	de.audi.app.terminalmode.smartphone.androidauto2.nav.AndroidAuto2NavHandler
	org.dsi.ifc.carplay.AppState
)
ok=0
for c in "${CLASSES[@]}"; do
	if diff -q <(javap -classpath "$REF" -c -private "$c" 2>/dev/null) <(javap -classpath "$BUILT_CP" -c -private "$c" 2>/dev/null) >/dev/null; then
		echo "OK  javap -c  $c"
		((ok++)) || true
	else
		echo "DIFF javap -c  $c" >&2
		diff -u <(javap -classpath "$REF" -c -private "$c") <(javap -classpath "$BUILT_CP" -c -private "$c") | head -40 >&2 || true
	fi
done
if ((ok == ${#CLASSES[@]})); then
	echo "All ${#CLASSES[@]} classes: instruction-level structure matches reference."
	echo "Note: raw .class bytes may still differ (constant pool order, line tables)."
	exit 0
fi
exit 1
