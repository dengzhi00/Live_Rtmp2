LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= rtmp

LOCAL_C_INCLUDES := $(LOCAL_PATH)

#定义宏遍历jni下所有文件及子目录文件
define audio-all-cpp-files
$(patsubst ./%,%, \
  $(shell cd $(LOCAL_PATH) ;\
          find $(1) -name "*.c*") \
 )
endef
LOCAL_SRC_FILES := $(call audio-all-cpp-files,.)
#LOCAL_SRC_FILES :=  \
	amf.c		\
	hashswf.c   \
	log.c		\
	parseurl.c  \
	rtmp.c 		
#LOCAL_SRC_FILES := librtmp.so
	
LOCAL_CFLAGS := -Wall -O2 -DSYS=posix -DNO_CRYPTO


include $(BUILD_STATIC_LIBRARY)
