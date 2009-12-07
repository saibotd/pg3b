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
 * $Rev: 224 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-27 01:34:22 -0500 (Fri, 27 Nov 2009) $
 ********************************************************************************
 */
using System;
using System.Text;
using System.IO.Ports;
using System.Collections.Generic;
using Microsoft.Xna.Framework.Input;

namespace PG3B.Interface
{
    public enum XBoxButtonState { Pressed, Released }

    public enum Device_t { Null, Keyboard, Mouse, Usb, XBox, Arcade, Joystick, Script };
    public enum XBTarget_t { LeftStickX, LeftStickY, RightStickX, RightStickY, LeftTrigger, RightTrigger, XKeyPress, XKeyRelease };
    public enum XBButton_t { AButton, BButton, XButton, YButton, UpPad, DownPad, LeftPad, RightPad, LeftShoulderButton, RightShoulderButton, LeftStickButton, RightStickButton, StartButton, GuideButton, BackButton };
    public enum XBGamePad_t { WiredCommonLineV1, WirelessCommonGroundV1 };

    public class XBoxController
    {
        private XBoxConnection xboxConnection;
        private XBoxButtons xboxButtons;
        private XBoxDPad xboxDPad;
        private XBoxTriggers xboxTriggers;
        private XBoxThumbSticks xboxThumbSticks;
        private XBoxControllerConfiguration xboxControllerConfig;
        private bool calibrationEnabled;
        private bool isWireless;

        public bool IsConnected { get { return xboxConnection.IsConnected; } }
        public int PacketNumber { get { return xboxConnection.PacketNumber; } }
        public bool CalibrationEnabled { get { return calibrationEnabled; } set { calibrationEnabled = value; xboxConnection.SetCalibration(value); } }
        public bool IsWireless { get { return isWireless; } set { isWireless = value; xboxConnection.SetGamePadIsWireless(value); } }

        public XBoxButtons Buttons { get { return xboxButtons; } }
        public XBoxDPad DPad { get { return xboxDPad; } }
        public XBoxTriggers Triggers { get { return xboxTriggers; } }
        public XBoxThumbSticks ThumbSticks { get { return xboxThumbSticks; } }
        public XBoxControllerConfiguration Configuration { get { return xboxControllerConfig; } }

        public XBoxController(string portName)
        {
            xboxConnection = new XBoxConnection(portName);
            xboxButtons = new XBoxButtons(xboxConnection);
            xboxDPad = new XBoxDPad(xboxConnection);
            xboxTriggers = new XBoxTriggers(xboxConnection);
            xboxThumbSticks = new XBoxThumbSticks(xboxConnection);
            xboxControllerConfig = new XBoxControllerConfiguration(xboxConnection);
            CalibrationEnabled = false;
            IsWireless = false;
        }
    }

    public class XBoxButtons
    {
        private XBoxConnection xboxConnection;

        public XBoxButtonState A { set { xboxConnection.SetButton(XBButton_t.AButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState B { set { xboxConnection.SetButton(XBButton_t.BButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState X { set { xboxConnection.SetButton(XBButton_t.XButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Y { set { xboxConnection.SetButton(XBButton_t.YButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Back { set { xboxConnection.SetButton(XBButton_t.BackButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Start { set { xboxConnection.SetButton(XBButton_t.StartButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState BigButton { set { xboxConnection.SetButton(XBButton_t.GuideButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState LeftShoulder { set { xboxConnection.SetButton(XBButton_t.LeftShoulderButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState RightShoulder { set { xboxConnection.SetButton(XBButton_t.RightShoulderButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState LeftStick { set { xboxConnection.SetButton(XBButton_t.LeftStickButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState RightStick { set { xboxConnection.SetButton(XBButton_t.RightStickButton, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }

        public XBoxButtons(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
        }
    }

    public class XBoxDPad
    {
        private XBoxConnection xboxConnection;

        public XBoxButtonState Up { set { xboxConnection.SetButton(XBButton_t.UpPad, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Down { set { xboxConnection.SetButton(XBButton_t.DownPad, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Left { set { xboxConnection.SetButton(XBButton_t.LeftPad, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }
        public XBoxButtonState Right { set { xboxConnection.SetButton(XBButton_t.RightPad, value == XBoxButtonState.Pressed ? XBTarget_t.XKeyPress : XBTarget_t.XKeyRelease); } }

        public XBoxDPad(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
        }
    }

    public class XBoxTriggers
    {
        private XBoxConnection xboxConnection;

        public float Left { set {  xboxConnection.SetTrigger(XBTarget_t.LeftTrigger, value); } }
        public float Right { set { xboxConnection.SetTrigger(XBTarget_t.RightTrigger, value); } }

        public XBoxTriggers(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
        }
    }

    public class XBoxThumbSticks
    {
        private XBoxConnection xboxConnection;
        private XBoxVector2 leftThumbStick;
        private XBoxVector2 rightThumbStick;

        public XBoxVector2 Left { get { return leftThumbStick; } }
        public XBoxVector2 Right { get { return rightThumbStick; } }

        public XBoxThumbSticks(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
            leftThumbStick = new XBoxVector2(xboxConnection, XBTarget_t.LeftStickX, XBTarget_t.LeftStickY);
            rightThumbStick = new XBoxVector2(xboxConnection, XBTarget_t.RightStickX, XBTarget_t.RightStickY);
        }
    }

    public class XBoxVector2
    {
        private XBoxConnection xboxConnection;
        private XBTarget_t targetCodeX;
        private XBTarget_t targetCodeY;

        public float X { set { xboxConnection.SetThumbStick(targetCodeX, value); } }
        public float Y { set { xboxConnection.SetThumbStick(targetCodeY, value); } }

        public XBoxVector2(XBoxConnection _xboxConnection, XBTarget_t _targetCodeX, XBTarget_t _targetCodeY)
        {
            xboxConnection = _xboxConnection;
            targetCodeX = _targetCodeX;
            targetCodeY = _targetCodeY;
        }
    }

    public class XBoxCalibration
    {
        private XBoxConnection xboxConnection;
        private const byte ConfigPage = 0;
        private const int PageBits = 5;
        private const int PageSize = (1<<PageBits);
        private const string MagicNumber = "PG3B";

        private const int CrcOffset = 0;
        private const int MagicOffset = 1;
        private const int SizeOffset = 5;
        private const int VersionOffset = 6;
        private const int ModelOffset = 7;
        private const int CalibrationOffset = 8;

        public byte[] LeftTrigger { set { UpdateCalibration(XBTarget_t.LeftTrigger, value); } }
        public byte[] RightTrigger { set { UpdateCalibration(XBTarget_t.RightTrigger, value); } }

        public XBoxCalibration(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
        }

        // Page 0 in the pg3b EEPROM is the system configuration record. It stores the state of
        // the calibration record for each analog control (on/off), and an enum representing the
        // type of controller.
        //
        // Byte 0: 8 Bit CRC
        //  1 - 4: magic number "PG3B"
        //      5: record size
        //      6: version
        //      7: model (XBoxGamePad_t), where 0 <= XBoxGamePad_t <= 1.
        //      8: calibration (bitfield 2^XBoxTarget_t), where 0 <= XBoxTarget_t <= 7.

        public byte[] GetConfigPage()
        {
            byte[] xboxConfiguration = xboxConnection.ReadPage(ConfigPage);
            string magicNumber = System.Text.Encoding.ASCII.GetString(xboxConfiguration, MagicOffset, MagicNumber.Length);
            if (magicNumber != MagicNumber)
                throw new System.IO.InvalidDataException("XBoxCalibration.GetConfigPage failed to validate magic number.");
            if (XBoxConnection.CalculateCrc(xboxConfiguration, CrcOffset + 1, xboxConfiguration[SizeOffset] - 1) != xboxConfiguration[CrcOffset])
                throw new System.IO.InvalidDataException("XBoxCalibration.GetConfigPage failed to validate the CRC.");

            return xboxConfiguration;
        }

        // NOTE: The concept of a "page" is used to simplify the implementation of pg3b <-> PC comms
        // and as a unit of reference for statically assigned blocks of EEPROM data.
        // Write the calibration data to consecutive pages. Pages are nominally 32 bytes each. A
        // calibration record is 256 bytes, or 8 pages.
        // 
        // Page 1: LeftStickX
        //      9: LeftStickY
        //     17: RightStickX
        //     25: RightStickY
        //     33: LeftTrigger
        //     41: RightTrigger

        public void UpdateCalibration(XBTarget_t xboxTarget, byte[] calibrationData)
        {
            byte[] pageData = new byte[PageSize];
            int pagesPerRecord = calibrationData.Length / PageSize;
            int firstPage = ((int)xboxTarget * pagesPerRecord) + 1;
            for( int pageCount = 0; pageCount < calibrationData.Length / PageSize; pageCount++)
            {
                for (int pageOffset = 0; pageOffset < PageSize; pageOffset++)
                    pageData[pageOffset] = calibrationData[pageCount * PageSize + pageOffset];
                xboxConnection.WritePage((byte)(firstPage + pageCount), pageData);
            }

            byte[] xboxConfiguration = GetConfigPage();
            xboxConfiguration[CalibrationOffset] |= (byte)(1 << (int)xboxTarget);
            xboxConnection.WritePage(ConfigPage, xboxConfiguration);
        }
    }

    public class XBoxControllerConfiguration
    {
        private XBoxConnection xboxConnection;
        private XBoxCalibration xboxCalibration;

        public XBoxCalibration Calibration { get { return xboxCalibration; } }

        public XBoxControllerConfiguration(XBoxConnection _xboxConnection)
        {
            xboxConnection = _xboxConnection;
            xboxCalibration = new XBoxCalibration(xboxConnection);
        }
    }

    public class XBoxConnection
    {
        private SerialPort serialPort;
        private Int16 sequenceNumber;
        private static byte[] crcTable;

        public bool IsConnected { get { return PingController(); } }
        public int PacketNumber { get { return sequenceNumber; } }

        static XBoxConnection()
        {
            XBoxConnection.crcTable = new byte[] {
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
        }

        public static byte CalculateCrc(byte[] data, int start, int size)
        {

            byte crc8 = 0xFF;
            for (int i = start; i < start + size; i++)
                crc8 = XBoxConnection.crcTable[crc8 ^ data[i]];

            return crc8;
        }

        public XBoxConnection(string portName)
        {
            try
            {
                OpenSerialPort(portName);
            }
            catch
            {
                CloseSerialPort();
            }
        }

        private void OpenSerialPort(string portName)
        {
            OpenSerialPort(portName, 100);
        }

        private void OpenSerialPort(string portName, int timeout)
        {
            serialPort = new SerialPort(portName, 230400, Parity.None, 8, StopBits.One);
            serialPort.Handshake = Handshake.None;
            serialPort.WriteTimeout = timeout;
            serialPort.ReadTimeout = timeout;
            serialPort.NewLine = "\n";
            serialPort.Open();
            serialPort.DtrEnable = true;
        }

        private void CloseSerialPort()
        {
            if (serialPort != null)
                serialPort.Close();
            serialPort = null;
        }

        private bool PingController()
        {
            bool reply = false;

            try
            {
                if (serialPort != null && serialPort.IsOpen)
                {
                    Command('D', BitConverter.GetBytes(false));
                    reply = true;
                }
            }
            catch
            {
            }

            return reply;
        }

        private byte[] Command(char commandCode, byte[] commandArgument)
        {
            string argumentString = ByteToString(commandArgument);
            string actionString = String.Format("X {0:X4} {1} {2}\r", sequenceNumber, commandCode, argumentString);
            string reponsePrefix = String.Format("X {0:X4} OK", sequenceNumber++);
            string responseString = Primitive(actionString, reponsePrefix);

            return StringToByte(responseString.Substring(reponsePrefix.Length).Trim());
        }

        private string Primitive(string actionString, string reponsePrefix)
        {
            serialPort.DiscardInBuffer();

            byte[] actionBytes = Encoding.ASCII.GetBytes(actionString);
            serialPort.Write(actionBytes, 0, actionBytes.Length);

            string responseString;
            do
            {
                responseString = serialPort.ReadLine();
            } while (! responseString.StartsWith(reponsePrefix));

            return responseString;
        }

        private Int16 EVA_NewAction(Int16 k, Int16 v)
        {
            return ((Int16)((k)|(v)));
        }

        private Int16 EVA_NewActionKey(Int16 d,Int16 t)
        {
            return ((Int16)(((d)<<12)|((t)<<8)));
        }

        private Int16 EVA_NewActionValue(Int16 v)
        {
            return ((Int16)(v));
        }

        public string ByteToString(byte[] byteArray)
        {
            char[] byteToString = new char[byteArray.Length * 2];
            for (int byteOffset = 0, charOffset = 0; byteOffset < byteArray.Length; byteOffset++, charOffset += 2)
            {
                byte dataByte = ((byte)(byteArray[byteOffset] >> 4));
                byteToString[charOffset] = (char)(dataByte > 9 ? dataByte + 0x37 : dataByte + 0x30);
                dataByte = ((byte)(byteArray[byteOffset] & 0x0F));
                byteToString[charOffset + 1] = (char)(dataByte > 9 ? dataByte + 0x37 : dataByte + 0x30);
            }

            return new string(byteToString);
        }

        public byte[] StringToByte(string hexString)
        {
            if ((hexString.Length & 1) > 0)
                hexString += "0";
            byte[] stringToByte = new byte[hexString.Length / 2];
            for (int i = 0; i < hexString.Length; i += 2)
                stringToByte[i / 2] = byte.Parse(hexString.Substring(i, 2), System.Globalization.NumberStyles.HexNumber);

            return stringToByte;
        }

        private byte[] ReverseBytes(byte[] byteArray)
        {
            Array.Reverse(byteArray);
            return byteArray;
        }
        
        public void SetButton(XBButton_t buttonCode, XBTarget_t buttonState)
        {
            Int16 actionKey = EVA_NewActionKey((Int16)Device_t.XBox, (Int16)buttonState);
            Int16 actionCode = EVA_NewAction((Int16)actionKey, (Int16)buttonCode);

            // 'A'ction <Int16>
            Command('A', ReverseBytes(BitConverter.GetBytes(actionCode)));
        }

        public void SetTrigger(XBTarget_t targetCode, float triggerState)
        {
            float wiperState = triggerState * byte.MaxValue;
            wiperState = (float)byte.MaxValue - wiperState;
            wiperState = Math.Max(wiperState, (float)byte.MinValue);
            wiperState = Math.Min(wiperState, (float)byte.MaxValue);

            Int16 wiperValue = Convert.ToInt16(wiperState);
            Int16 actionKey = EVA_NewActionKey((Int16)Device_t.XBox, (Int16)targetCode);
            Int16 actionCode = EVA_NewAction((Int16)actionKey, (Int16)wiperValue);

            // 'A'ction <Int16>
            Command('A', ReverseBytes(BitConverter.GetBytes(actionCode)));
        }

        public void SetThumbStick(XBTarget_t targetCode, float targetState)
        {
            float wiperState = (targetState + 1.0F) / 2 * (float)byte.MaxValue;
            if (targetCode == XBTarget_t.LeftStickX || targetCode == XBTarget_t.RightStickX)
                wiperState = (float)byte.MaxValue - wiperState;
            wiperState = Math.Max(wiperState, (float)byte.MinValue);
            wiperState = Math.Min(wiperState, (float)byte.MaxValue);

            Int16 wiperValue = Convert.ToInt16(wiperState);
            Int16 actionKey = EVA_NewActionKey((Int16)Device_t.XBox, (Int16)targetCode);
            Int16 actionCode = EVA_NewAction((Int16)actionKey, (Int16)wiperValue);

            // 'A'ction <Int16>
            Command('A', ReverseBytes(BitConverter.GetBytes(actionCode)));
        }

        public void SetCalibration(bool calibrationEnabled)
        {
            // 'C'alibration Enabled <boolean>
            Command('C', ReverseBytes(BitConverter.GetBytes(calibrationEnabled)));
        }

        public void SetGamePadIsWireless(bool isWireless)
        {
            // 'G'amePad IsWireless <boolean>
            Command('G', ReverseBytes(BitConverter.GetBytes(isWireless)));
        }

        public byte[] WritePage(byte pageNumber, byte[] pageData)
        {
            byte[] writePage = new byte[pageData.Length+2];
            writePage[0] = pageNumber;
            pageData.CopyTo(writePage, 1);
            writePage[writePage.Length - 1] = XBoxConnection.CalculateCrc(writePage, 0, pageData.Length - 1);

            // 'W'rite <byte[0..63]>
            Command('W', writePage);


            // 'R'ead <byte[0..63]>
            byte[] verifyPage = Command('R', new byte[] { pageNumber });
            if (XBoxConnection.CalculateCrc(verifyPage, 0, verifyPage.Length - 1) == verifyPage[verifyPage.Length - 1])
                Array.Resize<byte>(ref verifyPage, verifyPage.Length - 1);
            else
                throw new System.IO.InvalidDataException("XBoxConnection.WritePage Failed CRC Check.");

            for (int i = 0; i < pageData.Length; i++)
                if (pageData[i] != verifyPage[i])
                    throw new System.IO.InvalidDataException("XBoxConnection.WritePage Failed to Verify Page."); ;

            return verifyPage;
        }

        public byte[] ReadPage(byte pageNumber)
        {
            // 'R'ead <byte[0..63]>
            byte[] pageData = Command('R', new byte[] { pageNumber });
            if (XBoxConnection.CalculateCrc(pageData, 0, pageData.Length - 1) == pageData[pageData.Length - 1])
                Array.Resize<byte>(ref pageData, pageData.Length - 1);
            else
                throw new System.IO.InvalidDataException("XBoxConnection.ReadPage Failed CRC Check.");

            return pageData;
        }
    }
}
