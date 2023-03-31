
package dev.webfx.prototype.parser.code;

import java.util.List;
import java.io.*;

public class A<T> {

	private List l;
	
	public static B getB() {
		return new B();
	}
	
	public void test1() {
		A.getB().getC();		
	}
	
	public B test2(A a) {
		return a.getB();
	}
	
	public void test3(List<T> aList) throws IOException {
		
	}
}
