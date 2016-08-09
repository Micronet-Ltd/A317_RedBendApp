LOCAL_PATH := $(call my-dir)
ROOT := $(ROOT)/..

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmplat
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmplat.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcpldp
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcpldp.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcpldir
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcpldir.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmscinv
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmscinv.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmutil
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmutil.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcplgeneric
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcplgeneric.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmscomo
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmscomo.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libdmacoapp
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libdmacoapp.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmengine
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmengine.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcplecu
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcplecu.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmcomm
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmcomm.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmdescmo
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmdescmo.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcplua
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcplua.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswmcadapter
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libswmcadapter.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmplclient
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmplclient.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcpldevice
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcpldevice.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswmcinstallers
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libswmcinstallers.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmipc
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmipc.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmsmm
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmsmm.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmfumo
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmfumo.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmsmmpl
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmsmmpl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmswmcplselfupgrade
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmswmcplselfupgrade.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libbl
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libbl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libdma_jni
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libdma_jni.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libvdmlawmo
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libvdmlawmo.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libdmammi
LOCAL_SRC_FILES := ../../android_ndk46/android_native_R8b_jb/rls/libdmammi.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libsmm
LOCAL_WHOLE_STATIC_LIBRARIES := libvdmplat libvdmswmcpldp libvdmswmcpldir libvdmscinv libvdmutil libvdmswmcplgeneric libvdmscomo libdmacoapp libvdmengine libvdmswmcplecu libvdmcomm libvdmdescmo libvdmswmcplua libswmcadapter libvdmplclient libvdmswmcpldevice libswmcinstallers libvdmipc libvdmsmm libvdmfumo libvdmsmmpl libvdmswmcplselfupgrade libbl libdma_jni libvdmlawmo libdmammi 
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)
