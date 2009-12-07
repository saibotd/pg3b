/*
 ********************************************************************************
 * Copyright (c) 2009 Richard Burke
 * All rights reserved.
 *
 * This module handles the USB interface, overall system configuration and
 * initialization. The scheduler TASK_LIST is defined here and scheduling is
 * started at the end of the initialization sequence.
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
 * $Rev: 227 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-27 21:37:10 -0500 (Fri, 27 Nov 2009) $
 ********************************************************************************
 */
#include <avr/io.h>
#include <avr/wdt.h>
#include <avr/power.h>
#include <avr/pgmspace.h>
#include <avr/interrupt.h>
#include <util/delay.h>

#include "usb_serial.h"
#include "RingBuffer.h"
#include "XBox360Controller.h"
#include "KeyboardDriver.h"
#include "MouseDriver.h"
#include "XBoxDriver.h"
#include "UsbDriver.h"
#include "EventAction.h"
#include "System.h"
#include "Profile.h"

/*
 ********************************************************************************
 * Local Functions and Variables
 ********************************************************************************
 */
static void ExecuteCommand( uint8_t *buffer, uint8_t size );
static uint8_t ReceiveString( uint8_t *buffer, uint8_t size );
static void DoWhileIdle( void );
void main( void ) __attribute__ ((noreturn));

#define CPU_PRESCALE(n) (CLKPR = 0x80, CLKPR = (n))

/*
 ********************************************************************************
 * DoWhileIdle
 ********************************************************************************
 */
static void DoWhileIdle( void )
{
    KB_EventTask( );
    MS_EventTask( );
}

/*
 ********************************************************************************
 * main
 ********************************************************************************
 */
void main( void )
{
    uint8_t buffer[128];

    /* Disable watchdog if enabled by bootloader/fuses */
    MCUSR &= ~(1 << WDRF);
    wdt_disable();

    /* Disable clock division */
    CPU_PRESCALE(0);
    SYS_Init( );

    /* Initialize Keyboard Driver */
    KB_Init();

    /* Initialize Mouse Driver */
    MS_Init();

    /* Initialize XBox Driver */
    XB_Init( );

    sei( );

    usb_init( );
    while( ! usb_configured( ) ) 
        DoWhileIdle( );
    _delay_ms( 1000 );

    while( 1 )
    {
        // wait for the user to run their terminal emulator program
        // which sets DTR to indicate it is ready to receive.
        while ( ! ( usb_serial_get_control( ) & USB_SERIAL_DTR ) )
            DoWhileIdle( );

        // discard anything that was received prior.  Sometimes the
        // operating system or other software will send a modem
        // "AT command", which can still be buffered.
        usb_serial_flush_input();

        // Listen for commands and process them
        while( 1 )
        {
            uint8_t size = ReceiveString( buffer, sizeof( buffer ) );
            if( size == 255 ) break;
            ExecuteCommand( buffer, size );
        }
    }
}

/*
 ****************************************************************************************************
 * ReceiveString
 *
 * Receive a string from the USB serial port.  The string is stored
 * in the buffer and this function will not exceed the buffer size.
 * A carriage return or newline completes the string, and is not
 * stored into the buffer.
 *
 * The return value is the number of characters received, or 255 if
 * the virtual serial connection was closed while waiting.
 ****************************************************************************************************
 */
static uint8_t ReceiveString( uint8_t *buffer, uint8_t size )
{
    int16_t r;
    uint8_t count = 0;

    while( count < size )
    {
        while( usb_serial_available( ) == 0 )
            DoWhileIdle( );
        r = usb_serial_getchar( );
        if( r == -1 )
        {
            if( ! usb_configured( ) || ! ( usb_serial_get_control( ) & USB_SERIAL_DTR ) )
                return 255; // user no longer connected
        }
        else
        {
            if( r == '\r' || r == '\n')
                return count;
            if ( r >= ' ' && r <= '~' )
            {
                *buffer++ = r;
                count++;
            }
        }
    }

    return count;
}

/*
 ****************************************************************************************************
 * Hexadecimal to Binary Convertion Functions (no error checking ... pass in a valid string or
 * experience bad mojo.
 ****************************************************************************************************
 */
static inline uint8_t nibble( uint8_t *digits )
{
    if( *digits > '9' )
        return *digits - 'A' + 10;

    return *digits - '0';
}

static inline uint8_t byte( uint8_t *digits  )
{
    return ( nibble( digits ) << 4 ) + nibble( digits + 1 );
}

static inline uint16_t word( uint8_t *digits )
{
    return ( byte( digits ) << 8 ) + byte( digits + 2 );
}

/*
 ****************************************************************************************************
 * Reads a command from the Rx_Buffer of the form: X [SSSS] [A|E] [VVVV]\r where,
 *       X: Offset 0: Literal 'X' character meaning eXecute command;
 *    SSSS: Offset 2: Sequence identifier with four ASCII characters. It must have 4 characters.
 *   A | E: Offset 7: Literal 'A' for action code, or literal 'E' for event code.
 *    VVVV: Offset 9: EVA action or event code (0000 - FFFF). It must have 4 digits.
 *      \r: Offset 13: Literal ASCII CR 0x0D (13)
 *  Filler: Offsets 1, 6, 8: Any ASCII character but spaces are more readable.
 * Example: X 5A5A A 000D\r
 *
 * Acknowledge is sent in response: X [SSSS] OK\r
 *       X: Offset 0: Literal 'X' character meaning eXecute command;
 *    SSSS: Offset 2: Sequence identifier with four ASCII characters. Whatever was given in the command.
 *    OK\r: Offset 7: Literal "OK\r".
 * Example: X 5A5A OK\r
 *
 * Acknowledgement means the command was received. It does not mean the command completed successfully.
 * The command may invoke a sequence of actions including delays and timers. It is up to the action
 * sequence to report state and completion codes.
 ****************************************************************************************************
 */
static void ExecuteCommand( uint8_t *buffer, uint8_t size )
{
    uint8_t command = *( buffer + 7 );
    uint16_t word0 = word( buffer + 9 );
    uint8_t byte0 = byte( buffer + 9 );

    if( buffer[0] != 'X' )
        return;

    buffer[7] = 'O';
    buffer[8] = 'K';
    usb_serial_write( buffer, 9 );
    switch( command )
    {
        case 'A': EVA_PerformAction( word0 ); break;
        case 'C': XB_EnableCalibration( byte0 ); break;
        case 'D': SYS_EnableLogging( byte0 ); break;
        case 'E': EVA_InvokeEventAction( word0 ); break;
        case 'G': XB_SetIsWireless( byte0 ); break;
        case 'P': PROF_Initialize( ); break;
        case 'Q': PROF_Finalize( ); break;
        case 'R': SYS_ReadPage( byte0 ); break;
        case 'W': SYS_WritePage( byte0, buffer + 11 ); break;
    }
    usb_serial_putchar( '\n' );
}
