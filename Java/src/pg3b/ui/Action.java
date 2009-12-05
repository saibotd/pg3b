
package pg3b.ui;

public interface Action {
	public boolean execute (Object payload);

	public boolean isValid ();
}
