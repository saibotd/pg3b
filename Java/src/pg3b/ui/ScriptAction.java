
package pg3b.ui;

public class ScriptAction implements Action {
	private String script;

	public ScriptAction () {
	}

	public ScriptAction (String scriptName) {
		setScriptName(scriptName);
	}

	public String getScriptName () {
		return script;
	}

	public void setScriptName (String scriptName) {
		this.script = scriptName;
	}

	public String toString () {
		return "Script: " + script.toString();
	}

	public void execute () {
		// BOZO
	}
}
