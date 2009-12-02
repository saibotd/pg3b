
package pg3b.ui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pg3b.PG3B;
import pg3b.XboxController;

public class StatusBar extends JPanel {
	JLabel pg3bLabel, controllerLabel;
	ImageIcon greenImage, redImage;

	public StatusBar () {
		greenImage = new ImageIcon(getClass().getResource("/green.png"));
		redImage = new ImageIcon(getClass().getResource("/red.png"));

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0)));
		{
			pg3bLabel = new JLabel("PG3B");
			add(pg3bLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(3, 6, 3, 0), 0, 0));
			pg3bLabel.setIcon(redImage);
		}
		{
			controllerLabel = new JLabel("Controller");
			add(controllerLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(3, 12, 3, 0), 0, 0));
			controllerLabel.setIcon(redImage);
		}
		{
			JPanel panel = new JPanel();
			add(panel, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
				0, 0, 0, 0), 0, 0));
		}
	}

	public void setPg3bClickedListener (final Runnable listener) {
		pg3bLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e) {
				listener.run();
			}
		});
	}

	public void setControllerClickedListener (final Runnable listener) {
		controllerLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e) {
				listener.run();
			}
		});
	}

	public void setPg3b (final PG3B pg3b) {
		EventQueue.invokeLater(new Runnable() {
			public void run () {
				pg3bLabel.setIcon(pg3b == null ? redImage : greenImage);
				pg3bLabel.setText(pg3b == null ? "PG3B" : "PG3B: " + pg3b.getPort());
			}
		});
	}

	public void setController (final XboxController controller) {
		controllerLabel.setIcon(controller == null ? redImage : greenImage);
		controllerLabel.setText(controller == null ? "Controller" : "Controller: " + (controller.getPort() + 1));
	}
}
