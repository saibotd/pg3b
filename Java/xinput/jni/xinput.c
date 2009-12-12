

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif


#include <stdio.h>
#include <tchar.h>
#include <jni.h>
#include <windows.h>
#include <xinput.h>


JNIEXPORT void JNICALL Java_pg3b_XInput_setEnabled
(JNIEnv* env, jclass c, jboolean enabled) {
	XInputEnable((BOOL)enabled);
}


JNIEXPORT void JNICALL Java_pg3b_XInput_poll
(JNIEnv* env, jclass c, jobject byteBuffer) {
	short *buffer = (short*)(*env)->GetDirectBufferAddress(env, byteBuffer);
	XINPUT_STATE state;
	DWORD i = 0;
	while (i < 4 * 8) {
		DWORD result = XInputGetState(i, &state);
		if (result != ERROR_SUCCESS) {
			buffer[i] = 0;
			i += 8;
			continue;
		}
		buffer[i++] = 1;
		buffer[i++] = state.Gamepad.wButtons;
		buffer[i++] = state.Gamepad.bLeftTrigger;
		buffer[i++] = state.Gamepad.bRightTrigger;
		buffer[i++] = state.Gamepad.sThumbLX;
		buffer[i++] = state.Gamepad.sThumbLY;
		buffer[i++] = state.Gamepad.sThumbRX;
		buffer[i++] = state.Gamepad.sThumbRY;
	}
}
