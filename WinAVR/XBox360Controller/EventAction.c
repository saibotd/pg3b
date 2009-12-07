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
 * $Rev: 191 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-16 00:48:21 -0500 (Mon, 16 Nov 2009) $
 ********************************************************************************
 */
#include <avr/pgmspace.h>
#include "RingBuffer.h"
#include "SerialPeripheral.h"
#include "KeyboardDriver.h"
#include "MouseDriver.h"
#include "XBoxDriver.h"
#include "UsbDriver.h"
#include "EventAction.h"
#include "System.h"

/*
 ********************************************************************************
 * Private Functions
 ********************************************************************************
 */
static uint16_t EVA_LookupEvent( uint16_t event );

/*
 ********************************************************************************
 * Event Action Table
 ********************************************************************************
 */
static const uint16_t eventaction[] PROGMEM = {
    MS_DeltaX( MVector ),                   XB_Control( RightStickX, Analog ),          // (RS) Look Left / Right
    USB_Control( RightStickX, Analog ),     XB_Control( RightStickX, Analog ),
    MS_DeltaY( MVector ),                   XB_Control( RightStickY, Analog ),          // (RS) Look Up / Down
    USB_Control( RightStickY, Analog ),     XB_Control( RightStickY, Analog ),

    KB_KeyPress( AKey ),                    XB_Control( LeftStickX, MoveLeft ),         // (LS) Left
    KB_KeyRelease( AKey ),                  XB_Control( LeftStickX, MoveIdle ),
    KB_KeyPress( DKey ),                    XB_Control( LeftStickX, MoveRight ),        // (LS) Right
    KB_KeyRelease( DKey ),                  XB_Control( LeftStickX, MoveIdle ),
    USB_Control( LeftStickX, Analog ),      XB_Control( LeftStickX, Analog ),

    KB_KeyPress( WKey ),                    XB_Control( LeftStickY, MoveForward ),      // (LS) Forward
    KB_KeyRelease( WKey ),                  XB_Control( LeftStickY, MoveIdle ),
    KB_KeyPress( SKey ),                    XB_Control( LeftStickY, MoveBackward ),     // (LS) Backward
    KB_KeyRelease( SKey ),                  XB_Control( LeftStickY, MoveIdle ),
    USB_Control( LeftStickY, Analog ),      XB_Control( LeftStickY, Analog ),

    MS_ButtonPress( MRight ),               XB_Control( RightTrigger, PullTrigger ),    // (RT) Shoot
    MS_ButtonRelease( MRight ),             XB_Control( RightTrigger, ReleaseTrigger ),
    USB_Control( RightTrigger, Analog ),    XB_Control( RightTrigger, Analog ),

    MS_ButtonPress( MLeft ),                XB_Control( LeftTrigger, PullTrigger ),     // (LT) Shoot / Grenade
    MS_ButtonRelease( MLeft ),              XB_Control( LeftTrigger, ReleaseTrigger ),
    USB_Control( LeftTrigger, Analog ),     XB_Control( LeftTrigger, Analog ),

    MS_ButtonPress( MMiddle ),              XB_ButtonPress( BButton ),                  // (B) Melee
    MS_ButtonRelease( MMiddle ),            XB_ButtonRelease( BButton ),
    MS_ButtonPress( MWheelUp ),             XB_ButtonPress( YButton ),                  // (Y) Switch Weapon
    MS_ButtonRelease( MWheelUp ),           XB_ButtonRelease( YButton ),
    KB_KeyPress( YKey ),                    XB_ButtonPress( YButton ),                  // (Y) Switch Weapon
    KB_KeyRelease( YKey ),                  XB_ButtonRelease( YButton ),
    MS_ButtonPress( MWheelDown ),           XB_ButtonPress( RightStickButton ),         // (RS) Zoom
    MS_ButtonRelease( MWheelDown ),         XB_ButtonRelease( RightStickButton ),
    KB_KeyPress( ZKey ),                    XB_ButtonPress( RightStickButton ),         // (RS) Zoom
    KB_KeyRelease( ZKey ),                  XB_ButtonRelease( RightStickButton ),

    KB_KeyPress( LeftShiftKey ),            XB_ButtonPress( LeftStickButton ),          // (LS) Crouch
    KB_KeyRelease( LeftShiftKey ),          XB_ButtonRelease( LeftStickButton ),
    KB_KeyPress( EKey ),                    XB_ButtonPress( LeftButton ),               // (LB) Reload / Switch Grenade
    KB_KeyRelease( EKey ),                  XB_ButtonRelease( LeftButton ),
    KB_KeyPress( RKey ),                    XB_ButtonPress( RightButton ),              // (RB) Use / Board / Reload
    KB_KeyRelease( RKey ),                  XB_ButtonRelease( RightButton ),
    KB_KeyPress( FKey ),                    XB_ButtonPress( XButton ),                  // (X) Use Equipment
    KB_KeyRelease( FKey ),                  XB_ButtonRelease( XButton ),
    KB_KeyPress( SpaceKey ),                XB_ButtonPress( AButton ),                  // (A) Jump / Accept
    KB_KeyRelease( SpaceKey ),              XB_ButtonRelease( AButton ),

    KB_KeyPress( EnterKey ),                XB_ButtonPress( AButton ),                  // (A) Jump / Accept
    KB_KeyRelease( EnterKey ),              XB_ButtonRelease( AButton ),

    KB_EKeyPress( UpKey ),                  XB_ButtonPress( UpPad ),                    // DPad UP
    KB_EKeyRelease( UpKey ),                XB_ButtonRelease( UpPad ),
    KB_EKeyPress( DownKey ),                XB_ButtonPress( DownPad ),                  // DPad Down
    KB_EKeyRelease( DownKey ),              XB_ButtonRelease( DownPad ),
    KB_EKeyPress( LeftKey ),                XB_ButtonPress( LeftPad ),                  // DPad Left
    KB_EKeyRelease( LeftKey ),              XB_ButtonRelease( LeftPad ),
    KB_EKeyPress( RightKey ),               XB_ButtonPress( RightPad ),                 // DPad Right
    KB_EKeyRelease( RightKey ),             XB_ButtonRelease( RightPad ),

    KB_KeyPress( F1Key ),                   XB_ButtonPress( GuideButton ),              // Guide
    KB_KeyRelease( F1Key ),                 XB_ButtonRelease( GuideButton ),
    KB_KeyPress( F2Key ),                   XB_ButtonPress( StartButton ),              // Start
    KB_KeyRelease( F2Key ),                 XB_ButtonRelease( StartButton ),
    KB_KeyPress( F3Key ),                   XB_ButtonPress( BackButton ),               // Back
    KB_KeyRelease( F3Key ),                 XB_ButtonRelease( BackButton ),

    KB_KeyPress( D0Key ),                   XB_Control( LeftStickX, MoveLeft ),
    KB_KeyRelease( D0Key ),                 XB_Control( LeftStickX, MoveIdle ),
    KB_KeyPress( D1Key ),                   XB_Control( LeftStickX, MoveRight ),
    KB_KeyRelease( D1Key ),                 XB_Control( LeftStickX, MoveIdle ),
    KB_KeyPress( D2Key ),                   XB_Control( LeftStickY, MoveForward ),
    KB_KeyRelease( D2Key ),                 XB_Control( LeftStickY, MoveIdle ),
    KB_KeyPress( D3Key ),                   XB_Control( LeftStickY, MoveBackward ),
    KB_KeyRelease( D3Key ),                 XB_Control( LeftStickY, MoveIdle ),

    KB_KeyPress( D4Key ),                   XB_Control( RightStickX, LookLeft ),
    KB_KeyRelease( D4Key ),                 XB_Control( RightStickX, LookIdle ),
    KB_KeyPress( D5Key ),                   XB_Control( RightStickX, LookRight ),
    KB_KeyRelease( D5Key ),                 XB_Control( RightStickX, LookIdle ),
    KB_KeyPress( D6Key ),                   XB_Control( RightStickY, LookUp ),
    KB_KeyRelease( D6Key ),                 XB_Control( RightStickY, LookIdle ),
    KB_KeyPress( D7Key ),                   XB_Control( RightStickY, LookDown ),
    KB_KeyRelease( D7Key ),                 XB_Control( RightStickY, LookIdle ),

    KB_KeyPress( D8Key ),                   XB_Control( LeftTrigger, PullTrigger ),
    KB_KeyRelease( D8Key ),                 XB_Control( LeftTrigger, ReleaseTrigger ),
    KB_KeyPress( D9Key ),                   XB_Control( RightTrigger, PullTrigger ),
    KB_KeyRelease( D9Key ),                 XB_Control( RightTrigger, ReleaseTrigger )
};

/*
 ********************************************************************************
 * EVA_LookupEvent
 ********************************************************************************
 */
static uint16_t EVA_LookupEvent( uint16_t event )
{
    uint16_t action = 0;

    SYS_LogWord( 'L', event );
    for ( uint16_t i = 0; i < sizeof( eventaction ) / sizeof( uint16_t ); i += 2 )
    {
        if( event == pgm_read_word( &eventaction[i] ) )
        {
            action = pgm_read_word( &eventaction[i + 1] );
            break;
        }
    }

    return action;
}

/*
 ********************************************************************************
 * EVA_InvokeEventAction
 ********************************************************************************
 */
void EVA_InvokeEventAction( uint16_t event )
{
    uint16_t action, special;

    special = EVA_NewEvent( EVA_NewEventKey( EVA_EventDevice( event ), EVA_EventTrigger( event ) ), EVA_EventValue( EVA_NullEvent ) );

    if( MS_FindByKey( event ) || KB_FindByKey( event ) || USB_FindByKey( event ) )
        action = EVA_LookupEvent( special );
    else
        action = EVA_LookupEvent( event );

    SYS_LogWord( 'I', action );

    EVA_InvokeAction( event, action );
}

/*
 ********************************************************************************
 * EVA_InvokeAction
 ********************************************************************************
 */
void EVA_InvokeAction( uint16_t event, uint16_t action )
{
    switch( EVA_ActionDevice( action ) )
    {
        case XBox: XB_XBoxAction( event, action ); break;
    }
}

/*
 ********************************************************************************
 * EVA_PerformAction
 ********************************************************************************
 */
void EVA_PerformAction( uint16_t event )
{
    uint8_t device = EVA_EventDevice( event );
    uint8_t target = EVA_EventTrigger( event );
    uint8_t value = EVA_EventValue( event );

    uint16_t action = ( target == XKeyPress || target == XKeyRelease )?
        EVA_NewAction( EVA_NewActionKey( device, target ), value ) :
        EVA_NewAction( EVA_NewActionKey( device, target ), Analog );
    EVA_InvokeAction( event, action );
}
