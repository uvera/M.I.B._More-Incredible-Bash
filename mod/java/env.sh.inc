# shellcheck shell=bash
# Shared paths for NavActiveIgnore rebuild workflow.
# Source from repo root:  . mod/java/env.sh.inc
# Or:  source "$(dirname "$0")/env.sh.inc"

_MIB_JAVA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
MIB_REPO_ROOT="$(cd "$_MIB_JAVA_ROOT/../.." && pwd)"
MIB_VC_ROOT="${MIB_VC_ROOT:-$MIB_REPO_ROOT/mib2-android-auto-vc}"

export MIB_JAVA_ROOT="$_MIB_JAVA_ROOT"
export MIB_REPO_ROOT
export MIB_VC_ROOT

export LSD_JAR="${LSD_JAR:-$MIB_VC_ROOT/lsd.jar}"
export REF_NAVIGNORE_JAR="${REF_NAVIGNORE_JAR:-$MIB_JAVA_ROOT/navignore_audi.jar}"
export CFR_JAR="${CFR_JAR:-$MIB_VC_ROOT/tools/cfr-0.152.jar}"
# Same archive as mib2-android-auto-vc/Makefile target `jdk` (unzip tools/... -> jdk/)
export IBM_JDK_ZIP="${IBM_JDK_ZIP:-$MIB_VC_ROOT/tools/ibm-java-ws-sdk-pxi3260sr4ifx.zip}"
export IBM_JDK_DIR="${IBM_JDK_DIR:-$MIB_VC_ROOT/jdk}"

# Flags after the target jar/class (matches mib2-android-auto-vc/Makefile CFT_OPTS suffix)
cfr_decompiler_flags=(--previewfeatures false --switchexpression false --removeinnerclasssynthetics false)

ibm_sdk_zip_usable() {
	if [[ ! -f "$IBM_JDK_ZIP" ]]; then
		echo "IBM JDK zip not found: $IBM_JDK_ZIP" >&2
		return 1
	fi
	if head -1 "$IBM_JDK_ZIP" 2>/dev/null | grep -q 'git-lfs\.github\.com'; then
		echo "IBM JDK path is a Git LFS pointer, not the real zip: $IBM_JDK_ZIP" >&2
		echo "Run: (cd \"$MIB_VC_ROOT\" && git lfs pull --include tools/ibm-java-ws-sdk-pxi3260sr4ifx.zip)" >&2
		return 1
	fi
	local sz
	sz=$(wc -c <"$IBM_JDK_ZIP" | tr -d ' ')
	if [[ "$sz" -lt 1048576 ]]; then
		echo "IBM JDK zip too small (${sz} bytes); expected full SDK (~76MB): $IBM_JDK_ZIP" >&2
		return 1
	fi
	if ! unzip -tqq "$IBM_JDK_ZIP" >/dev/null 2>&1; then
		echo "IBM JDK zip is not a valid archive: $IBM_JDK_ZIP" >&2
		return 1
	fi
	return 0
}

ensure_ibm_jdk() {
	if [[ -x "$IBM_JDK_DIR/bin/javac" ]]; then
		export PATH="$IBM_JDK_DIR/bin:$PATH"
		return 0
	fi
	local found=""
	if [[ -d "$IBM_JDK_DIR" ]]; then
		found="$(find "$IBM_JDK_DIR" -path '*/bin/javac' -type f 2>/dev/null | head -1 || true)"
	fi
	if [[ -n "$found" && -x "$found" ]]; then
		IBM_JDK_DIR="$(cd "$(dirname "$found")/.." && pwd)"
		export PATH="$IBM_JDK_DIR/bin:$PATH"
		return 0
	fi
	ibm_sdk_zip_usable || return 1
	echo "Extracting IBM JDK (same layout as: make -C \"$MIB_VC_ROOT\" jdk) -> $IBM_JDK_DIR"
	mkdir -p "$IBM_JDK_DIR"
	if ! unzip -q -o "$IBM_JDK_ZIP" -d "$IBM_JDK_DIR"; then
		echo "unzip failed: $IBM_JDK_ZIP" >&2
		return 1
	fi
	if [[ -x "$IBM_JDK_DIR/bin/javac" ]]; then
		export PATH="$IBM_JDK_DIR/bin:$PATH"
		return 0
	fi
	found="$(find "$IBM_JDK_DIR" -path '*/bin/javac' -type f 2>/dev/null | head -1 || true)"
	if [[ -n "$found" && -x "$found" ]]; then
		IBM_JDK_DIR="$(cd "$(dirname "$found")/.." && pwd)"
		export PATH="$IBM_JDK_DIR/bin:$PATH"
		return 0
	fi
	echo "javac not found under $IBM_JDK_DIR after unzip" >&2
	return 1
}

javac_mib2() {
	ensure_ibm_jdk || return 1
	local classes_dir="$1"
	shift
	mkdir -p "$classes_dir"
	"$IBM_JDK_DIR/bin/javac" -source 1.2 -target 1.2 -cp ".:$LSD_JAR" -d "$classes_dir" "$@"
}

# Eclipse JDT (Java 1.2 class files, major 46). Bytecode layout differs from IBM javac but is runtime-equivalent.
ECJ_JAR="${ECJ_JAR:-$MIB_JAVA_ROOT/tools/ecj-3.19.0.jar}"
ECJ_DOWNLOAD_URL="${ECJ_DOWNLOAD_URL:-https://repo1.maven.org/maven2/org/eclipse/jdt/org.eclipse.jdt.core/3.19.0/org.eclipse.jdt.core-3.19.0.jar}"

ensure_ecj() {
	if [[ -f "$ECJ_JAR" ]]; then
		return 0
	fi
	mkdir -p "$(dirname "$ECJ_JAR")"
	echo "Downloading ECJ -> $ECJ_JAR" >&2
	curl -fsSL -o "$ECJ_JAR" "$ECJ_DOWNLOAD_URL"
}

host_ibm_javac_runs() {
	if [[ ! -x "${IBM_JDK_DIR:-}/bin/javac" ]] && ! ensure_ibm_jdk 2>/dev/null; then
		return 1
	fi
	"$IBM_JDK_DIR/bin/javac" -version >/dev/null 2>&1
}

compile_navignore_sources() {
	local classes_dir="$1"
	shift
	local -a sources=("$@")
	mkdir -p "$classes_dir"
	# 1) Host IBM javac (only if it actually starts — often fails on hardened 64-bit with J9 execstack)
	if ibm_sdk_zip_usable >/dev/null 2>&1 && host_ibm_javac_runs; then
		"$IBM_JDK_DIR/bin/javac" -source 1.2 -target 1.2 -cp ".:$LSD_JAR" -d "$classes_dir" "${sources[@]}"
		return 0
	fi
	# 2) Same IBM SDK inside i386 Docker (matches reference bytecode structure; see verify_navignore_structure.sh)
	if [[ "${MIB_NAVIGNORE_SKIP_DOCKER:-}" != 1 ]] && command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1 && ibm_sdk_zip_usable 2>/dev/null; then
		local rel="${classes_dir#$MIB_JAVA_ROOT/}"
		if [[ "$rel" == "$classes_dir" ]]; then
			echo "compile_navignore_sources: classes_dir must be under MIB_JAVA_ROOT ($MIB_JAVA_ROOT)" >&2
			return 1
		fi
		"$MIB_JAVA_ROOT/compile_navignore_ibm_docker.sh" "$rel"
		return 0
	fi
	# 3) ECJ fallback (same logic, different constant pools / switches)
	ensure_ecj || return 1
	java -cp "$ECJ_JAR" org.eclipse.jdt.internal.compiler.batch.Main \
		-source 1.3 -target 1.2 -cp "$LSD_JAR" -d "$classes_dir" "${sources[@]}"
}
