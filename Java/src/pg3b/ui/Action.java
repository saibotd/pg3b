
package pg3b.ui;

/**
 * Takes some action as a response to a {@link Trigger}.
 */
public interface Action {
	/**
	 * @param trigger The trigger that executed this action.
	 * @param payload The value that caused the trigger to execute this action.
	 */
	public void execute (Trigger trigger, Object payload);

	/**
	 * Returns true if this action is ready to be executed.
	 */
	public boolean isValid ();
}
