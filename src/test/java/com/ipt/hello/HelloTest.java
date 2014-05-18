package com.ipt.hello;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HelloTest {
	
	/**
	 * Test method for example
	 */
	@Test
	public void getGreetingsTest()
	{
		String expectedGreetings = "Hello, world";
		assertEquals(expectedGreetings, Hello.getGreetings());
	}
}