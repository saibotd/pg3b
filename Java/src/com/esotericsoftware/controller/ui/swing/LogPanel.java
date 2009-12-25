
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.esotericsoftware.controller.ui.Settings;
import com.esotericsoftware.controller.util.MultiplexOutputStream;
import com.esotericsoftware.controller.util.TextComponentOutputStream;
import com.esotericsoftware.minlog.Log;

public class LogPanel extends JPanel {
	private JButton clearButton;
	private JTextArea logText;
	private JRadioButton traceRadio, debugRadio, infoRadio, warnRadio, errorRadio;
	private JScrollPane logScroll;
	private JCheckBox alwaysScrollCheckBox;
	private TextComponentOutputStream output;

	public LogPanel () {
		initializeLayout();
		initializeEvents();

		output = new TextComponentOutputStream(logText, logScroll);
		System.setOut(new PrintStream(new MultiplexOutputStream(System.out, output), true));
		System.setErr(new PrintStream(new MultiplexOutputStream(System.err, output), true));
	}

	private void initializeEvents () {
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				logText.setText("");
			}
		});

		alwaysScrollCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				output.setScrollToBottom(alwaysScrollCheckBox.isSelected());
			}
		});

		traceRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Log.set(LEVEL_TRACE);
				Settings.get().logLevel = LEVEL_TRACE;
				Settings.save();
			}
		});
		debugRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Log.set(LEVEL_DEBUG);
				Settings.get().logLevel = LEVEL_DEBUG;
				Settings.save();
			}
		});
		infoRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Log.set(LEVEL_INFO);
				Settings.get().logLevel = LEVEL_INFO;
				Settings.save();
			}
		});
		warnRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Log.set(LEVEL_WARN);
				Settings.get().logLevel = LEVEL_WARN;
				Settings.save();
			}
		});
		errorRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Log.set(LEVEL_ERROR);
				Settings.get().logLevel = LEVEL_ERROR;
				Settings.save();
			}
		});
		if (TRACE)
			traceRadio.setSelected(true);
		else if (DEBUG)
			debugRadio.setSelected(true);
		else if (INFO)
			infoRadio.setSelected(true);
		else if (WARN)
			warnRadio.setSelected(true);
		else if (Log.ERROR) {
			errorRadio.setSelected(true);
		}
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			logScroll = new JScrollPane();
			this.add(logScroll, new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(6, 6, 6, 6), 0, 0));
			{
				logText = new JTextArea();
				logScroll.setViewportView(logText);
				logText.setLineWrap(true);
				logText.setEditable(false);
				try {
					logText.setFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VeraMono.ttf")).deriveFont(
						12f));
				} catch (Exception ignored) {
				}
			}
		}
		{
			JPanel panel = new JPanel();
			add(panel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
				0, 6, 6, 0), 0, 0));
			panel.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 0));
			{
				errorRadio = new JRadioButton("Error");
				panel.add(errorRadio);
			}
			{
				warnRadio = new JRadioButton("Warning");
				panel.add(warnRadio);
			}
			{
				infoRadio = new JRadioButton("Info");
				panel.add(infoRadio);
			}
			{
				debugRadio = new JRadioButton("Debug");
				panel.add(debugRadio);
			}
			{
				traceRadio = new JRadioButton("Trace");
				panel.add(traceRadio);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			this.add(panel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 6, 0), 0, 0));
			{
				alwaysScrollCheckBox = new JCheckBox("Scroll to bottom");
				panel.add(alwaysScrollCheckBox);
				alwaysScrollCheckBox.setSelected(true);
			}
			{
				clearButton = new JButton();
				panel.add(clearButton);
				clearButton.setText("Clear");
			}
		}

		ButtonGroup group = new ButtonGroup();
		group.add(traceRadio);
		group.add(debugRadio);
		group.add(infoRadio);
		group.add(warnRadio);
		group.add(errorRadio);
	}
}
