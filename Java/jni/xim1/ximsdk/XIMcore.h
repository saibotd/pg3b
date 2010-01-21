#pragma once

#define XIMAPI __stdcall

#define XIMSTATUS int

#define XIMSTATUS_OK                            0
#define XIMSTATUS_INVALID_INPUT_REFERENCE       101
#define XIMSTATUS_INVALID_STICK_VALUE           102
#define XIMSTATUS_INVALID_BUFFER                103
#define XIMSTATUS_INVALID_DEADZONE_TYPE         104
#define XIMSTATUS_HARDWARE_ALREADY_CONNECTED    105
#define XIMSTATUS_HARDWARE_NOT_CONNECTED        106
#define XIMSTATUS_DEVICE_NOT_FOUND              401
#define XIMSTATUS_DEVICE_CONNECTION_FAILED      402
#define XIMSTATUS_CONFIGURATION_FAILED          403
#define XIMSTATUS_READ_FAILED                   404
#define XIMSTATUS_WRITE_FAILED                  405
#define XIMSTATUS_TRANSFER_CORRUPTION           406

#define XIMButtonPressed      TRUE

#define XIMStickRest          ((char)0)
#define XIMStickRightMost     ((char)127)
#define XIMStickLeftMost      ((char)-127)
#define XIMStickUpMost        ((char)127)
#define XIMStickDownMost      ((char)-127)

#define XIMStickMaximum       ((char)127)
#define XIMStickMinimum       ((char)-127)

extern "C"
{

// sizeof(XIMXbox360Input) == 72
__declspec(align(1))
struct XIMXbox360Input
{
    BOOL LeftTrigger;
    BOOL LeftBumper;
    BOOL LeftStick;
    BOOL RightTrigger;
    BOOL RightBumper;
    BOOL RightStick;
    BOOL A;
    BOOL B;
    BOOL X;
    BOOL Y;
    BOOL Up;
    BOOL Down;
    BOOL Left;
    BOOL Right;
    BOOL Start;
    BOOL Back;
    BOOL Guide;
    char RightStickX;
    char RightStickY;
    char LeftStickX;
    char LeftStickY;
};


//
// Core APIs.
//

// Connect to XIM hardware.
XIMSTATUS XIMAPI XIMConnect();

// Disconnect from XIM hardware.
void XIMAPI XIMDisconnect();

// Send Xbox 360 controller state.
//
// Controller state will persist (latch) until the next call. Method
// will not return until state is fully committed to the Xbox 360 controller.
XIMSTATUS XIMAPI XIMSendXbox360Input(XIMXbox360Input* input);


//
// Utility APIs.
//

// Translate raw device input deltas (i.e. mouse, Wiimote, etc.) to Xbox 360 analog stick
// positions with input conditioning and dead zone size and shape compensation.
//
// See Default.xim for an explanation of values.
//
// NOTE: If stick smoothness is used (i.e. trigger value > 0), then global "last stick position"
// persistent storage must provided (i.e. global scope) that is reused on every computation
// call for a given stick.

#define XIMDeadZoneCircular   0
#define XIMDeadZoneSquare     1

XIMSTATUS XIMAPI XIMComputeStickValues(
    float deltaX, float deltaY, float deltaDeadZone, float stickYXRatio,
    float stickDeadZone, int stickDeadZoneType, float stickTranslationExponent, float stickSensitivity,
    float stickSmoothness, 
    OUT float* lastStickPositionX, OUT float* lastStickPositionY, OUT char* stickResultX, OUT char* stickResultY);
}
