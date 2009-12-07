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
 * $Rev: 230 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-27 23:49:30 -0500 (Fri, 27 Nov 2009) $
 ********************************************************************************
 */
#include <avr/io.h>
#include <avr/pgmspace.h>
#include "EventAction.h"
#include "Calibration.h"
#include "XBoxDriver.h"
#include "SerialPeripheral.h"
#include "System.h"

/*
 ********************************************************************************
 * Local Variables
 ********************************************************************************
 */
static uint8_t calibrated = 0;
static uint8_t isWireless = 0;

// Mapping from the button enumeration to physical hardware 
static const uint8_t buttons[] PROGMEM = {
    XB_NewButton( PortD, PD0 ),     // AButton
    XB_NewButton( PortD, PD1 ),     // BButton
    XB_NewButton( PortD, PD2 ),     // XButton
    XB_NewButton( PortD, PD3 ),     // YButton
    XB_NewButton( PortD, PD4 ),     // UpButton
    XB_NewButton( PortD, PD5 ),     // DownButton
    XB_NewButton( PortD, PD6 ),     // LeftButton
    XB_NewButton( PortD, PD7 ),     // RightButton
    XB_NewButton( PortC, PC1 ),     // LeftButton
    XB_NewButton( PortC, PC0 ),     // RightButton
    XB_NewButton( PortC, PC5 ),     // LeftStickButton
    XB_NewButton( PortC, PC6 ),     // RightStickButton
    XB_NewButton( PortC, PC3 ),     // StartButton
    XB_NewButton( PortC, PC4 ),     // GuideButton
    XB_NewButton( PortC, PC2 )      // BackButton
};

// Mapping from the control enumeration to Serial Peripheral Interface (SPI)
static const uint8_t controls[] PROGMEM = {
    XB_NewControl( PB4, POT1 ),     // LeftStickX
    XB_NewControl( PB4, POT2 ),     // LeftStickY
    XB_NewControl( PB5, POT1 ),     // RightStickX
    XB_NewControl( PB5, POT2 ),     // RightStickY
    XB_NewControl( PB6, POT2 ),     // LeftTrigger
    XB_NewControl( PB6, POT1 ),     // RightTrigger
};

/*
 ********************************************************************************
 * Private Functions
 ********************************************************************************
 */
static uint8_t XB_CalibrateWiper( uint8_t trigger, uint8_t value );
static void XB_ControlAction( uint16_t event, uint16_t action );
static void XB_ButtonAction( uint16_t event, uint16_t action );
static void XB_ResetButtons( void );
static void XB_ResetControls( void );

/*
 ********************************************************************************
 * XB_Init
 ********************************************************************************
 */
void XB_Init( void )
{
    /* Initialize communications with MCP42010 */
    SPI_InitMCP42010( );
    SPI_InitMaster( );

    // Enable Output
    DDRD |= ( _BV(PD0) | _BV(PD1) | _BV(PD2) | _BV(PD3) | _BV(PD4) | _BV(PD5) | _BV(PD6) | _BV(PD7) );
    DDRC |= ( _BV(PC0) | _BV(PC1) | _BV(PC2) | _BV(PC3) | _BV(PC4) | _BV(PC5) | _BV(PC6) | _BV(PC7) );

    // All buttons released
    XB_ResetButtons( );

    // ALl controls at idle state
    XB_ResetControls( );
}

/*
 ********************************************************************************
 * XB_XBoxAction
 ********************************************************************************
 */
void XB_XBoxAction( uint16_t event, uint16_t action )
{
    switch( EVA_ActionTarget( action ) )
    {
        case XKeyPress: XB_ButtonAction( event, action ); break;
        case XKeyRelease: XB_ButtonAction( event, action ); break;
        default: XB_ControlAction( event, action ); break;
    }
}

/*
 ********************************************************************************
 * XB_ButtonAction
 ********************************************************************************
 */
void XB_ButtonAction( uint16_t event, uint16_t action )
{
    uint8_t button = pgm_read_byte( &buttons[ EVA_ActionValue( action ) ] );
    uint8_t port = XB_ButtonPort( button );
    uint8_t pin = _BV( XB_ButtonPin( button ) );

    SYS_LogByte( 'B', button );
    SYS_LogByte( 'V', EVA_ActionValue( action ) );

    if( ! isWireless )
    {
        if( EVA_ActionTarget( action ) == XKeyPress )
            _SFR_IO8( port ) |= pin;        // Hi
        else
            _SFR_IO8( port ) &= ~pin;       // Lo
    }
    else
    {
        if( EVA_ActionTarget( action ) == XKeyPress )
            _SFR_IO8( port ) &= ~pin;       // Lo
        else
            _SFR_IO8( port ) |= pin;        // Hi
    }
}

/*
 ********************************************************************************
 * XB_ControlAction
 ********************************************************************************
 */
void XB_ControlAction( uint16_t event, uint16_t action )
{
    uint8_t control = pgm_read_byte( &controls[ EVA_ActionTarget( action ) ] );
    uint8_t target = EVA_ActionTarget( action );
    uint8_t wiper;

    switch( EVA_ActionValue( action ) )
    {
        case Idle:
            wiper = XB_CalibrateWiper( target, ACTION_VW_IDLE );
            break;
        case Low:
            wiper = XB_CalibrateWiper( target, ACTION_VW_LOW );
            break;
        case High:
            wiper = XB_CalibrateWiper( target, ACTION_VW_HIGH );
            break;
        default:
            wiper = XB_CalibrateWiper( target, EVA_EventValue( event ) );
    }

    SYS_LogByte( 'C', control );
    SYS_LogByte( 'V', wiper );

    SPI_TransferBytes( XB_ControlPin( control ), ACTION_WIPER | XB_ControlPot( control ), wiper );
}

/*
 ********************************************************************************
 * XB_CalibrateWiper
 ********************************************************************************
 */
static uint8_t XB_CalibrateWiper( uint8_t target, uint8_t value )
{
    if( isWireless && ( target == LeftTrigger || target == RightTrigger ) )
        value = ~value;

    if( calibrated == 0 )
    {
        return value;
    }

    switch( target )
    {
        case LeftTrigger:
            return pgm_read_byte( &analog_LT[ value ] );
        case RightTrigger:
            return pgm_read_byte( &analog_RT[ value ] );
        case RightStickX:
            return pgm_read_byte( &analog_RSX[ value ] );
        case RightStickY:
            return pgm_read_byte( &analog_RSY[ value ] );
        case LeftStickX:
            return pgm_read_byte( &analog_LSX[ value ] );
        case LeftStickY:
            return pgm_read_byte( &analog_LSY[ value ] );
    }

    return value;
}

/*
 ********************************************************************************
 * XB_ResetButtons
 ********************************************************************************
 */
static void XB_ResetButtons( void )
{
    if( isWireless )
    {
        PORTD |= ( _BV(PD0) | _BV(PD1) | _BV(PD2) | _BV(PD3) | _BV(PD4) | _BV(PD5) | _BV(PD6) | _BV(PD7) );
        PORTC |= ( _BV(PC0) | _BV(PC1) | _BV(PC2) | _BV(PC3) | _BV(PC4) | _BV(PC5) | _BV(PC6) | _BV(PC7) );
    }
    else
    {
        PORTD &= ~( _BV(PD0) | _BV(PD1) | _BV(PD2) | _BV(PD3) | _BV(PD4) | _BV(PD5) | _BV(PD6) | _BV(PD7) );
        PORTC &= ~( _BV(PC0) | _BV(PC1) | _BV(PC2) | _BV(PC3) | _BV(PC4) | _BV(PC5) | _BV(PC6) | _BV(PC7) );
    }
}

/*
 ********************************************************************************
 * XB_ResetControls
 ********************************************************************************
 */
static void XB_ResetControls( void )
{
    XB_ControlAction( LeftTrigger, ReleaseTrigger  );
    XB_ControlAction( RightTrigger, ReleaseTrigger );
    XB_ControlAction( LeftStickX, MoveIdle );
    XB_ControlAction( LeftStickY, MoveIdle );
    XB_ControlAction( RightStickX, MoveIdle );
    XB_ControlAction( RightStickY, MoveIdle );
}

/*
 ********************************************************************************
 * XB_EnableCalibration
 ********************************************************************************
 */
void XB_EnableCalibration( uint8_t state )
{
    calibrated = state > 0;
}

/*
 ********************************************************************************
 * XB_SetIsWireless
 ********************************************************************************
 */
void XB_SetIsWireless( uint8_t state )
{
    isWireless = state > 0;
    XB_ResetButtons( );
    XB_ResetControls( );
}
