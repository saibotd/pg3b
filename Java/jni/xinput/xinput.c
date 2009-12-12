

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif


#include <stdio.h>
#include <tchar.h>
#include <jni.h>
#include <windows.h>
#include <xinput.h>


JNIEXPORT void JNICALL Java_pg3b_xboxcontroller_XInputXboxController_setEnabled
(JNIEnv* env, jclass c, jboolean enabled) {
	XInputEnable((BOOL)enabled);
}


JNIEXPORT void JNICALL Java_pg3b_xboxcontroller_XInputXboxController_poll
(JNIEnv* env, jclass c, jint index, jobject byteBuffer) {
	short *buffer = (short*)(*env)->GetDirectBufferAddress(env, byteBuffer);
	XINPUT_STATE state;
	DWORD result = XInputGetState((int)index, &state);
	if (result != ERROR_SUCCESS) {
		buffer[0] = 0;
		return;
	}
	buffer[0] = 1;
	buffer[1] = state.Gamepad.wButtons;
	buffer[2] = state.Gamepad.bLeftTrigger;
	buffer[3] = state.Gamepad.bRightTrigger;
	buffer[4] = state.Gamepad.sThumbLX;
	buffer[5] = state.Gamepad.sThumbLY;
	buffer[6] = state.Gamepad.sThumbRX;
	buffer[7] = state.Gamepad.sThumbRY;
}
