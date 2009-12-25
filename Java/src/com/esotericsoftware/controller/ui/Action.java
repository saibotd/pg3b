
package com.esotericsoftware.controller.ui;

/**
 * Takes some action as a response to a {@link Trigger}.
 */
public interface Action {
	/**
	 * @param config The config that the trigger that executed this action belongs to.
	 * @param trigger The trigger that executed this action.
	 * @param payload The value that caused the trigger to execute this action.
	 * @return Returns true if the action was executed.
	 */
	public boolean execute (Config config, Trigger trigger, Object payload);

	/**
	 * Returns true if this action is able to be executed.
	 */
	public boolean isValid ();
}
