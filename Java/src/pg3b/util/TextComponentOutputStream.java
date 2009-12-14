
package pg3b.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

public class TextComponentOutputStream extends OutputStream {
	StringBuilder buffer = new StringBuilder(512);
	private JTextComponent textComponent;
	private boolean scrollToBottom = true;

	public TextComponentOutputStream (final JTextComponent textComponent, JScrollPane scrollPane) {
		this.textComponent = textComponent;
	}

	public void write (int b) throws IOException {
		char c = (char)b;
		buffer.append(c);
		if (c != '\n') return;
		Document document = textComponent.getDocument();
		try {
			document.insertString(document.getEndPosition().getOffset(), buffer.toString(), null);
		} catch (BadLocationException ignored) {
		}
		if (scrollToBottom) textComponent.setCaretPosition(document.getLength());
		buffer.setLength(0);
	}

	public void setScrollToBottom (boolean scrollToBottom) {
		this.scrollToBottom = scrollToBottom;
	}
}
