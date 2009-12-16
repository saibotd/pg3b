
package pg3b;

/**
 * The low level commands that can be sent to the PG3B.
 */
enum Command {
	action('A'), //
	eventAction('E'), //
	setCalibrationEnabled('C'), //
	setDebugMessagesEnabled('D'), //
	setFrequency('F'), //
	setIsWireless('G'), // Deprecated.
	setResolution('R'), //
	setScaling('S'), //
	initializeProfile('P'), //
	finalizeProfile('Q'), //
	readPage('R'), //
	writePage('W');

	char code;

	private Command (char code) {
		this.code = code;
	}
}
