

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif


#include <jni.h>
#include <windows.h>
#include <XIMCore.h>

extern "C" {

JNIEXPORT jint JNICALL Java_com_esotericsoftware_controller_xim_XIM_connect
(JNIEnv* env, jclass c) {
	return XIMConnect();
}

JNIEXPORT void JNICALL Java_com_esotericsoftware_controller_xim_XIM_disconnect
(JNIEnv* env, jclass c) {
	XIMDisconnect();
}

JNIEXPORT jint JNICALL Java_com_esotericsoftware_controller_xim_XIM_setMode
(JNIEnv* env, jclass c, jint mode) {
	return XIMSetMode(mode);
}

JNIEXPORT jint JNICALL Java_com_esotericsoftware_controller_xim_XIM_setState
(JNIEnv* env, jclass c, jobject byteBuffer, jfloat timeout) {
	XIMXbox360Input *input = (XIMXbox360Input*)env->GetDirectBufferAddress(byteBuffer);
	return XIMSendXbox360Input(input, timeout);
}

static XIMSMOOTHNESS smoothness;

JNIEXPORT void JNICALL Java_com_esotericsoftware_controller_xim_XIMMouseTranslation_setSmoothness (
	JNIEnv* env, jclass c,
	jfloat intensity, jint inputUpdateFrequency, jfloat stickYXRatio, jfloat stickTranslationExponent,
	jfloat stickSensitivity
) {
	if (smoothness) XIMFreeSmoothness(smoothness);
	smoothness = XIMAllocSmoothness(
		intensity, inputUpdateFrequency, stickYXRatio, stickTranslationExponent, stickSensitivity
	);
}

JNIEXPORT void JNICALL Java_com_esotericsoftware_controller_xim_XIMMouseTranslation_computeStickValues (
	JNIEnv* env, jclass c,
    jfloat deltaX, jfloat deltaY,
    jfloat stickYXRatio, jfloat stickTranslationExponent, jfloat stickSensitivity,
    jfloat stickDiagonalDampen,
    jint stickDeadZoneType, jfloat stickDeadZone,
	jobject byteBuffer
) {
	if (!smoothness) return;
	short stickResultX, stickResultY;
	XIMComputeStickValues(
		deltaX, deltaY,
		stickYXRatio, stickTranslationExponent, stickSensitivity,
		stickDiagonalDampen,
		smoothness,
		stickDeadZoneType, stickDeadZone,
		&stickResultX, &stickResultY
	);
	short *buffer = (short*)env->GetDirectBufferAddress(byteBuffer);
	buffer[0] = stickResultX;
	buffer[1] = stickResultY;
}

}
