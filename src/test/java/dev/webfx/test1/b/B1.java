package dev.webfx.test1.b;

import dev.webfx.test1.c.C1Implements;

public class B1 {
	 
	private B2Record b2Field;
	private B3Enum b3Field;

	public static C1Implements getC() {
	    B2Record b2Param;
		B3Enum b3Param;
		
		return new C1Implements();
	}
}
