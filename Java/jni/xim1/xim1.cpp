

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif


#include <jni.h>
#include <windows.h>
#include <XIMCore.h>

extern "C" {

JNIEXPORT jint JNICALL Java_com_esotericsoftware_controller_xim_XIM1_connect
(JNIEnv* env, jclass c) {
	return XIMConnect();
}

JNIEXPORT void JNICALL Java_com_esotericsoftware_controller_xim_XIM1_disconnect
(JNIEnv* env, jclass c) {
	XIMDisconnect();
}

JNIEXPORT jint JNICALL Java_com_esotericsoftware_controller_xim_XIM1_setState
(JNIEnv* env, jclass c, jobject byteBuffer) {
	XIMXbox360Input *input = (XIMXbox360Input*)env->GetDirectBufferAddress(byteBuffer);
	return XIMSendXbox360Input(input);
}

static float lastStickPositionX, lastStickPositionY;

JNIEXPORT void JNICALL Java_com_esotericsoftware_controller_xim_XIM1MouseTranslation_computeStickValues (
	JNIEnv* env, jclass c,
	jfloat deltaX, jfloat deltaY,
	jfloat stickYXRatio, jfloat stickTranslationExponent, jfloat stickSensitivity,
	jint stickDeadZoneType, jfloat stickDeadZone, jfloat deltaDeadZone, jfloat stickSmoothness,
	jobject byteBuffer
) {
	char stickResultX, stickResultY;
	XIMComputeStickValues(
		deltaX, deltaY, deltaDeadZone,
		stickYXRatio, stickDeadZone, stickDeadZoneType, 
		stickTranslationExponent, stickSensitivity,
		stickSmoothness,
		&lastStickPositionX, &lastStickPositionY,
		&stickResultX, &stickResultY
	);
	char *buffer = (char*)env->GetDirectBufferAddress(byteBuffer);
	buffer[0] = stickResultX;
	buffer[1] = stickResultY;
}

}
