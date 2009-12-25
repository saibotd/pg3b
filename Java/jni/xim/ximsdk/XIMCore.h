#pragma once

#define XIMAPI __stdcall

typedef int           XIMSTATUS;
typedef unsigned int  XIMMODE;
typedef unsigned char XIMDIGITAL;
typedef short         XIMANALOG;

#define XIMSTATUS_OK                            0
#define XIMSTATUS_INVALID_INPUT_REFERENCE       101
#define XIMSTATUS_INVALID_MODE                  102
#define XIMSTATUS_INVALID_STICK_VALUE           103
#define XIMSTATUS_INVALID_TRIGGER_VALUE         104
#define XIMSTATUS_INVALID_TIMEOUT_VALUE         105
#define XIMSTATUS_INVALID_BUFFER                107
#define XIMSTATUS_INVALID_DEADZONE_TYPE         108
#define XIMSTATUS_HARDWARE_ALREADY_CONNECTED    109
#define XIMSTATUS_HARDWARE_NOT_CONNECTED        109
#define XIMSTATUS_DEVICE_NOT_FOUND              401
#define XIMSTATUS_DEVICE_CONNECTION_FAILED      402
#define XIMSTATUS_CONFIGURATION_FAILED          403
#define XIMSTATUS_READ_FAILED                   404
#define XIMSTATUS_WRITE_FAILED                  405
#define XIMSTATUS_TRANSFER_CORRUPTION           406
#define XIMSTATUS_NEEDS_CALIBRATION             407

#define XIMModeNone                             0x00000000
#define XIMModeAutoAnalogDisconnect             0x00000001

#define XIMButtonPressed      TRUE
#define XIMButtonReleased     FALSE

#define XIMStickRest          ((XIMANALOG)0)
#define XIMStickRightMost     ((XIMANALOG)32767)
#define XIMStickLeftMost      ((XIMANALOG)-32767)
#define XIMStickUpMost        ((XIMANALOG)32767)
#define XIMStickDownMost      ((XIMANALOG)-32767)

#define XIMStickMaximum       ((XIMANALOG)32767)
#define XIMStickMinimum       ((XIMANALOG)-32767)

#define XIMTriggerRest        ((XIMANALOG)0)
#define XIMTriggerMost        ((XIMANALOG)32767)

#define XIMTriggerMinimum     ((XIMANALOG)0)
#define XIMTriggerMaximum     ((XIMANALOG)32767)


extern "C"
{

// sizeof(XIMXbox360Input) == 28
__declspec(align(1))
struct XIMXbox360Input
{
    XIMDIGITAL RightBumper;
    XIMDIGITAL RightStick;
    XIMDIGITAL LeftBumper;
    XIMDIGITAL LeftStick;
    XIMDIGITAL A;
    XIMDIGITAL B;
    XIMDIGITAL X;
    XIMDIGITAL Y;
    XIMDIGITAL Up;
    XIMDIGITAL Down;
    XIMDIGITAL Left;
    XIMDIGITAL Right;
    XIMDIGITAL Start;
    XIMDIGITAL Back;
    XIMDIGITAL Guide;
    XIMANALOG  RightStickX;
    XIMANALOG  RightStickY;
    XIMANALOG  LeftStickX;
    XIMANALOG  LeftStickY;
    XIMANALOG  RightTrigger;
    XIMANALOG  LeftTrigger;
};


//
// Core APIs.
//

// Connect to XIM hardware.
XIMSTATUS XIMAPI XIMConnect();

// Disconnect from XIM hardware.
void XIMAPI XIMDisconnect();

// Set runtime mode option (combined flags).
XIMSTATUS XIMAPI XIMSetMode(XIMMODE mode);

// Send Xbox 360 controller state.
// Controller state will persist (latch) until the next call. Method
// will not return until state is fully committed to the Xbox 360 controller and
// the specified timeout was met.
XIMSTATUS XIMAPI XIMSendXbox360Input(XIMXbox360Input* input, float timeoutMS);


//
// Utility APIs.
//

//
// Translate raw device input deltas (i.e. mouse, Wiimote, etc.) to Xbox 360 analog stick
// positions with input conditioning and dead zone compensation.
//

typedef void* XIMSMOOTHNESS;

#define XIMDeadZoneCircular   0
#define XIMDeadZoneSquare     1

XIMSMOOTHNESS XIMAPI XIMAllocSmoothness(float intensity, int inputUpdateFrequency, float stickYXRatio, float stickTranslationExponent, float stickSensitivity);
void XIMAPI XIMFreeSmoothness(XIMSMOOTHNESS smoothness);

XIMSTATUS XIMAPI XIMComputeStickValues(
    float deltaX, float deltaY,
    float stickYXRatio, float stickTranslationExponent, float stickSensitivity,
    float stickDiagonalDampen,
    XIMSMOOTHNESS stickSmoothness,
    int stickDeadZoneType, float stickDeadZone,
    OUT short* stickResultX, OUT short* stickResultY);
}
