/*
 ********************************************************************************
 * Copyright (c) 2009 Richard Burke
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holders nor the names of
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * $Rev: 220 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-26 12:19:45 -0500 (Thu, 26 Nov 2009) $
 ********************************************************************************
 */
#ifndef __XBOX_DRIVER_H__
#define __XBOX_DRIVER_H__

#define XB_NewButton(p,b)       ((uint8_t)(((p)<<4)|(b)))
#define XB_ButtonPort(t)        ((uint8_t)(((t)>>4)&0x0f))
#define XB_ButtonPin(t)         ((uint8_t)((t)&0x0f))

#define XB_NewControl(p,b)      ((uint8_t)(((p)<<4)|(b)))
#define XB_ControlPin(t)        ((uint8_t)(((t)>>4)&0x0f))
#define XB_ControlPot(t)        ((uint8_t)((t)&0x0f))

#define XB_GetTableAddress(t)   ((uint16_t)(((t)<<8)+32))
#define XB_GetTableEnabled(t,v) (_BV(t)&(v))
#define XB_SetTableEnabled(a)   

#define POT1                    1
#define POT2                    2

#define ACTION_WIPER            0x98
#define ACTION_VW_IDLE          128
#define ACTION_VW_LOW           0
#define ACTION_VW_HIGH          255
#define ACTION_VW_MAXVECTOR     127
#define ACTION_VW_MINVECTOR     0

/*
 ****************************************************************************************************
 * Actions
 *
 *  ------------- ------------- -------------------------
 * | D0 D1 D2 D3 | T0 T1 T2 T3 | S7 S6 S5 S4 S3 S2 S1 S0 |
 *  ------------- ------------- -------------------------
 *   ^             ^             ^------------------------  XBoxValue_t : Idle, ... Analog
 *   |             `-------------------------------------- XBoxTarget_t : LeftStickX, ... RightTrigger
 *   `----------------------------------------------------     Device_t : XBox
 *
 * or,
 *
 *  ------------- ------------- -------------------------
 * | D0 D1 D2 D3 | T0 T1 T2 T3 | S7 S6 S5 S4 S3 S2 S1 S0 |
 *  ------------- ------------- -------------------------
 *   ^             ^             ^------------------------ XBoxButton_t : AButton, ... BackButton
 *   |             `-------------------------------------- XBoxTarget_t : XKeyPress, XKeyRelease
 *   `----------------------------------------------------     Device_t : XBox
 *
 ****************************************************************************************************
 */
// XBox Controllers
typedef enum {  WiredCommonLineV1, WirelessCommonGroundV1 } XBoxGamePad_t;

// XBox Targets
typedef enum {  LeftStickX, LeftStickY, RightStickX, RightStickY, LeftTrigger, RightTrigger, XKeyPress, XKeyRelease } XBoxTarget_t;

// XBox Values for LeftStickX, LeftStickY, RightStickX, RightStickY, LeftTrigger, RightTrigger
typedef enum {  Idle, Low, High, Analog } XBoxValue_t;
typedef enum {  MoveLeft = High, MoveRight = Low, MoveUp = High, MoveDown = Low, MoveForward = High, MoveBackward = Low, MoveIdle = Idle } XBoxMoveAlias_t;
typedef enum {  LookUp = High, LookDown = Low, LookLeft = Low, LookRight = High, LookIdle = Idle } XBoxLookAlias_t;
typedef enum {  PullTrigger = High, ReleaseTrigger = Low } XBoxTriggerAlias_t;

// XBox Values for XKeyPress, XKeyRelease
typedef enum {  AButton, BButton, XButton, YButton, UpPad, DownPad, LeftPad, RightPad, LeftButton, RightButton, LeftStickButton, RightStickButton,
                StartButton, GuideButton, BackButton } XBoxButton_t;

// Internal Constants
typedef enum {  PortC = 0x08, PortD = 0x0B } ButtonPort_t;
typedef enum {  PortB = 0x05 } ControlPort_t;
typedef enum {  Pot1 = 1, Pot2 = 2 } Pot_t;

// XBox Actions
#define XB_Control(c,v)			EVA_NewAction( EVA_NewActionKey( XBox, ( c ) ), ( v ) )
#define XB_ButtonPress(b)		EVA_NewAction( EVA_NewActionKey( XBox, XKeyPress ), (b) )
#define XB_ButtonRelease(b)		EVA_NewAction( EVA_NewActionKey( XBox, XKeyRelease ), (b) )

extern void XB_Init( void );
void XB_EnableCalibration( uint8_t state );
extern void XB_XBoxAction( uint16_t event, uint16_t action );
extern void XB_SetIsWireless( uint8_t state );

#endif
