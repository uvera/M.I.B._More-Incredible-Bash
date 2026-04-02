#!/bin/sh
# List static byte[] CODING (or similar) init in de/audi/app/car/sdis/comp/* from an lsd.jar.
# Use YOUR train's jar: ./scan_sdis_coding_arrays.sh /path/to/lsd.jar
# If CAR_FUNCTION_SPORT_HMI (byte 52) never appears in any component's registerCarStates list,
# CarFuncAdapImpl-only patches may not drive Sport visibility through those handlers.

set -e
JAR="${1:?usage: $0 /path/to/lsd.jar}"
JAVAP=/usr/bin/javap
[ -x "$JAVAP" ] || JAVAP=javap

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

unzip -q -d "$TMP" "$JAR" 'de/audi/app/car/sdis/comp/*.class' 2>/dev/null || true

for c in "$TMP"/de/audi/app/car/sdis/comp/*.class; do
	[ -f "$c" ] || continue
	rel=${c#"$TMP"/}
	cls=${rel%.class}
	cls=${cls//\//.}
	# Pull static initializer and show bipush/byte array stores (heuristic)
	out=$("$JAVAP" -classpath "$TMP" -c "$cls" 2>/dev/null | sed -n '/static {/,/^}/p' || true)
	if echo "$out" | grep -q newarray; then
		echo "=== $cls ==="
		echo "$out" | grep -E 'bipush|iconst_|bastore|newarray' || true
		echo ""
	fi
done
