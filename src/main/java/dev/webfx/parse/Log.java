package dev.webfx.parse;

/**
 * @author Alexander Belch
 */
public class Log {

	private LogType requiredLogType;
	
	private int indent;
	
	/**
	 * Public constructor
	 */
	public Log() {
		requiredLogType = LogType.INFO;
	}
	
	/**
	 * Set logging level
	 * 
	 * @param logLevel
	 */
	public void setLogLevel (final LogType logLevel) {
		this.requiredLogType = logLevel;
	}
	
	/**
	 * Indent logging by 1 space
	 */
	public void indent() {
		indent++;
	}
	
	/**
	 * Outdent logging by 1 space
	 */
	public void outdent() {
		indent--;
	}
	
	/**
	 * Log information
	 * 
	 * @param text
	 */
	public void info(final String text) {
		log(LogType.INFO, text);
	}
	
	/**
	 * Log warning
	 * 
	 * @param text
	 */
	public void warn(final String text) {
		log(LogType.WARN, text);
	}
	
	/**
	 * Log error
	 * 
	 * @param text
	 */
	public void error(final String text) {
		log(LogType.ERROR, text);
	}
	
	/**
	 * Log verbose
	 * 
	 * @param text
	 */
	public void verbose(final String text) {
		log(LogType.VERBOSE, text);
	}
	
	/**
	 * Log debug
	 * 
	 * @param text
	 */
	public void debug(final String text) {
		log(LogType.DEBUG, text);
	}
	
	/**
	 * Log text with indent spaces
	 * 
	 * @param text The text to log
	 */
	private void log(final LogType logLevel, final String text) {
		if (text.isBlank()) {
			return;
		}
		
		if (requiredLogType != LogType.OFF && 
			requiredLogType.getLevel() >= logLevel.getLevel()) {
		    final StringBuilder sb = new StringBuilder();
		    sb.append("[");
		    sb.append(logLevel.getText());
		    sb.append("] ");
		
		    for (int i = 0; i < indent; i++) {
			    sb.append("  ");
		    }
		    sb.append(text);
		    System.out.println (sb.toString()); // NOSONAR
		}
	}
	
}