
package com.esotericsoftware.controller.ui;

/**
 * Takes some action as a response to a {@link Trigger}.
 */
public interface Action {
	public void reset (Config config, Trigger trigger);

	/**
	 * @param config The config that the trigger that executed this action belongs to.
	 * @param trigger The trigger that executed this action.
	 * @param isActive True if the trigger that executed this action should be considered active.
	 * @param payload The payload of the trigger that executed this action.
	 * @return Returns a non-null result if the action was executed.
	 */
	public Object execute (Config config, Trigger trigger, boolean isActive, Object payload);

	/**
	 * Returns true if this action is able to be executed.
	 */
	public boolean isValid ();

	/**
	 * Returns the general type of this action. Eg, "Device".
	 */
	public String getType ();
}
