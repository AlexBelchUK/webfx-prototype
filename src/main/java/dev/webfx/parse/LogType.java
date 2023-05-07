package dev.webfx.parse;

/**
 * @author Alexander Belch
 */
public enum LogType {
    OFF(0, "Off"), ERROR(1, "Error"), WARN(2, "Warn"), INFO(3, "Info"), VERBOSE(4, "Verbose"), DEBUG(5, "Debug");
	
	private final int level;
	private final String text;
	
	/**
	 * Private constructor
	 * 
	 * @param level The log level value
	 * @param text The log text
	 */
	private LogType(final int level,
	                 final String text) {
		this.level = level;
		this.text = text; 
	}
	
	/**
	 * @return Log level value
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * @return Log text
	 */
	public String getText() {
		return text;
	}	
}