#!/usr/bin/env bash
# Bytecode-oriented diff of two patch JARs (e.g. your build vs navignore_audi.jar).
# For each class present in BOTH jars, writes unified javap diff under reports/.
#
# Usage:
#   ./compare_patch_jars.sh ./navignore_audi.jar /path/to/reference_navignore_audi.jar
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

A_JAR="${1:-$MIB_JAVA_ROOT/navignore_audi.jar}"
B_JAR="${2:-$REF_NAVIGNORE_JAR}"
REPORT_DIR="${REPORT_DIR:-$MIB_JAVA_ROOT/reports/patch-diff-$(date +%Y%m%d-%H%M%S)}"

if [[ ! -f "$A_JAR" || ! -f "$B_JAR" ]]; then
	echo "Usage: $0 <jar-a> <jar-b>" >&2
	echo "Defaults: navignore_audi.jar vs REF_NAVIGNORE_JAR" >&2
	exit 1
fi

mkdir -p "$REPORT_DIR"
list_classes() {
	jar tf "$1" | awk '/\.class$/ && $0 !~ /\$/ { sub(/\.class$/, ""); gsub(/\//, "."); print }' | sort -u
}

mapfile -t IN_A < <(list_classes "$A_JAR")
mapfile -t IN_B < <(list_classes "$B_JAR")

comm -12 <(printf '%s\n' "${IN_A[@]}" | sort) <(printf '%s\n' "${IN_B[@]}" | sort) >"$REPORT_DIR/common_classes.txt" || true

# bash comm needs sorted files - already sorted
while IFS= read -r c; do
	[[ -z "$c" ]] && continue
	safe=$(echo "$c" | tr '.' '_')
	javap -classpath "$A_JAR" -c -private -verbose "$c" >"$REPORT_DIR/javap_a_${safe}.txt" 2>/dev/null || true
	javap -classpath "$B_JAR" -c -private -verbose "$c" >"$REPORT_DIR/javap_b_${safe}.txt" 2>/dev/null || true
	if diff -u "$REPORT_DIR/javap_b_${safe}.txt" "$REPORT_DIR/javap_a_${safe}.txt" >"$REPORT_DIR/diff_${safe}.txt" 2>/dev/null; then
		echo "MATCH: $c"
	else
		echo "DIFF:  $c -> diff_${safe}.txt"
	fi
done <"$REPORT_DIR/common_classes.txt"

comm -23 <(printf '%s\n' "${IN_A[@]}" | sort) <(printf '%s\n' "${IN_B[@]}" | sort) >"$REPORT_DIR/only_in_a.txt" || true
comm -13 <(printf '%s\n' "${IN_A[@]}" | sort) <(printf '%s\n' "${IN_B[@]}" | sort) >"$REPORT_DIR/only_in_b.txt" || true
echo "Report: $REPORT_DIR"
