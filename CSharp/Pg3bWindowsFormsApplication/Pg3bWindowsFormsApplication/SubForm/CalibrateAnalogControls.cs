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
 * $Rev: 204 $
 * $Author: rburke $
 * $LastChangedDate: 2009-11-19 00:48:00 -0500 (Thu, 19 Nov 2009) $
 ********************************************************************************
 */
using System;
using System.Drawing;
using System.Threading;
using System.Windows.Forms;
using PG3B.Interface;
using ZedGraph;
using System.IO;

namespace Pg3bWindowsFormsApplication.SubForm
{
    public partial class CalibrateAnalogControls : Form
    {
        delegate void SetTrigger(float v);
        delegate float GetTrigger();

        delegate void SetThumbStick(float v);
        delegate float GetThumbStick();

        XBoxController xboxController;
        GamepadController gamepadController;

        public CalibrateAnalogControls()
        {
            InitializeComponent();
        }

        private void GraphAnalogControl_Load(object sender, EventArgs e)
        {
            MainForm mainForm = Parent.Parent as MainForm;
            xboxController = mainForm.SharedXBoxController();
            gamepadController = mainForm.SharedGamepadController();
        }


        private void clearButton_Click(object sender, EventArgs e)
        {
            zedGraphControl.GraphPane.CurveList.Clear();
            zedGraphControl.AxisChange();
            zedGraphControl.Invalidate();
        }

        private void stopButton_Click(object sender, EventArgs e)
        {
            stopButton.Enabled = false;
        }

        private void graphTrigger(string titleText, SetTrigger setTrigger, GetTrigger getTrigger, byte[] calibrationTable)
        {
            zedGraphControl.GraphPane.CurveList.Clear();
            GraphPane graphPane = zedGraphControl.GraphPane;
            graphPane.Title.Text = titleText;
            graphPane.XAxis.Title.Text = "Wiper Value";
            graphPane.XAxis.Scale.Min = 0;
            graphPane.XAxis.Scale.Max = 255;
            graphPane.YAxis.Title.Text = "Trigger Value";
            graphPane.YAxis.Scale.Min = 0.0;
            graphPane.YAxis.Scale.Max = 1.0;

            PointPairList targetValues = new PointPairList();
            PointPairList actualValues = new PointPairList();
            LineItem targetCurve = graphPane.AddCurve("Target", targetValues, Color.Red, SymbolType.None);
            LineItem actualCurve = graphPane.AddCurve("Actual", actualValues, Color.Blue, SymbolType.None);

            for (int wiper = 0; wiper <= byte.MaxValue && stopButton.Enabled; wiper++)
            {
                float deflection = (float)wiper / (float)byte.MaxValue;
                float calibratedDeflection = calibrationTable == null? deflection : (float)calibrationTable[wiper] / (float)byte.MaxValue;
                setTrigger(calibratedDeflection);
                Application.DoEvents();
                Thread.Sleep(16);

                IPointListEdit targetList = targetCurve.Points as IPointListEdit;
                targetList.Add(wiper, deflection);

                IPointListEdit actualList = actualCurve.Points as IPointListEdit;
                float actualTrigger = getTrigger();
                actualList.Add(wiper, actualTrigger);

                zedGraphControl.AxisChange();
                zedGraphControl.Invalidate();
            }
            setTrigger(0.0F);
        }

        private void graphThumbStick(string titleText, SetThumbStick setThumbStick, GetThumbStick getThumbStick, byte[] calibrationTable)
        {
            zedGraphControl.GraphPane.CurveList.Clear();
            GraphPane graphPane = zedGraphControl.GraphPane;
            graphPane.Title.Text = titleText;
            graphPane.XAxis.Title.Text = "Wiper Value";
            graphPane.XAxis.Scale.Min = 0;
            graphPane.XAxis.Scale.Max = 255;
            graphPane.YAxis.Title.Text = "Trigger Value";
            graphPane.YAxis.Scale.Min = -1.0;
            graphPane.YAxis.Scale.Max = 1.0;

            PointPairList targetValues = new PointPairList();
            PointPairList actualValues = new PointPairList();
            LineItem targetCurve = graphPane.AddCurve("Target", targetValues, Color.Red, SymbolType.None);
            LineItem actualCurve = graphPane.AddCurve("Actual", actualValues, Color.Blue, SymbolType.None);

            for (int wiper = 0; wiper <= byte.MaxValue && stopButton.Enabled; wiper++)
            {
                float deflection = (float)wiper / (float)byte.MaxValue * 2.0F - 1.0F;
                float calibratedDeflection = calibrationTable == null ? deflection : (float)calibrationTable[wiper] / (float)byte.MaxValue * 2.0F - 1.0F;
                setThumbStick(calibratedDeflection);
                Application.DoEvents();
                Thread.Sleep(16);

                IPointListEdit targetList = targetCurve.Points as IPointListEdit;
                targetList.Add(wiper, deflection);

                IPointListEdit actualList = actualCurve.Points as IPointListEdit;
                float actualThumbStick = getThumbStick();
                actualList.Add(wiper, actualThumbStick);

                zedGraphControl.AxisChange();
                zedGraphControl.Invalidate();
            }

            setThumbStick(0.0F);
        }

        private void graphButton_Click(object sender, EventArgs e)
        {
            stopButton.Enabled = true;
            calibrateControlComboBox.Enabled = false;
            calibrationEnabledComboBox.Enabled = false;
            graphButton.Enabled = false;
            calibrateButton.Enabled = false;

            xboxController.CalibrationEnabled = calibrationEnabledComboBox.Text == "Calibrated";

            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left Trigger") && stopButton.Enabled)
                graphTrigger("Left Trigger " + calibrationEnabledComboBox.Text, (f) => xboxController.Triggers.Left = f, () => gamepadController.Triggers.Left, null);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right Trigger") && stopButton.Enabled)
                graphTrigger("Right Trigger " + calibrationEnabledComboBox.Text, (f) => xboxController.Triggers.Right = f, () => gamepadController.Triggers.Right, null);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left ThumbStick X") && stopButton.Enabled)
                graphThumbStick("Left ThumbStick X " + calibrationEnabledComboBox.Text, (f) => xboxController.ThumbSticks.Left.X = f, () => gamepadController.ThumbSticks.Left.X, null);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left ThumbStick Y") && stopButton.Enabled)
                graphThumbStick("Left ThumbStick Y " + calibrationEnabledComboBox.Text, (f) => xboxController.ThumbSticks.Left.Y = f, () => gamepadController.ThumbSticks.Left.Y, null);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right ThumbStick X") && stopButton.Enabled)
                graphThumbStick("Right ThumbStick X " + calibrationEnabledComboBox.Text, (f) => xboxController.ThumbSticks.Right.X = f, () => gamepadController.ThumbSticks.Right.X, null);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right ThumbStick Y") && stopButton.Enabled)
                graphThumbStick("Right ThumbStick Y " + calibrationEnabledComboBox.Text, (f) => xboxController.ThumbSticks.Right.Y = f, () => gamepadController.ThumbSticks.Right.Y, null);

            calibrateButton.Enabled = true;
            graphButton.Enabled = true;
            calibrateControlComboBox.Enabled = true;
            calibrationEnabledComboBox.Enabled = true;
            stopButton.Enabled = false;

            xboxController.CalibrationEnabled = false;
        }

        private void calibrateThumbStick(string controlName, SetThumbStick setThumbStick, GetThumbStick getThumbStick, bool isInverted)
        {
            float[] actualValues = new float[byte.MaxValue + 1];
            byte[] calibrationTable = new byte[byte.MaxValue + 1];

            // Read the GamePad ThumbStick deflection for each wiper value
            for (int wiper = 0; wiper <= byte.MaxValue && stopButton.Enabled; wiper++)
            {
                float deflection = ((float)wiper / (float)byte.MaxValue) * 2.0F - 1.0F;
                setThumbStick(deflection);
                Application.DoEvents();
                Thread.Sleep(16);
                actualValues[wiper] = getThumbStick();
            }

            // Find value of array entry whose value is closest to zero
            float zero = float.PositiveInfinity;
            Array.ForEach<float>(actualValues, (f) => { zero = (Math.Abs(f) < Math.Abs(zero)) ? f : zero; });

            // Find index of first array entry whose value is closest to zero
            int firstZero = Array.FindIndex<float>(actualValues, (f) => (f == zero));
            if (firstZero == -1) firstZero = byte.MaxValue / 2;

            // Find index of last array entry whose value is closest to zero
            int lastZero = Array.FindLastIndex<float>(actualValues, (f) => (f == zero));
            if (lastZero == -1) lastZero = byte.MaxValue / 2;

            // Find value of array entry whose value is closest to 1.0F
            float plusOne = float.PositiveInfinity;
            Array.ForEach<float>(actualValues, (f) => { plusOne = (Math.Abs(f - 1.0F) < Math.Abs(plusOne - 1.0F)) ? f : plusOne; });

            // Find index of first array entry whose value is closest to 1.0F
            int firstPlusOne = Array.FindIndex<float>(actualValues, (f) => (f == plusOne));
            if (firstPlusOne == -1) firstPlusOne = byte.MaxValue;

            // Find value of array entry whose value is closest to -1.0F
            float minusOne = float.NegativeInfinity;
            Array.ForEach<float>(actualValues, (f) => { minusOne = (Math.Abs(f + 1.0F) < Math.Abs(minusOne + 1.0F)) ? f : minusOne; });

            // Find index of first array entry whose value is closest to -1.0F
            int lastMinusOne = Array.FindLastIndex<float>(actualValues, (f) => (f == minusOne));
            if (lastMinusOne == -1) lastMinusOne = byte.MinValue;

            for (int wiper = 0; wiper <= byte.MaxValue; wiper++)
            {
                float deflection = ((float)wiper / (float)byte.MaxValue) * 2.0F - 1.0F;
                int match = (firstZero + lastZero) / 2;
                for (int index = lastMinusOne; index <= firstPlusOne; index++)
                {
                    if (Math.Abs(actualValues[index] - deflection) < Math.Abs(actualValues[match] - deflection))
                        match = index;
                }
                calibrationTable[wiper] = (byte)match;
            }

            calibrationTable[byte.MinValue] = (byte)((lastMinusOne > byte.MinValue) ? lastMinusOne - 1 : byte.MinValue);
            calibrationTable[byte.MaxValue] = (byte)((firstPlusOne < byte.MaxValue) ? firstPlusOne + 1 : byte.MaxValue);
            calibrationTable[byte.MaxValue / 2 + 1] = (byte)((firstZero + lastZero) / 2);

            setThumbStick(0.0F);

            graphThumbStick(controlName + " Calibrated (Simulation)", setThumbStick, getThumbStick, calibrationTable);
            writeCalibrationTable(controlName, calibrationTable, isInverted);
        }

        private void calibrateTrigger(string controlName, SetTrigger setTrigger, GetTrigger getTrigger, bool isInverted)
        {
            float[] actualValues = new float[byte.MaxValue + 1];
            byte[] calibrationTable = new byte[byte.MaxValue + 1];

            // Read the GamePad Trigger deflection for each wiper value
            for (int wiper = 0; wiper <= byte.MaxValue && stopButton.Enabled; wiper++)
            {
                float deflection = (float)wiper / (float)byte.MaxValue;
                setTrigger(deflection);
                Application.DoEvents();
                Thread.Sleep(16);
                actualValues[wiper] = getTrigger();
            }

            // Find value array entry whose value is closest to zero
            float zero = float.PositiveInfinity;
            Array.ForEach<float>(actualValues, (f) => { zero = (Math.Abs(f) < Math.Abs(zero)) ? f : zero; });

            // Find the last entry whose value is closest to zero
            int lastZero = Array.FindLastIndex<float>(actualValues, (f) => (f == zero));
            if (lastZero == -1) lastZero = byte.MinValue;

            // Find *any* array entry whose value is closest to 1.0F
            float plusOne = float.PositiveInfinity;
            Array.ForEach<float>(actualValues, (f) => { plusOne = (Math.Abs(f - 1.0F) < Math.Abs(plusOne - 1.0F)) ? f : plusOne; });

            // Find the first array entry whose value is closest to 1.0F
            int firstPlusOne = Array.FindIndex<float>(actualValues, (f) => (f == plusOne));
            if (firstPlusOne == -1) firstPlusOne = byte.MaxValue;

            for (int wiper = 0; wiper <= byte.MaxValue; wiper++)
            {
                float deflection = (float)wiper / (float)byte.MaxValue;
                int match = firstPlusOne;
                for (int index = lastZero; index <= firstPlusOne; index++)
                {
                    if (Math.Abs(actualValues[index] - deflection) < Math.Abs(actualValues[match] - deflection))
                        match = index;
                }
                calibrationTable[wiper] = (byte)match;
            }

            setTrigger(0.0F);

            graphTrigger(controlName + " Calibrated (Simulation)", setTrigger, getTrigger, calibrationTable);
            writeCalibrationTable(controlName, calibrationTable, isInverted);
            sendCalibrationTable(controlName, calibrationTable, isInverted);
        }

        private void sendCalibrationTable(string controlName, byte[] calibrationTable, bool isInverted)
        {
            throw new NotImplementedException();
//          xboxController.Configuration.Calibration.LeftTrigger = calibrationTable;
        }

        private void writeCalibrationTable(string controlName, byte[] calibrationTable, bool isInverted)
        {
            TextWriter tw = new StreamWriter("Calibration" + controlName + ".h");
            tw.WriteLine("/*");
            tw.WriteLine(" ********************************************************************************");
            tw.WriteLine(" Calibration Table");
            tw.WriteLine(" ********************************************************************************");
            tw.WriteLine(" */");
            tw.WriteLine(String.Format("const uint8_t analog_{0}[] PROGMEM = {{", controlName));
            for (int wiper = 0; wiper <= byte.MaxValue; wiper++)
                tw.WriteLine(String.Format("\t{0},\t// wiper = {1}", isInverted ? byte.MaxValue - calibrationTable[byte.MaxValue - wiper] : calibrationTable[wiper], wiper));
            tw.WriteLine("};");
            tw.Close();
        }

        private void calibrateButton_Click(object sender, EventArgs e)
        {
            stopButton.Enabled = true;
            calibrateControlComboBox.Enabled = false;
            calibrationEnabledComboBox.Enabled = false;
            graphButton.Enabled = false;
            calibrateButton.Enabled = false;

            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left Trigger") && stopButton.Enabled)
                calibrateTrigger("LT", (f) => xboxController.Triggers.Left = f, () => gamepadController.Triggers.Left, true);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right Trigger") && stopButton.Enabled)
                calibrateTrigger("RT", (f) => xboxController.Triggers.Right = f, () => gamepadController.Triggers.Right, true);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left ThumbStick X") && stopButton.Enabled)
                calibrateThumbStick("LSX", (f) => xboxController.ThumbSticks.Left.X = f, () => gamepadController.ThumbSticks.Left.X, true);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Left ThumbStick Y") && stopButton.Enabled)
                calibrateThumbStick("LSY", (f) => xboxController.ThumbSticks.Left.Y = f, () => gamepadController.ThumbSticks.Left.Y, false);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right ThumbStick X") && stopButton.Enabled)
                calibrateThumbStick("RSX", (f) => xboxController.ThumbSticks.Right.X = f, () => gamepadController.ThumbSticks.Right.X, true);
            if ((calibrateControlComboBox.Text == "All" || calibrateControlComboBox.Text == "Right ThumbStick Y") && stopButton.Enabled)
                calibrateThumbStick("RSY", (f) => xboxController.ThumbSticks.Right.Y = f, () => gamepadController.ThumbSticks.Right.Y, false);

            calibrateButton.Enabled = true;
            graphButton.Enabled = true;
            calibrateControlComboBox.Enabled = true;
            calibrationEnabledComboBox.Enabled = true;
            stopButton.Enabled = false;

            xboxController.CalibrationEnabled = false;
        }
    }
}
