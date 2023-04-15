package dev.webfx.test1.a;

import dev.webfx.test1.b.B1;
 
public class A4SamePackage {

	private B1 b1Field = new B1();
	
	public B1 getB1() {
		var a5Var = new A5VarArgs();
		a5Var.varArgMethod("test1", "test2", "test3");
		
		return b1Field;
	}
	
}
