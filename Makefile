API ?= 35
TARGET ?= pa3q-S9380ZHU1AYA1

# 设置CLANG编译器，优先使用环境变量，否则使用TARGET_CC
ifndef CLANG
    CLANG := $(ANDROID_NDK_HOME)/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android$(API)-clang
endif

ROOT := .
SRC_ADB := $(ROOT)/src/adb
SRC_APP := $(ROOT)/src/app
# Keep the historical variable names for callers that override or inspect them.
SRC := $(SRC_ADB)
SRC_ORIGINAL := $(SRC_ADB)
SRC_DEVICE := $(SRC_ADB)
ADB_TARGET_DIR := $(ROOT)/target/adb/$(TARGET)
APP_TARGET_DIR := $(ROOT)/target/app/$(TARGET)
TARGET_DIR := $(ADB_TARGET_DIR)
INCLUDE_DIR := $(ROOT)/include
ADB_TARGET_INCLUDE := $(INCLUDE_DIR)/targets/$(TARGET)/target.h
APP_TARGET_INCLUDE := $(INCLUDE_DIR)/targets/app/$(TARGET)/target.h
TARGET_INCLUDE := $(ADB_TARGET_INCLUDE)
BUILD_DIR := $(ROOT)/build/v6
OBJ_DIR := $(BUILD_DIR)/obj
APP_OBJ_DIR := $(BUILD_DIR)/app-obj
OUT_DIR := $(BUILD_DIR)/artifact

TARGET_FLAGS := --target=aarch64-linux-android$(API)
COMMON_CFLAGS := $(TARGET_FLAGS) -O2 -g0 -Wall -Wextra -Wno-unused-parameter
ADB_CPPFLAGS := -I$(SRC_ADB) -I$(INCLUDE_DIR) -I$(ADB_TARGET_DIR) \
	-DTARGET_CONFIG_H=\"target.h\" -DAPP_PAYLOAD=0
APP_CPPFLAGS := -I$(SRC_APP) -I$(INCLUDE_DIR) -I$(APP_TARGET_DIR) \
	-DTARGET_CONFIG_H=\"target.h\" -DAPP_PAYLOAD=1
ORIGINAL_CPPFLAGS := $(ADB_CPPFLAGS)
DEVICE_CPPFLAGS := $(ADB_CPPFLAGS)

ORIGINAL_OBJECTS := \
	$(OBJ_DIR)/main.o \
	$(OBJ_DIR)/util.o \
	$(OBJ_DIR)/fops.o \
	$(OBJ_DIR)/pipe.o \
	$(OBJ_DIR)/preload_minimal.o \
	$(OBJ_DIR)/root_compat_globals.o

DEVICE_OBJECTS := \
	$(OBJ_DIR)/root-umh.o \
	$(OBJ_DIR)/slide-tracefs.o

APP_OBJECTS := \
	$(APP_OBJ_DIR)/main.o \
	$(APP_OBJ_DIR)/util.o \
	$(APP_OBJ_DIR)/fops.o \
	$(APP_OBJ_DIR)/pipe.o \
	$(APP_OBJ_DIR)/preload_minimal.o \
	$(APP_OBJ_DIR)/root_compat_globals.o \
	$(APP_OBJ_DIR)/root-umh.o \
	$(APP_OBJ_DIR)/slide-app.o

PAYLOAD := $(OUT_DIR)/cve-2026-43499-root-original-zhu-tracefs-v6.so
APP_PAYLOAD := $(OUT_DIR)/cve-2026-43499-app.so
HELPER := $(OUT_DIR)/cve-2026-43499-root

.PHONY: all clean hashes debug

all: $(PAYLOAD) $(APP_PAYLOAD) $(HELPER)

# 调试目标，显示变量值
debug:
	@echo "API = $(API)"
	@echo "TARGET = $(TARGET)"
	@echo "ANDROID_NDK_HOME = $(ANDROID_NDK_HOME)"
	@echo "CLANG = $(CLANG)"
	@echo "TARGET_FLAGS = $(TARGET_FLAGS)"
	@echo "ADB_PAYLOAD = $(PAYLOAD)"
	@echo "APP_PAYLOAD = $(APP_PAYLOAD)"
	@echo "HELPER = $(HELPER)"

$(ADB_TARGET_INCLUDE): $(ADB_TARGET_DIR)/target.h
	mkdir -p $(@D)
	cp $< $@

$(APP_TARGET_INCLUDE): $(APP_TARGET_DIR)/target.h
	mkdir -p $(@D)
	cp $< $@

$(OBJ_DIR) $(APP_OBJ_DIR) $(OUT_DIR):
	mkdir -p $@

$(OBJ_DIR)/%.o: $(SRC_ADB)/%.c $(ADB_TARGET_INCLUDE) | $(OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(ADB_CPPFLAGS) -c $< -o $@

$(OBJ_DIR)/root-umh.o: $(SRC_ADB)/root.c $(ADB_TARGET_INCLUDE) | $(OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(ADB_CPPFLAGS) -c $< -o $@

$(OBJ_DIR)/slide-tracefs.o: $(SRC_ADB)/slide.c $(ADB_TARGET_INCLUDE) | $(OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(ADB_CPPFLAGS) -c $< -o $@

$(PAYLOAD): $(ORIGINAL_OBJECTS) $(DEVICE_OBJECTS) | $(OUT_DIR)
	$(CLANG) $(TARGET_FLAGS) -shared -fuse-ld=lld \
		-Wl,--no-undefined -Wl,-z,relro -Wl,-z,now \
		$(ORIGINAL_OBJECTS) $(DEVICE_OBJECTS) -pthread -ldl -o $@

$(APP_OBJ_DIR)/%.o: $(SRC_APP)/%.c $(APP_TARGET_INCLUDE) | $(APP_OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(APP_CPPFLAGS) -c $< -o $@

$(APP_OBJ_DIR)/root-umh.o: $(SRC_APP)/root.c $(APP_TARGET_INCLUDE) | $(APP_OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(APP_CPPFLAGS) -c $< -o $@

$(APP_OBJ_DIR)/slide-app.o: $(SRC_APP)/slide_app.c $(APP_TARGET_INCLUDE) | $(APP_OBJ_DIR)
	$(CLANG) $(COMMON_CFLAGS) -fPIC $(APP_CPPFLAGS) -c $< -o $@

$(APP_PAYLOAD): $(APP_OBJECTS) | $(OUT_DIR)
	$(CLANG) $(TARGET_FLAGS) -shared -fuse-ld=lld \
		-Wl,--no-undefined -Wl,-z,relro -Wl,-z,now \
		$(APP_OBJECTS) -pthread -ldl -o $@

$(HELPER): $(ROOT)/helper/su_daemon.c | $(OUT_DIR)
	$(CLANG) $(TARGET_FLAGS) -fPIE -pie -O2 -g0 -Wall -Wextra \
		$< -ldl -o $@

hashes: all
	sha256sum $(PAYLOAD) $(APP_PAYLOAD) $(HELPER) \
		$(ADB_TARGET_DIR)/target.h $(APP_TARGET_DIR)/target.h

clean:
	rm -rf $(BUILD_DIR)
