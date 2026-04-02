#!/usr/bin/env bash
# Decompile every class in the reference patch JAR with CFR, using the same flags as
# mib2-android-auto-vc/Makefile extractOriginal / lsd_java (so sources align with lsd.jar).
#
# Output: decompiled/  (gitignored) — copy relevant .java files into src/ for editing.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

PATCH_JAR="${1:-$REF_NAVIGNORE_JAR}"
OUT_DIR="${2:-$MIB_JAVA_ROOT/decompiled}"

if [[ ! -f "$PATCH_JAR" ]]; then
	echo "Patch JAR not found: $PATCH_JAR" >&2
	exit 1
fi
if [[ ! -f "$LSD_JAR" ]]; then
	echo "lsd.jar required on classpath for CFR: $LSD_JAR" >&2
	exit 1
fi
if [[ ! -f "$CFR_JAR" ]]; then
	echo "CFR not found: $CFR_JAR" >&2
	exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Target is the patch jar; stock LSD only for resolution (same idea as Makefile + lsd.jar).
# shellcheck disable=SC2068
java -jar "$CFR_JAR" "$PATCH_JAR" \
	${cfr_decompiler_flags[@]} \
	--extraclasspath "$LSD_JAR" \
	--outputdir "$OUT_DIR"

echo "Decompiled to $OUT_DIR"
echo "Makefile-style cleanups (optional, from mib2-android-auto-vc):"
echo "  find $OUT_DIR -name '*.java' -exec sed -i 's: final : /*final*/ :g' {} +"
echo "  find $OUT_DIR -name '*.java' -exec sed -r -i 's:  @Override: // @Override:g' {} +"
