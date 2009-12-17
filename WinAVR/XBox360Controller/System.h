/*
 ********************************************************************************
 * System.h
 *
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
#ifndef __SYSTEM_H__
#define __SYSTEM_H__
 
#define EEPROM_CALIBRATION_SIZE     (1<<EEPROM_CALIBRATION_BITS)
#define EEPROM_CALIBRATION_BITS     8
#define EEPROM_PAGE_BITS            5
#define EEPROM_PAGE_SIZE            (1<<EEPROM_PAGE_BITS)
#define EEPROM_MAGIC_SIZE           4
#define EEPROM_MAGIC_NUMBER         { 'P', 'G', '3', 'B' }
#define EEPROM_CONTENTS_VERSION     1

typedef struct _contents
{
    uint8_t crc;
    uint8_t magic[EEPROM_MAGIC_SIZE];
    uint8_t size;
    uint8_t version;
    uint8_t model;
    uint8_t calibration;
} sys_config_t;

extern void SYS_Init( void );
extern void SYS_EnableLogging( uint8_t value );
extern void SYS_ReadPage( uint8_t page );
extern void SYS_WritePage( uint8_t page, uint8_t *buffer );
extern uint8_t SYS_IsLoggingEnabled( void );
extern uint8_t SYS_CalibratedValue( uint8_t xboxTarget, uint8_t rawValue );
extern uint8_t SYS_ControllerModel( void );
extern uint8_t SYS_LogByte( uint8_t prefix, uint8_t data );
extern uint16_t SYS_LogWord( uint8_t prefix, uint16_t data );

#endif
