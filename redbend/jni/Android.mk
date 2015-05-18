# PLATFORM TOOLCHAIN and BUILD variables are taken through the environment from
# the build script

# the local path is the root path
LOCAL_PATH := $(call my-dir)
ROOT := $(ROOT)/..

# it is assumed that the vDM libraries are already built
include $(CLEAR_VARS)
LOCAL_MODULE := libsmm
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_ics/rls/libsmm.so
include $(PREBUILT_SHARED_LIBRARY)

