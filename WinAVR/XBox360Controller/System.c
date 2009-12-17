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
 * $Rev$
 * $Author$
 * $LastChangedDate$
 ********************************************************************************
 */
#include <avr/io.h>
#include <avr/pgmspace.h>
#include <avr/eeprom.h>
#include <string.h>
#include "usb_serial.h"
#include "System.h"

/*
 ********************************************************************************
 * Local Variables
 ********************************************************************************
 */
static sys_config_t config;
static uint8_t magic[] = EEPROM_MAGIC_NUMBER;
static uint8_t logging;

/*
 ********************************************************************************
 * Private Functions
 ********************************************************************************
 */
static uint8_t CalculateCrc( uint8_t *data, uint8_t size );
static uint8_t IsConfigValid( void );
static uint8_t UInt4ToHex( uint8_t data );
static void InitSystemConfig( void );

static inline uint8_t HexToUInt4( uint8_t *digits );
static inline uint8_t HexToUInt8( uint8_t *digits  );
static inline uint16_t HexToUInt16( uint8_t *digits );

static uint8_t crctable[] PROGMEM = {
    // x^8 + x^2 + x^1 + x^0
    0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15,
    0x38, 0x3F, 0x36, 0x31, 0x24, 0x23, 0x2A, 0x2D,
    0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65,
    0x48, 0x4F, 0x46, 0x41, 0x54, 0x53, 0x5A, 0x5D,
    0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB, 0xF2, 0xF5,
    0xD8, 0xDF, 0xD6, 0xD1, 0xC4, 0xC3, 0xCA, 0xCD,
    0x90, 0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85,
    0xA8, 0xAF, 0xA6, 0xA1, 0xB4, 0xB3, 0xBA, 0xBD,
    0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5, 0xD2,
    0xFF, 0xF8, 0xF1, 0xF6, 0xE3, 0xE4, 0xED, 0xEA,
    0xB7, 0xB0, 0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2,
    0x8F, 0x88, 0x81, 0x86, 0x93, 0x94, 0x9D, 0x9A,
    0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32,
    0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A,
    0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42,
    0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D, 0x7A,
    0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B, 0x9C,
    0xB1, 0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4,
    0xF9, 0xFE, 0xF7, 0xF0, 0xE5, 0xE2, 0xEB, 0xEC,
    0xC1, 0xC6, 0xCF, 0xC8, 0xDD, 0xDA, 0xD3, 0xD4,
    0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C,
    0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44,
    0x19, 0x1E, 0x17, 0x10, 0x05, 0x02, 0x0B, 0x0C,
    0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34,
    0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B,
    0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63,
    0x3E, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B,
    0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13,
    0xAE, 0xA9, 0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB,
    0x96, 0x91, 0x98, 0x9F, 0x8A, 0x8D, 0x84, 0x83,
    0xDE, 0xD9, 0xD0, 0xD7, 0xC2, 0xC5, 0xCC, 0xCB,
    0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4, 0xF3
};

/*
 ****************************************************************************************************
 * SYS_Init
 ****************************************************************************************************
 */
void SYS_Init( void )
{
    InitSystemConfig( );
}

/*
 ****************************************************************************************************
 * SYS_ControllerModel
 ****************************************************************************************************
 */
uint8_t SYS_ControllerModel( void )
{
    return config.model;
}

/*
 ****************************************************************************************************
 * SYS_CalibratedValue
 ****************************************************************************************************
 */
uint8_t SYS_CalibratedValue( uint8_t xboxTarget, uint8_t rawValue )
{
    if( config.calibration & ( 1 << xboxTarget ) )
        return eeprom_read_byte( (void *)( ( xboxTarget << EEPROM_CALIBRATION_BITS ) + rawValue ) );

    return rawValue;
}

/*
 ****************************************************************************************************
 * SYS_LogByte
 ****************************************************************************************************
 */
uint8_t SYS_LogByte( uint8_t prefix, uint8_t data )
{
    if( logging )
    {
        usb_serial_putchar( prefix );
        usb_serial_putchar( ' ' );
        usb_serial_putchar( UInt4ToHex( data >> 4 ) );
        usb_serial_putchar( UInt4ToHex( data ) );
        usb_serial_putchar( '\n' );
    }

    return data;
}

/*
 ****************************************************************************************************
 * SYS_LogWord
 ****************************************************************************************************
 */
uint16_t SYS_LogWord( uint8_t prefix, uint16_t data )
{
    if( logging )
    {
        usb_serial_putchar( prefix );
        usb_serial_putchar( ' ' );
        usb_serial_putchar( UInt4ToHex( data >> 12 ) );
        usb_serial_putchar( UInt4ToHex( data >> 8 ) );
        usb_serial_putchar( UInt4ToHex( data >> 4 ) );
        usb_serial_putchar( UInt4ToHex( data ) );
        usb_serial_putchar( '\n' );
    }

    return data;
}

/*
 ********************************************************************************
 * SYS_ReadPage
 ********************************************************************************
 */
void SYS_ReadPage( uint8_t page )
{
    uint8_t crc8 = 0xff;

    usb_serial_putchar( ' ' );
    for( uint16_t address = (uint16_t)page << 5; address < ( (uint16_t)page << 5 ) + EEPROM_PAGE_SIZE; address++ )
    {
        uint8_t byte = eeprom_read_byte( (void *)address );
        crc8 = pgm_read_byte( &crctable[crc8 ^ byte] );
        usb_serial_putchar( UInt4ToHex( byte >> 4 ) );
        usb_serial_putchar( UInt4ToHex( byte ) );
    }
    usb_serial_putchar( UInt4ToHex( crc8 >> 4 ) );
    usb_serial_putchar( UInt4ToHex( crc8 ) );
}

/*
 ********************************************************************************
 * SYS_WritePage
 ********************************************************************************
 */
void SYS_WritePage( uint8_t page, uint8_t *buffer )
{
//  static char error[] = "CRC FAILED";
    uint8_t crc8 = 0xff;
    
    for( uint16_t address = (uint16_t)page << 5; address < ( (uint16_t)page << 5 ) + EEPROM_PAGE_SIZE; address++, buffer += 2 )
    {
        uint8_t byte = HexToUInt8( buffer );
        crc8 = pgm_read_byte( &crctable[crc8 ^ byte] );
        eeprom_write_byte( (void *)address, byte );
    }
//  if( crc8 - HexToUInt8( buffer ) > 0 )
//      usb_serial_write( (void *)error, sizeof( error ) - 1 );
}

/*
 ********************************************************************************
 * SYS_EnableLogging
 *
 * This function sets or clears the datacollection flag. When enabled, debug
 * events are reported to the USB port.
 ********************************************************************************
 */
void SYS_EnableLogging( uint8_t value )
{
    logging = value > 0;
}

/*
 ********************************************************************************
 * SYS_IsLoggingEnabled
 *
 * This function sets or clears the datacollection flag. When enabled, debug
 * events are reported to the USB port.
 ********************************************************************************
 */
uint8_t SYS_IsLoggingEnabled( void )
{
    return logging;
}

/*
 ********************************************************************************
 * CalculateCrc
 ********************************************************************************
 */
static uint8_t CalculateCrc( uint8_t *data, uint8_t size )
{
    uint8_t crc8 = 0xFF;
    for( uint8_t i = 0; i < size; i++ )
        crc8 = pgm_read_byte( &crctable[crc8 ^ *data++] );

    return crc8;
}

/*
 ****************************************************************************************************
 * IsConfigValid
 ****************************************************************************************************
 */
static uint8_t IsConfigValid( void )
{
    if( strncmp( (void *)config.magic, (void *)magic, sizeof( config.magic ) ) )
        return 0;
    if( CalculateCrc( config.magic, sizeof( sys_config_t ) - 1 ) - config.crc )
        return 0;

    return 1;
}

/*
 ****************************************************************************************************
 * InitSystemConfig
 ****************************************************************************************************
 */
static void InitSystemConfig( void )
{
    eeprom_read_block( &config, 0, sizeof( sys_config_t ) );
    if( ! IsConfigValid( ) )
    {
        memcpy( config.magic, magic, sizeof( config.magic ) );
        config.size = sizeof( sys_config_t );
        config.version = EEPROM_CONTENTS_VERSION;
        config.model = 0;
        config.calibration = 0;
        config.crc = CalculateCrc( config.magic, sizeof( sys_config_t ) - 1 );
        eeprom_write_block( &config, 0, sizeof( config ) );
    }
}

/*
 ****************************************************************************************************
 * UInt4ToHex
 ****************************************************************************************************
 */
static uint8_t UInt4ToHex( uint8_t data )
{
    data &= 0x0f;
    return data + ( ( data < 10 )? '0' : 'A' - 10 );
}

/*
 ****************************************************************************************************
 * Hexadecimal to Binary Convertion Functions (no error checking ... pass in a valid string or
 * experience bad mojo.
 ****************************************************************************************************
 */
static inline uint8_t HexToUInt4( uint8_t *digits )
{
    if( *digits > '9' )
        return *digits - 'A' + 10;

    return *digits - '0';
}

static inline uint8_t HexToUInt8( uint8_t *digits  )
{
    return ( HexToUInt4( digits ) << 4 ) + HexToUInt4( digits + 1 );
}

static inline uint16_t HexToUInt16( uint8_t *digits )
{
    return ( HexToUInt8( digits ) << 8 ) + HexToUInt8( digits + 2 );
}
