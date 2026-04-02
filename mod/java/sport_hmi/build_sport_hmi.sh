#!/usr/bin/env bash
# Build SportHmiMIB.jar — IBM javac in Docker (same pattern as compile_navignore_ibm_docker.sh).
# Overrides: CarFuncAdapImpl, ViewOptionMapper, CarDrivingCharacteristicsComponent (+ inner), MenuCtrl.isCoded (MFLG_SPORTHMI menu row).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
MIB_JAVA="$(cd "$ROOT/.." && pwd)"
# shellcheck source=../env.sh.inc
source "$MIB_JAVA/env.sh.inc"

REL_OUT="${SPORT_HMI_BUILD_REL:-build/sport-hmi-classes}"
OUT_DIR="$MIB_JAVA/$REL_OUT"
OUT_JAR="${SPORT_HMI_OUT_JAR:-$ROOT/SportHmiMIB.jar}"
IBM_IMAGE="${MIB_DOCKER_IBM_IMAGE:-i386/debian:bookworm-slim}"

ibm_sdk_zip_usable || exit 1
[[ -f "$LSD_JAR" ]] || {
	echo "Missing lsd.jar: $LSD_JAR" >&2
	exit 1
}

if ! find "$ROOT/src" -name '*.java' -type f | grep -q .; then
	echo "No sources under $ROOT/src" >&2
	exit 1
fi

echo "IBM javac in Docker ($IBM_IMAGE) -> $OUT_DIR"

docker run --rm \
	-e "REL_OUT=$REL_OUT" \
	-e "IBM_JAVAC_EXTRA_FLAGS=${IBM_JAVAC_EXTRA_FLAGS:-}" \
	-e "FIXUID=$(id -u)" \
	-e "FIXGID=$(id -g)" \
	-v "$MIB_VC_ROOT:/vc:ro" \
	-v "$MIB_JAVA:/java" \
	-w /java \
	"$IBM_IMAGE" \
	bash -c 'set -euo pipefail
		export DEBIAN_FRONTEND=noninteractive
		apt-get update -qq
		apt-get install -y -qq unzip >/dev/null
		rm -rf "/java/${REL_OUT}" /tmp/ibmjdk
		mkdir -p "/java/${REL_OUT}" /tmp/ibmjdk
		unzip -q -o /vc/tools/ibm-java-ws-sdk-pxi3260sr4ifx.zip -d /tmp/ibmjdk
		/tmp/ibmjdk/bin/javac -version
		mapfile -t SRCS < <(find /java/sport_hmi/src -name "*.java" -type f | sort)
		/tmp/ibmjdk/bin/javac -source 1.2 -target 1.2 ${IBM_JAVAC_EXTRA_FLAGS:-} -cp ".:/vc/lsd.jar" -d "/java/${REL_OUT}" "${SRCS[@]}"
		chown -R "${FIXUID}:${FIXGID}" "/java/${REL_OUT}"
	'

JAR_CMD=""
for _c in /usr/bin/jar /usr/lib/jvm/default/bin/jar; do
	[[ -x "$_c" ]] && JAR_CMD="$_c" && break
done
[[ -n "$JAR_CMD" ]] || JAR_CMD="$(command -v jar 2>/dev/null || echo jar)"

OUT_ABS="$(cd "$(dirname "$OUT_JAR")" && pwd)/$(basename "$OUT_JAR")"
mkdir -p "$(dirname "$OUT_ABS")"
rm -f "$OUT_ABS"
(
	cd "$OUT_DIR" || exit 1
	"$JAR_CMD" cvf "$OUT_ABS" \
		de/audi/atip/sysapp/CarFuncAdapImpl.class \
		de/audi/app/car/MenuCtrl.class \
		de/audi/app/car/sdis/base/ViewOptionMapper.class \
		de/audi/app/car/sdis/comp/CarDrivingCharacteristicsComponent.class \
		'de/audi/app/car/sdis/comp/CarDrivingCharacteristicsComponent$CarStateHandler.class'
)
echo "Wrote $OUT_ABS ($(wc -c <"$OUT_ABS" | tr -d ' ') bytes)"
SD_JAR="$MIB_JAVA/SportHmiMIB.jar"
cp -f "$OUT_ABS" "$SD_JAR"
echo "SD layout: $SD_JAR (same folder as navignore_*.jar for copy-paste mod/java/)"
