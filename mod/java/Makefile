# NavActiveIgnore rebuild helpers (see mib2-android-auto-vc for lsd.jar / IBM JDK).
MIB_JAVA := $(abspath .)

.PHONY: help compare decompile build compare-patches verify-structure ibm-docker

help:
	@echo "Targets:"
	@echo "  compare          - REF patch vs stock lsd.jar (javap + sha256)"
	@echo "  compare-patches  - navignore_audi.jar vs REF_NAVIGNORE_JAR"
	@echo "  decompile        - CFR reference jar -> decompiled/"
	@echo "  build            - javac src/ -> navignore_audi.jar (host IBM, else Docker i386, else ECJ)"
	@echo "  ibm-docker       - only IBM javac inside Docker"
	@echo "  verify-structure - javap -c vs reference (instruction match)"
	@echo "Set LSD_JAR, REF_NAVIGNORE_JAR, MIB_VC_ROOT as needed."

compare:
	@$(MIB_JAVA)/compare_navignore_bytecode.sh

compare-patches:
	@$(MIB_JAVA)/compare_patch_jars.sh

decompile:
	@$(MIB_JAVA)/decompile_navignore_reference.sh

build:
	@$(MIB_JAVA)/build_navignore_au57x.sh

ibm-docker:
	@$(MIB_JAVA)/compile_navignore_ibm_docker.sh

verify-structure:
	@$(MIB_JAVA)/verify_navignore_structure.sh
