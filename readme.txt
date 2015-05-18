Build:
======
1.go to "redbend" folder
2.run "$ANDROID_NDK_ROOT/ndk-build"
3.run "$ANDROID_SDK_ROOT/tools/android update project -p . --target android-14"
3a.run "$ANDROID_SDK_ROOT/tools/android update project -p ../common --target android-14"
3b.run "$ANDROID_SDK_ROOT/tools/android update project -p ../swm_common --target android-14"
4.run "ant debug" (or release)

Integration:
============
- if you want to replace one of RedBend's libraries with your own implementation, use redbend/jni/Android_libs.mk: Rename the original redbend/jni/Android.mk to redbend/jni/Android.mk~ for example, and redbend/jni/Android_libs.mk to  redbend/jni/Android.mk
    proceed to step 2 in "Build" section.
- If you want to replace the Drop-In client Google OTA configuration file -- you need to sign it with the system certificate of the target device:
  redbend/gota_config/build_config.sh <model> <public_key> <private_key> <output_file>
  See redbend/gota_config/maguro directory for an example
  The output file should be placed at: redbend/assets/files/rb_ota.zip
  Then proceed with step 4 above, to rebuild the application

Directory structure:
====================
apk:
common/
swm_common/
com.redbend.client.apk

libraries built for platform:
android_ndk46/android_native_R8b_ics/rls/
android_ndk46/android_native_R8b_ics/rls_dbginfo/

vDM Porting Layer source files
android_ndk46/android_native_R8b_ics/platform

android application:
redbend/

vDM Porting Layer header files:
sdk/import

Installation on a device:
apk should be placed in /system/app
libsmm.so should be placed in /system/lib

integration with vRM UA files:
setup/rb_ua 
	-should be placed in /system/SWM-Client/SYSAPK
	-note that this is not the update agent executable, only a file which holds the version
	 
Sample vRM configuration files (suitable for maguro platform):
setup/rb_recovery.fstab (should be placed in /system/etc/)
setup/rb_ua.conf (should be placed in /system/etc/)

integration with SWM server files:
setup/com.redbend.client (should be placed in (/system/SWM-Client/SYSAPK)

