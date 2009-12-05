
package pg3b.ui;

public class ScriptAction implements Action {
	private String scriptName;

	public ScriptAction () {
	}

	public ScriptAction (String scriptName) {
		setScriptName(scriptName);
	}

	public String getScriptName () {
		return scriptName;
	}

	public void setScriptName (String scriptName) {
		this.scriptName = scriptName;
	}

	public String toString () {
		return "Script: " + scriptName.toString();
	}

	public void execute () {
		// BOZO
	}
}
