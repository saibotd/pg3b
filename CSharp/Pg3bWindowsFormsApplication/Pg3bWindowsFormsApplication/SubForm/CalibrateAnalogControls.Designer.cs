namespace Pg3bWindowsFormsApplication.SubForm
{
    partial class CalibrateAnalogControls
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.panel1 = new System.Windows.Forms.Panel();
            this.zedGraphControl = new ZedGraph.ZedGraphControl();
            this.graphButton = new System.Windows.Forms.Button();
            this.stopButton = new System.Windows.Forms.Button();
            this.calibrateControlComboBox = new System.Windows.Forms.ComboBox();
            this.calibrateButton = new System.Windows.Forms.Button();
            this.calibrationEnabledComboBox = new System.Windows.Forms.ComboBox();
            this.panel1.SuspendLayout();
            this.SuspendLayout();
            // 
            // panel1
            // 
            this.panel1.Controls.Add(this.zedGraphControl);
            this.panel1.Location = new System.Drawing.Point(12, 12);
            this.panel1.Name = "panel1";
            this.panel1.Size = new System.Drawing.Size(760, 455);
            this.panel1.TabIndex = 0;
            // 
            // zedGraphControl
            // 
            this.zedGraphControl.Dock = System.Windows.Forms.DockStyle.Fill;
            this.zedGraphControl.Location = new System.Drawing.Point(0, 0);
            this.zedGraphControl.Name = "zedGraphControl";
            this.zedGraphControl.ScrollGrace = 0;
            this.zedGraphControl.ScrollMaxX = 0;
            this.zedGraphControl.ScrollMaxY = 0;
            this.zedGraphControl.ScrollMaxY2 = 0;
            this.zedGraphControl.ScrollMinX = 0;
            this.zedGraphControl.ScrollMinY = 0;
            this.zedGraphControl.ScrollMinY2 = 0;
            this.zedGraphControl.Size = new System.Drawing.Size(760, 455);
            this.zedGraphControl.TabIndex = 0;
            // 
            // graphButton
            // 
            this.graphButton.Location = new System.Drawing.Point(689, 473);
            this.graphButton.Name = "graphButton";
            this.graphButton.Size = new System.Drawing.Size(75, 23);
            this.graphButton.TabIndex = 1;
            this.graphButton.Text = "Graph";
            this.graphButton.UseVisualStyleBackColor = true;
            this.graphButton.Click += new System.EventHandler(this.graphButton_Click);
            // 
            // stopButton
            // 
            this.stopButton.Enabled = false;
            this.stopButton.Location = new System.Drawing.Point(527, 473);
            this.stopButton.Name = "stopButton";
            this.stopButton.Size = new System.Drawing.Size(75, 23);
            this.stopButton.TabIndex = 2;
            this.stopButton.Text = "Stop";
            this.stopButton.UseVisualStyleBackColor = true;
            this.stopButton.Click += new System.EventHandler(this.stopButton_Click);
            // 
            // calibrateControlComboBox
            // 
            this.calibrateControlComboBox.FormattingEnabled = true;
            this.calibrateControlComboBox.Items.AddRange(new object[] {
            "All",
            "Left Trigger",
            "Right Trigger",
            "Left ThumbStick X",
            "Left ThumbStick Y",
            "Right ThumbStick X",
            "Right ThumbStick Y"});
            this.calibrateControlComboBox.Location = new System.Drawing.Point(273, 474);
            this.calibrateControlComboBox.Name = "calibrateControlComboBox";
            this.calibrateControlComboBox.Size = new System.Drawing.Size(121, 21);
            this.calibrateControlComboBox.TabIndex = 3;
            this.calibrateControlComboBox.Text = "Left Trigger";
            // 
            // calibrateButton
            // 
            this.calibrateButton.Location = new System.Drawing.Point(608, 473);
            this.calibrateButton.Name = "calibrateButton";
            this.calibrateButton.Size = new System.Drawing.Size(75, 23);
            this.calibrateButton.TabIndex = 4;
            this.calibrateButton.Text = "Calibrate";
            this.calibrateButton.UseVisualStyleBackColor = true;
            this.calibrateButton.Click += new System.EventHandler(this.calibrateButton_Click);
            // 
            // calibrationEnabledComboBox
            // 
            this.calibrationEnabledComboBox.FormattingEnabled = true;
            this.calibrationEnabledComboBox.Items.AddRange(new object[] {
            "Uncalibrated",
            "Calibrated",
            "All High",
            "All Centered",
            "All Low"});
            this.calibrationEnabledComboBox.Location = new System.Drawing.Point(400, 474);
            this.calibrationEnabledComboBox.Name = "calibrationEnabledComboBox";
            this.calibrationEnabledComboBox.Size = new System.Drawing.Size(121, 21);
            this.calibrationEnabledComboBox.TabIndex = 5;
            this.calibrationEnabledComboBox.Text = "Uncalibrated";
            // 
            // CalibrateAnalogControls
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(784, 508);
            this.Controls.Add(this.calibrationEnabledComboBox);
            this.Controls.Add(this.calibrateButton);
            this.Controls.Add(this.calibrateControlComboBox);
            this.Controls.Add(this.stopButton);
            this.Controls.Add(this.graphButton);
            this.Controls.Add(this.panel1);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.None;
            this.Name = "CalibrateAnalogControls";
            this.Text = "GraphAnalogControl";
            this.Load += new System.EventHandler(this.GraphAnalogControl_Load);
            this.panel1.ResumeLayout(false);
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.Panel panel1;
        private System.Windows.Forms.Button graphButton;
        private System.Windows.Forms.Button stopButton;
        private ZedGraph.ZedGraphControl zedGraphControl;
        private System.Windows.Forms.ComboBox calibrateControlComboBox;
        private System.Windows.Forms.Button calibrateButton;
        private System.Windows.Forms.ComboBox calibrationEnabledComboBox;
    }
}