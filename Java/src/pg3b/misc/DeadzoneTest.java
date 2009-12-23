
package pg3b.misc;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pg3b.Deadzone;
import pg3b.Deadzone.Round;
import pg3b.Deadzone.Square;

public class DeadzoneTest extends JFrame {
	float xState, yState;
	float deadzoneX = 0.3f, deadzoneY = 0.3f;
	int sizeX = (int)(255 * deadzoneX), sizeY = (int)(255 * deadzoneX);

	public DeadzoneTest () {
		super("DeadzoneTest");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final CardLayout cardLayout = new CardLayout();
		final JPanel centerPanel = new JPanel(cardLayout);
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setPreferredSize(new Dimension(512, 512));

		Hashtable labels = new Hashtable();
		labels.put(-255, new JLabel("-1"));
		labels.put(-128, new JLabel("-0.5"));
		labels.put(0, new JLabel("0"));
		labels.put(128, new JLabel("0.5"));
		labels.put(255, new JLabel("1"));

		final JSlider ySlider = new JSlider(JSlider.VERTICAL, -256, 256, 0);
		getContentPane().add(ySlider, BorderLayout.EAST);
		ySlider.setInverted(true);
		ySlider.setLabelTable(labels);
		ySlider.setPaintLabels(true);
		ySlider.setMajorTickSpacing(32);
		ySlider.setSnapToTicks(true);
		ySlider.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				yState = ySlider.getValue() / 255f;
				centerPanel.repaint();
			}
		});

		final JSlider xSlider = new JSlider(JSlider.HORIZONTAL, -256, 256, 0);
		getContentPane().add(xSlider, BorderLayout.SOUTH);
		xSlider.setLabelTable(labels);
		xSlider.setPaintLabels(true);
		xSlider.setMajorTickSpacing(32);
		xSlider.setSnapToTicks(true);
		xSlider.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				xState = xSlider.getValue() / 255f;
				centerPanel.repaint();
			}
		});

		JPanel deadzonePanel = new JPanel();
		getContentPane().add(deadzonePanel, BorderLayout.WEST);

		final JSlider deadzoneSliderX = new JSlider(JSlider.VERTICAL, 0, 100, 33);
		deadzonePanel.add(deadzoneSliderX, BorderLayout.WEST);
		deadzoneSliderX.setInverted(true);
		deadzoneSliderX.createStandardLabels(25);
		deadzoneSliderX.setPaintLabels(true);
		deadzoneSliderX.setMajorTickSpacing(25);
		deadzoneSliderX.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				deadzoneX = deadzoneSliderX.getValue() / 100f;
				sizeX = (int)(255 * deadzoneX);
				centerPanel.repaint();
			}
		});

		final JSlider deadzoneSliderY = new JSlider(JSlider.VERTICAL, 0, 100, 33);
		deadzonePanel.add(deadzoneSliderY, BorderLayout.WEST);
		deadzoneSliderY.setInverted(true);
		deadzoneSliderY.createStandardLabels(25);
		deadzoneSliderY.setPaintLabels(true);
		deadzoneSliderY.setMajorTickSpacing(25);
		deadzoneSliderY.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				deadzoneY = deadzoneSliderY.getValue() / 100f;
				sizeY = (int)(255 * deadzoneY);
				centerPanel.repaint();
			}
		});

		final JComboBox combo = new JComboBox();
		combo.setModel(new DefaultComboBoxModel(new Object[] {"round", "round (crappy)", "square"}));
		getContentPane().add(combo, BorderLayout.NORTH);
		combo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				cardLayout.show(centerPanel, (String)combo.getSelectedItem());
			}
		});

		centerPanel.add(new DeadzonePanel(new Deadzone.Square()), "square");

		centerPanel.add(new DeadzonePanel(new Deadzone.Round()), "round");

		cardLayout.show(centerPanel, (String)combo.getSelectedItem());
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private class DeadzonePanel extends JPanel {
		private final Deadzone deadzone;

		public DeadzonePanel (Deadzone deadzone) {
			this.deadzone = deadzone;
		}

		public void paintComponent (Graphics g) {
			g.setColor(Color.gray);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.white);
			g.fillRect(0, 0, 512, 512);

			g.setColor(Color.green);
			if (true) {
				// Draws all edge points.
				for (int i = -255; i < 256; i++)
					drawDeflection(g, i / 255f, 1);
				for (int i = -255; i < 256; i++)
					drawDeflection(g, i / 255f, -1);
				for (int i = -255; i < 256; i++)
					drawDeflection(g, 1, i / 255f);
				for (int i = -255; i < 256; i++)
					drawDeflection(g, -1, i / 255f);
			} else if (false) {
				// Draws all possible points (slow).
				for (int x = -255; x < 256; x++)
					for (int y = -255; y < 256; y++)
						drawDeflection(g, x / 255f, y / 255f);
			}

			g.setColor(Color.red);
			drawDeflection(g, xState, yState);

			if (deadzone instanceof Deadzone.Square) {
				g.drawRect(256 - sizeX, 256 - sizeY, sizeX * 2, sizeY * 2);
			} else if (deadzone instanceof Deadzone.Round) {
				g.drawOval(256 - sizeX, 256 - sizeY, sizeX * 2, sizeY * 2);
			}
		}

		public void drawDeflection (Graphics g, float x, float y) {
			deadzone.setSizeX(deadzoneX);
			deadzone.setSizeY(deadzoneY);
			float[] deflection = deadzone.toDeflection(x, y);
			int r = 5, d = r * 2;
			g.fillRect((int)(deflection[0] * 256) + 256 - r, (int)(deflection[1] * 256) + 256 - r, d, d);
		}
	}

	public static void main (String[] args) {
		new DeadzoneTest();
	}
}
