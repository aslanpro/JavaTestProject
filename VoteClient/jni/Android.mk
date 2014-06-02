LOCAL_PATH:= $(call my-dir)

# first lib, which will be built statically
#
include $(CLEAR_VARS)

LOCAL_MODULE    := libstaticAES
LOCAL_SRC_FILES := AES.cpp

include $(BUILD_STATIC_LIBRARY)

# second lib, which will depend on and include the first one
#
include $(CLEAR_VARS)

LOCAL_MODULE    := libAES
LOCAL_SRC_FILES := aes_jni.cpp

LOCAL_STATIC_LIBRARIES := libstaticAES

include $(BUILD_SHARED_LIBRARY)
