
package dev.webfx.test1.a;

import java.util.*;

import dev.webfx.test1.b.B1;
import dev.webfx.test1.c.C1Implements;

import java.io.*;
 
public class A1Generic<T> {

	private List<T> listField;
	
	private A4SamePackage a4Field;
	
	private T t0Field;
	
	private dev.webfx.test1.c.C6BasicClass c6Field;
	
	private class A3Inner {
		private C1Implements c1Field;
		
		public A3Inner() {
			c1Field = new C1Implements();
		}
		
		public void setC1(final C1Implements c1Arg) {
			this.c1Field = c1Arg;
		}
		
		public C1Implements getC1() {
			return c1Field;
		}
	}
	
	public A1Generic() {
		listField = new ArrayList<T>();
	}
	
	public Object getB1() {
		return new B1();		
	}
	
	public void paramTest3(B1 b1Arg, C1Implements c1Arg) throws IOException {
		final A2Outer a2Param = new A2Outer();
	}
	
	public Thread lambdaTest() {
		return new Thread(() -> {
			final A4SamePackage a4ThreadParam;
		});
	}
	
	public T genericMethod(T t1Arg) {
	    return t1Arg;
	}
	
	public static dev.webfx.test1.c.C6BasicClass staticMethod(A1Generic a1Arg, B1 b1Arg) {
		return new dev.webfx.test1.c.C6BasicClass();
	}
}

class A2Outer {
	
	private B1 b1Field;
	
	public A2Outer() {
		b1Field = new B1();
	}
	
	public B1 getB1() {
		return b1Field;
	}
}