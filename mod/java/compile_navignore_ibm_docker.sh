#!/usr/bin/env bash
# Run IBM javac from tools/ibm-java-ws-sdk-pxi3260sr4ifx.zip inside linux/386 (i386 Debian).
# Host 64-bit kernels often block old J9 (executable stack); Docker i386 avoids that.
#
# Usage: compile_navignore_ibm_docker.sh [relative_classes_dir_under_mod_java]
#   Default: build/ibm-docker-classes
# Env: MIB_DOCKER_IBM_IMAGE, IBM_JAVAC_EXTRA_FLAGS
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=env.sh.inc
source "$HERE/env.sh.inc"

REL_OUT="${1:-build/ibm-docker-classes}"
OUT_DIR="$MIB_JAVA_ROOT/$REL_OUT"
IBM_IMAGE="${MIB_DOCKER_IBM_IMAGE:-i386/debian:bookworm-slim}"

ibm_sdk_zip_usable || exit 1
if [[ ! -f "$LSD_JAR" ]]; then
	echo "Missing lsd.jar: $LSD_JAR" >&2
	exit 1
fi

if ! find "$MIB_JAVA_ROOT/src" -name '*.java' -type f | grep -q .; then
	echo "No sources under $MIB_JAVA_ROOT/src" >&2
	exit 1
fi

echo "IBM javac in Docker ($IBM_IMAGE) -> $OUT_DIR"

docker run --rm \
	-e "REL_OUT=$REL_OUT" \
	-e "IBM_JAVAC_EXTRA_FLAGS=${IBM_JAVAC_EXTRA_FLAGS:-}" \
	-e "FIXUID=$(id -u)" \
	-e "FIXGID=$(id -g)" \
	-v "$MIB_VC_ROOT:/vc:ro" \
	-v "$MIB_JAVA_ROOT:/java" \
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
		mapfile -t SRCS < <(find /java/src -name "*.java" -type f | sort)
		/tmp/ibmjdk/bin/javac -source 1.2 -target 1.2 ${IBM_JAVAC_EXTRA_FLAGS:-} -cp ".:/vc/lsd.jar" -d "/java/${REL_OUT}" "${SRCS[@]}"
		chown -R "${FIXUID}:${FIXGID}" "/java/${REL_OUT}"
	'

echo "Compiled into $OUT_DIR"
