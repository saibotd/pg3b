
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;
import static org.fife.ui.rsyntaxtextarea.Token.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SquiggleUnderlineHighlightPainter;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.RTextScrollPane;

import pnuts.lang.Context;
import pnuts.lang.ParseException;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.ui.Action;
import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.Script;
import com.esotericsoftware.controller.ui.ScriptAction;
import com.esotericsoftware.controller.ui.Trigger;
import com.esotericsoftware.controller.util.JMultilineTooltip;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.minlog.Log;

public class ScriptEditor extends EditorPanel<Script> {
	private int lastCaretPosition;
	private HashMap<File, Integer> fileToPosition = new HashMap();
	private TimerTask compileTask;
	private Object highlightTag;
	private RecordListener recordListener = new RecordListener();

	private RSyntaxTextArea codeText;
	private RTextScrollPane codeScroll;
	private SquiggleUnderlineHighlightPainter errorPainter = new SquiggleUnderlineHighlightPainter(Color.red);
	private AutoCompletion autoCompletion;
	private JButton executeButton;
	private JToggleButton recordButton;
	private JLabel errorLabel;
	private Device device;

	public ScriptEditor (UI owner) {
		super(owner, Script.class, new File("scripts"), ".script");

		initializeLayout();
		initializeEvents();

		setFontSize(12f);

		DefaultCompletionProvider provider = new DefaultCompletionProvider() {
			protected boolean isValidChar (char ch) {
				return super.isValidChar(ch) || ch == '.';
			}
		};
		try {
			provider.loadFromXML(getClass().getResourceAsStream("/completion.xml"));
		} catch (IOException ex) {
			if (Log.WARN) warn("Error loading autocompletion.", ex);
		}
		provider.addCompletion(new ShorthandCompletion(provider, "S", "device.set(\"start\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "G", "device.set(\"guide\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "BK", "device.set(\"back\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "A", "device.set(\"a\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "B", "device.set(\"b\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "X", "device.set(\"x\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "Y", "device.set(\"y\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "U", "device.set(\"up\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "D", "device.set(\"down\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "L", "device.set(\"left\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "R", "device.set(\"right\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RT", "device.set(\"rightTrigger\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LT", "device.set(\"leftTrigger\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RSH", "device.set(\"rightShoulder\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LSH", "device.set(\"leftShoulder\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RST", "device.set(\"rightStick\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LST", "device.set(\"leftStick\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RX", "device.set(\"rightStickX\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RY", "device.set(\"rightStickY\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "RXY", "device.set(\"rightStick\", 1, 1)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LX", "device.set(\"leftStickX\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LY", "device.set(\"leftStickY\", payload)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LXY", "device.set(\"leftStick\", 1, 1)"));
		autoCompletion = new AutoCompletion(provider);
		autoCompletion.setListCellRenderer(new CellRenderer());
		autoCompletion.setShowDescWindow(true);
		autoCompletion.setParameterAssistanceEnabled(true);
		autoCompletion.setDescriptionWindowSize(300, 300);
		autoCompletion.setAutoCompleteSingleChoices(false);
		autoCompletion.install(codeText);
	}

	protected void updateFieldsFromItem (Script script) {
		if (script == null)
			codeText.setText("");
		else {
			Integer position = fileToPosition.get(script.getFile());
			codeText.setText(script.getCode());
			try {
				codeText.setCaretPosition(position == null ? 0 : position);
			} catch (Exception ignored) {
			}
		}
	}

	protected void updateItemFromFields (Script script) {
		script.setCode(codeText.getText());
	}

	protected void clearItemSpecificState () {
		lastCaretPosition = 0;
		codeText.discardAllEdits();
	}

	protected void itemRenamed (Script oldScript, Script newScript) {
		// Update any ScriptActions that reference the old item.
		ConfigEditor configEditor = owner.getConfigTab().getConfigEditor();
		Config selectedConfig = configEditor.getSelectedItem();
		for (Config config : configEditor.getItems()) {
			boolean needsSave = false;
			for (Trigger trigger : config.getTriggers()) {
				if (trigger.getAction() instanceof ScriptAction) {
					ScriptAction action = (ScriptAction)trigger.getAction();
					if (action.getScriptName().equals(oldScript.getName())) {
						action.setScriptName(newScript.getName());
						needsSave = true;
					}
				}
			}
			if (needsSave) configEditor.saveItem(config, true);
		}
		configEditor.setSelectedItem(selectedConfig);
	}

	protected JPopupMenu getPopupMenu () {
		final Script script = getSelectedItem();
		final ConfigEditor configEditor = owner.getConfigTab().getConfigEditor();
		final HashMap<Config, Trigger> configs = new HashMap();
		outer: //
		for (Config config : configEditor.getItems()) {
			int i = 0;
			for (Trigger trigger : config.getTriggers()) {
				Action action = trigger.getAction();
				if (action instanceof ScriptAction) {
					if (((ScriptAction)action).getScript() == script) {
						configs.put(config, trigger);
						continue outer;
					}
				}
				i++;
			}
		}
		if (configs.isEmpty()) return null;
		JPopupMenu popupMenu = new JPopupMenu();
		for (final Config config : configs.keySet()) {
			popupMenu.add(new JMenuItem("Go to " + config + "...")).addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					configEditor.setSelectedItem(config);
					configEditor.setSelectedTrigger(configs.get(config));
					owner.getTabs().setSelectedComponent(owner.getConfigTab());
				}
			});
		}
		return popupMenu;
	}

	public void saveItem (Script item, boolean force) {
		super.saveItem(item, force);
		errorLabel.setText("");
		if (highlightTag != null) {
			codeText.getHighlighter().removeHighlight(highlightTag);
			highlightTag = null;
		}

		if (Config.getActive() != null) {
			// Try to reset the saved trigger in the active config.
			ConfigEditor configEditor = owner.getConfigTab().getConfigEditor();
			for (Config config : configEditor.getItems()) {
				for (Trigger trigger : config.getTriggers()) {
					if (trigger.getAction() instanceof ScriptAction) {
						ScriptAction action = (ScriptAction)trigger.getAction();
						if (action.getScript() == item) {
							try {
								action.reset(config, trigger);
							} catch (Exception ignored) {
							}
						}
					}
				}
			}
		}
	}

	private void execute () {
		errorLabel.setForeground(Color.black);
		errorLabel.setText("Executing script...");
		if (highlightTag != null) {
			codeText.getHighlighter().removeHighlight(highlightTag);
			highlightTag = null;
		}
		new Thread("Execute") {
			public void run () {
				try {
					Context context = ScriptAction.getContext(null, null, null);
					Pnuts pnuts = Pnuts.parse(new StringReader(codeText.getText()));
					pnuts.run(context);
					ScriptAction.execute(pnuts, context, ScriptAction.FUNCTION_INIT, 0);
					ScriptAction.execute(pnuts, context, ScriptAction.FUNCTION_ACTIVATE, 1);
					Device device = owner.getDevice();
					try {
						if (device != null) device.collect();
						ScriptAction.execute(pnuts, context, ScriptAction.FUNCTION_CONTINUOUS, 1);
						ScriptAction.execute(pnuts, context, ScriptAction.FUNCTION_DEACTIVATE, 0);
					} finally {
						try {
							if (device != null) device.apply();
						} catch (IOException ex) {
							if (Log.ERROR) error("Error applying device changes.", ex);
						}
					}
					EventQueue.invokeLater(new Runnable() {
						public void run () {
							errorLabel.setForeground(Color.black);
							errorLabel.setText("Script completed successfully.");
						}
					});
				} catch (final PnutsException ex) {
					if (DEBUG) debug("Error during script execution: " + getSelectedItem(), ex);
					final String message;
					if (ex.getThrowable() != null)
						message = ex.getThrowable().getClass().getSimpleName() + ": " + ex.getThrowable().getMessage();
					else
						message = ex.getMessage();
					EventQueue.invokeLater(new Runnable() {
						public void run () {
							try {
								int line = ex.getLine();
								int start = codeText.getLineStartOffset(line - 1) + ex.getColumn();
								int end = codeText.getLineEndOffset(line - 1);
								highlightTag = codeText.getHighlighter().addHighlight(start, end, errorPainter);
							} catch (BadLocationException ignored) {
							}
							displayErrorMessage(message);
						}
					});
				} catch (final Exception ex) {
					if (DEBUG) debug("Error during script compilation: " + getSelectedItem(), ex);
					final String message = ex.getMessage();
					EventQueue.invokeLater(new Runnable() {
						public void run () {
							displayErrorMessage(message);
						}
					});
				}
			}
		}.start();
		codeText.requestFocus();
	}

	private void displayErrorMessage (String message) {
		errorLabel.setForeground(Color.red);
		errorLabel.setText(message);
		errorLabel.setToolTipText(message);
	}

	public void setDevice (Device device) {
		if (this.device != null) this.device.removeListener(recordListener);
		this.device = device;
		recordButton.setEnabled(device != null && getSelectedItem() != null);
		recordButton.setSelected(false);
	}

	private void initializeEvents () {
		codeText.addCaretListener(new CaretListener() {
			public void caretUpdate (CaretEvent event) {
				Script script = getSelectedItem();
				if (script == null) return;
				if (codeText.getText().length() > 0) fileToPosition.put(script.getFile(), event.getDot());
			}
		});

		codeText.addParser(new AbstractParser() {
			public ParseResult parse (RSyntaxDocument doc, String style) {
				if (highlightTag != null) {
					codeText.getHighlighter().removeHighlight(highlightTag);
					highlightTag = null;
				}
				DefaultParseResult result = new DefaultParseResult(this);
				result.setParsedLines(0, codeText.getLineCount() - 1);
				if (getSelectedItem() == null) return result;
				try {
					Pnuts.parse(codeText.getText());
				} catch (ParseException ex) {
					if (DEBUG) debug("Error during script compilation: " + getSelectedItem(), ex);
					try {
						String message = ex.getMessage();
						int line = ex.getErrorLine() - 1;
						int offset = codeText.getLineStartOffset(line) + ex.getErrorColumn() - 1;
						int length = codeText.getLineEndOffset(line) - offset;
						result.addNotice(new DefaultParserNotice(this, message, line, offset, length));
						displayErrorMessage(message);
					} catch (BadLocationException ex2) {
						throw new RuntimeException(ex2);
					}
				}
				return result;
			}
		});

		executeButton.addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent event) {
				execute();
			}
		});

		recordButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (recordButton.isSelected()) {
					recordListener.lastTime = -1;
					device.addListener(recordListener);
				} else
					device.removeListener(recordListener);
			}
		});

		codeText.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK), "execute");
		codeText.getActionMap().put("execute", new AbstractAction() {
			public void actionPerformed (ActionEvent event) {
				execute();
			}
		});
	}

	private void initializeLayout () {
		{
			codeText = new RSyntaxTextArea();
			codeText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			codeText.setHighlightCurrentLine(false);
			codeText.setCloseCurlyBraces(false);
			codeText.setCaretColor(Color.black);
			codeText.setBackground(Color.white);
			codeText.setSelectionColor(new Color(0xb8ddff));
			codeText.setTextAntiAliasHint("VALUE_TEXT_ANTIALIAS_ON");
			{
				codeScroll = new RTextScrollPane(codeText);
				getContentPanel().add(
					codeScroll,
					new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
						0, 0), 0, 0));
			}
		}
		{
			errorLabel = new JLabel() {
				public JToolTip createToolTip () {
					return new JMultilineTooltip(640);
				}
			};
			getContentPanel().add(
				errorLabel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			executeButton = new JButton("Execute");
			getContentPanel().add(
				executeButton,
				new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
		}
		{
			recordButton = new JToggleButton("Record");
			getContentPanel().add(
				recordButton,
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
		}

		Util.enableWhenModelHasSelection(getSelectionModel(), new Runnable() {
			public void run () {
				recordButton.setEnabled(device != null && recordButton.isEnabled());
			}
		}, recordButton, codeText, executeButton);
	}

	private class RecordListener extends Device.Listener {
		long lastTime;

		public void axisChanged (final Axis axis, final float state) {
			sleep();
			EventQueue.invokeLater(new Runnable() {
				public void run () {
					codeText.append("device.set(\"" + axis.name() + "\", " + state + ")\n");
				}
			});
		}

		public void buttonChanged (final Button button, final boolean pressed) {
			sleep();
			EventQueue.invokeLater(new Runnable() {
				public void run () {
					codeText.append("device.set(\"" + button.name() + "\", " + pressed + ")\n");
				}
			});
		}

		private void sleep () {
			long time = System.currentTimeMillis();
			final long sleep = time - lastTime;
			if (lastTime != -1 && sleep > 0) {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						codeText.append("sleep(" + sleep + ")\n");
					}
				});
			}
			lastTime = time;
		}
	};

	private void setFontSize (float size) {
		try {
			Font mono = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VeraMono.ttf")).deriveFont(size);
			Font monoBold = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VeraMonoBold.ttf"))
				.deriveFont(size);
			Font plain = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Vera.ttf")).deriveFont(size);

			Style black = new Style(new Color(0, 0, 0), null);
			Style blackBold = new Style(new Color(0, 0, 0), null, monoBold);
			Style greenPlain = new Style(new Color(0, 164, 82), null, plain);
			Style darkBlueBold = new Style(new Color(0, 0, 128), null, monoBold);
			Style darkBlue = new Style(new Color(0, 0, 128), null);
			Style blue = new Style(new Color(0, 0, 255), null);
			Style red = new Style(new Color(255, 0, 0), null);
			Style darkGreen = new Style(new Color(64, 128, 128), null);

			Style[] styles = new Style[Token.NUM_TOKEN_TYPES];
			styles[COMMENT_DOCUMENTATION] = greenPlain;
			styles[COMMENT_MULTILINE] = greenPlain;
			styles[COMMENT_EOL] = greenPlain;
			styles[RESERVED_WORD] = darkBlueBold;
			styles[FUNCTION] = darkBlueBold;
			styles[LITERAL_BOOLEAN] = darkBlueBold;
			styles[LITERAL_NUMBER_DECIMAL_INT] = blue;
			styles[LITERAL_NUMBER_FLOAT] = blue;
			styles[LITERAL_NUMBER_HEXADECIMAL] = blue;
			styles[LITERAL_STRING_DOUBLE_QUOTE] = darkGreen;
			styles[LITERAL_CHAR] = darkGreen;
			styles[LITERAL_BACKQUOTE] = darkGreen;
			styles[DATA_TYPE] = darkBlueBold;
			styles[VARIABLE] = darkBlue;
			styles[IDENTIFIER] = black;
			styles[WHITESPACE] = black;
			styles[SEPARATOR] = blue;
			styles[OPERATOR] = blue;
			styles[PREPROCESSOR] = blue;
			styles[MARKUP_TAG_DELIMITER] = black;
			styles[MARKUP_TAG_NAME] = black;
			styles[MARKUP_TAG_ATTRIBUTE] = black;
			styles[ERROR_IDENTIFIER] = red;
			styles[ERROR_NUMBER_FORMAT] = red;
			styles[ERROR_STRING_DOUBLE] = red;
			styles[ERROR_CHAR] = red;

			SyntaxScheme scheme = new SyntaxScheme(false);
			scheme.styles = styles;
			codeText.setSyntaxScheme(scheme);
			codeText.setFont(mono);
			codeScroll.getGutter().setLineNumberFont(mono);
		} catch (Exception ex) {
			if (WARN) warn("Error setting fonts.", ex);
		}
	}

	static class CellRenderer extends CompletionCellRenderer {
		private Icon variableIcon, functionIcon, emptyIcon;

		public CellRenderer () {
			variableIcon = new ImageIcon(getClass().getResource("/variable.png"));
			functionIcon = new ImageIcon(getClass().getResource("/function.png"));
			emptyIcon = new EmptyIcon(16);
		}

		protected void prepareForOtherCompletion (JList list, Completion c, int index, boolean selected, boolean hasFocus) {
			super.prepareForOtherCompletion(list, c, index, selected, hasFocus);
			setIcon(emptyIcon);
		}

		protected void prepareForVariableCompletion (JList list, VariableCompletion vc, int index, boolean selected,
			boolean hasFocus) {
			super.prepareForVariableCompletion(list, vc, index, selected, hasFocus);
			setIcon(variableIcon);
		}

		protected void prepareForFunctionCompletion (JList list, FunctionCompletion fc, int index, boolean selected,
			boolean hasFocus) {
			super.prepareForFunctionCompletion(list, fc, index, selected, hasFocus);
			setIcon(functionIcon);
		}
	}

	static class EmptyIcon implements Icon, Serializable {
		private int size;

		public EmptyIcon (int size) {
			this.size = size;
		}

		public int getIconHeight () {
			return size;
		}

		public int getIconWidth () {
			return size;
		}

		public void paintIcon (Component c, Graphics g, int x, int y) {
		}
	}
}
