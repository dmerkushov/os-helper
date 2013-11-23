/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.dmerkushov.oshelper;

/**
 *
 * @author shandr
 */
public class OSHelperException extends Exception {

	/**
	 * Creates a new instance of <code>OSHelperException</code> without detail message.
	 */
	public OSHelperException () {
	}

	/**
	 * Constructs an instance of <code>OSHelperException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public OSHelperException (String msg) {
		super (msg);
	}

	/**
	 * Constructs an instance of <code>OSHelperException</code> with the specified cause.
	 * @param cause the cause (which is saved for later retrieval by the Exception.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public OSHelperException (Throwable cause) {
		super (cause);
	}

	/**
	 * Constructs an instance of <code>OSHelperException</code> with the specified detail message and cause.
	 * @param msg the detail message.
	 * @param cause the cause (which is saved for later retrieval by the Exception.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public OSHelperException (String msg, Throwable cause) {
		super (msg, cause);
	}
}
