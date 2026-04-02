#!/usr/bin/env bash
# Compare classes in a reference NavActiveIgnore patch JAR (e.g. navignore_audi.jar)
# against the same logical classes inside stock lsd.jar — javap listings + raw .class digests.
#
# Prerequisites:
#   - lsd.jar from mib2-android-auto-vc (see ../mib2-android-auto-vc/sync_lsd_from_backup.sh)
#   - navignore_audi.jar copied to mod/java/ or set REF_NAVIGNORE_JAR
#
# Usage:
#   ./compare_navignore_bytecode.sh
#   REF_NAVIGNORE_JAR=/path/to/patch.jar LSD_JAR=/path/to/lsd.jar ./compare_navignore_bytecode.sh
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

PATCH_JAR="${1:-$REF_NAVIGNORE_JAR}"
STOCK_JAR="${2:-$LSD_JAR}"
REPORT_DIR="${REPORT_DIR:-$MIB_JAVA_ROOT/reports/$(date +%Y%m%d-%H%M%S)}"

if [[ ! -f "$PATCH_JAR" ]]; then
	echo "Patch JAR not found: $PATCH_JAR" >&2
	echo "Copy navignore_audi.jar to mod/java/ or pass path as arg1." >&2
	exit 1
fi
if [[ ! -f "$STOCK_JAR" ]]; then
	echo "Stock lsd.jar not found: $STOCK_JAR" >&2
	echo "Run mib2-android-auto-vc/sync_lsd_from_backup.sh or make -C mib2-android-auto-vc lsd.jar" >&2
	exit 1
fi

command -v javap >/dev/null || {
	echo "javap not in PATH; install a JDK or use IBM jdk bin after extract." >&2
	exit 1
}

mkdir -p "$REPORT_DIR"
echo "Report: $REPORT_DIR"
echo "PATCH_JAR=$PATCH_JAR"
echo "STOCK_JAR=$STOCK_JAR"

list_classes() {
	jar tf "$1" | awk '/\.class$/ && $0 !~ /\$/ { sub(/\.class$/, ""); gsub(/\//, "."); print }' | sort -u
}

mapfile -t CLASSES < <(list_classes "$PATCH_JAR")
if ((${#CLASSES[@]} == 0)); then
	echo "No .class entries in $PATCH_JAR" >&2
	exit 1
fi

printf '%s\n' "${CLASSES[@]}" >"$REPORT_DIR/patch_class_list.txt"
echo "Classes in patch jar (${#CLASSES[@]}):"
cat "$REPORT_DIR/patch_class_list.txt"

digest_class_from_jar() {
	local jar_path=$1
	local internal_path=$2
	unzip -p "$jar_path" "$internal_path" 2>/dev/null | sha256sum | awk '{print $1}'
}

for c in "${CLASSES[@]}"; do
	internal="${c//.//}.class"
	p_digest=$(digest_class_from_jar "$PATCH_JAR" "$internal")
	echo "$p_digest  PATCH $c" >>"$REPORT_DIR/class_sha256.txt"

	stock_digest=""
	if unzip -l "$STOCK_JAR" "$internal" >/dev/null 2>&1; then
		stock_digest=$(digest_class_from_jar "$STOCK_JAR" "$internal")
		echo "$stock_digest  STOCK $c" >>"$REPORT_DIR/class_sha256.txt"
	else
		echo "(no matching path in lsd.jar)  STOCK $c" >>"$REPORT_DIR/class_sha256.txt"
	fi

	safe=$(echo "$c" | tr '.' '_')
	javap -classpath "$PATCH_JAR" -c -private -verbose "$c" >"$REPORT_DIR/javap_patch_${safe}.txt" 2>/dev/null || true
	if [[ -n "$stock_digest" ]]; then
		javap -classpath "$STOCK_JAR" -c -private -verbose "$c" >"$REPORT_DIR/javap_stock_${safe}.txt" 2>/dev/null || true
		if diff -u "$REPORT_DIR/javap_stock_${safe}.txt" "$REPORT_DIR/javap_patch_${safe}.txt" >"$REPORT_DIR/diff_javap_${safe}.txt" 2>/dev/null; then
			echo "MATCH javap: $c (unexpected if patch changes behavior)"
		else
			echo "DIFF javap: $c -> see diff_javap_${safe}.txt"
		fi
	else
		echo "NEW (not in lsd.jar): $c — patch-only or different package layout"
	fi
done

echo ""
echo "Done. Summary digests: $REPORT_DIR/class_sha256.txt"
echo "Next: ./decompile_navignore_reference.sh then edit src/ and ./build_navignore_au57x.sh"
