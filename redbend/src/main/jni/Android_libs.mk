# PLATFORM TOOLCHAIN and BUILD variables are taken through the environment from
# the build script

# the local path is the root path
LOCAL_PATH := $(call my-dir)
ROOT := $(ROOT)/..

# it is assumed that the vDM libraries are already built
include $(CLEAR_VARS)
LOCAL_MODULE := libsmm

LOCAL_CFLAGS := -Wall -DDEBUG -DVDM_PL_ANDROID

LOCAL_LDLIBS := -L ../android_ndk46/android_native_R8b_jb/rls/ \
				-libdmacoapp.a -libdma_jni.a -libdmammi.a -libua_handoff_installer.a -libvdmcomm.a \
				-libvdmengine.a -libvdmfumo.a -libvdmipc.a -libvdmlawmo.a -libvdmplat.a -libvdmplclient.a \
				-libvdmscinv.a -libvdmscomo.a -libvdmsmm.a -libvdmsmmpl.a -libvdmswmcpldevice.a \
				-libvdmswmcpldir.a -libvdmswmcplua.a -libvdmutil.a -libvdmxml.a

include $(BUILD_SHARED_LIBRARY)

