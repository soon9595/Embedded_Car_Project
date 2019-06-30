#include <jni.h>
#include "android/log.h"
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/unistd.h>
#include <sys/syscall.h>
#include <asm/ioctl.h>

#define LOG_TAG "NATIVE"
#define SWITCH_SIZE 9
#define driver_name "/dev/dev_driver"
#define push_switch "/dev/fpga_push_switch"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libnav", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libnav", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libnav", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libnav", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libnav", __VA_ARGS__)

JNIEXPORT jint JNICALL Java_com_embe_bluetoothtest_DeviceManager_openDriver(JNIEnv *env, jobject this){
	int fd;
	LOGI("JNI log");
	fd = open(push_switch, O_RDWR);

	if(fd < 0){
		LOGI("%s", strerror(errno));
		exit(-1);
	}
	return fd;
}
JNIEXPORT jint JNICALL Java_com_embe_bluetoothtest_BluetoothTest_openDriver(JNIEnv *env, jobject this){
	int fd;
	LOGI("JNI log2222");
	fd = open(driver_name, O_RDWR);

	if(fd < 0){
		LOGI("%s2222", strerror(errno));
		exit(-1);
	}
	return fd;
}
JNIEXPORT jint JNICALL Java_com_embe_bluetoothtest_DeviceManager_SwitchOpen(JNIEnv *env, jobject this,jint fd)
{
	unsigned char sw[SWITCH_SIZE];
	int bt=-1;
	int i;
	int size = sizeof(sw);;

	read(fd, sw, size);
	for(i = 0; i < SWITCH_SIZE; i++){
		if(sw[i]== 1) { // change from 0 to 1
			bt = i;
			break;
		}
	}
	return bt;
}

JNIEXPORT void JNICALL Java_com_embe_bluetoothtest_BluetoothTest_writeDriver(JNIEnv *env, jobject this, jint fd, jint direction, jint mode){
	int val = syscall(376,direction,mode);
	LOGI("val = %d",val);
	int ret = write(fd, &val, sizeof(val));
}

JNIEXPORT jint JNICALL Java_com_embe_bluetoothtest_DeviceManager_readDriver(JNIEnv *env, jobject this, jint fd){
	char buf[2] = {0};
	int ret = read(fd, buf, 2);
	return ret;
}

JNIEXPORT void JNICALL Java_com_embe_bluetoothtest_DeviceManager_closeDriver(JNIEnv *env, jobject this, jint fd){
	int ret = close(fd);
	LOGV("close file: %d", ret);
}
