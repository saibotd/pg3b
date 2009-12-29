
package com.esotericsoftware.controller.ui;

/**
 * Takes some action as a response to a {@link Trigger}.
 */
public interface Action {
	public void reset (Config config, Trigger trigger);

	/**
	 * @param config The config that the trigger that executed this action belongs to.
	 * @param trigger The trigger that executed this action.
	 * @return Returns a non-null result if the action was executed.
	 */
	public Object execute (Config config, Trigger trigger);

	/**
	 * Returns true if this action is able to be executed.
	 */
	public boolean isValid ();
}
