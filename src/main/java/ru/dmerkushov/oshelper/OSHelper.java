/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.dmerkushov.oshelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author Dmitriy Merkushov
 */
public class OSHelper {

	/**
	 * @deprecated Use ProcessReturn.osh_PROCESS_EXITED_BY_ITSELF
	 */
	public static final int osh_PROCESS_EXITED_BY_ITSELF = 1;
	/**
	 * @deprecated Use ProcessReturn.osh_PROCESS_KILLED_BY_TIMEOUT
	 */
	public static final int osh_PROCESS_KILLED_BY_TIMEOUT = 2;

	public static class ProcessReturn {

		public static final int osh_PROCESS_EXITED_BY_ITSELF = 1;
		public static final int osh_PROCESS_KILLED_BY_TIMEOUT = 2;
		/**
		 * The exit status of the process:
		 * <code>osh_PROCESS_EXITED_BY_ITSELF</code> if it exited normally, or
		 * <code>osh_PROCESS_KILLED_BY_TIMEOUT</code> if it was killed by
		 * timeout
		 */
		public int exitStatus;
		public int exitCode;
		public String stdout;
		public String stderr;
	}

	/**
	 * Runs a new OS process
	 *
	 * @param toRun OS console command to run
	 * @return the process object
	 * @throws OSHelperException
	 */
	public static Process runCommand (List<String> toRun) throws OSHelperException {
		Process proc;
		ProcessBuilder procBuilder = new ProcessBuilder ();
		procBuilder.command (toRun);

		try {
			proc = procBuilder.start ();
		} catch (IOException ex) {
			throw new OSHelperException ("Received an IOException when running command:\n" + toRun + "\n" + ex.getMessage (), ex);
		}

		return proc;
	}

	/*public static Thread runCommandInThread (final List<String> toRun) throws OSHelperException {
	 Thread commandThread;

	 }*/
	public static ProcessReturn procWaitWithProcessReturn (Process process) throws OSHelperException {
		ProcessReturn processReturn = new ProcessReturn ();
		try {
			processReturn.exitCode = process.waitFor ();
		} catch (InterruptedException ex) {
			throw new OSHelperException ("Received an InterruptedException when waiting for an external process to terminate.", ex);
		}
		InputStream stdoutIs = process.getInputStream ();
		processReturn.stdout = readInputStreamAsString (stdoutIs);

		InputStream stderrIs = process.getErrorStream ();
		processReturn.stderr = readInputStreamAsString (stderrIs);

		processReturn.exitStatus = ProcessReturn.osh_PROCESS_EXITED_BY_ITSELF;
		return processReturn;
	}

	/**
	 * Waits for a specified process to terminate for the specified amount of
	 * milliseconds. If it is not terminated in the desired time, it is
	 * terminated explicitly.
	 *
	 * NOT TESTED!!!
	 *
	 * @param process
	 * @param timeout
	 * @return
	 * @throws OSHelperException
	 */
	public static ProcessReturn procWaitWithProcessReturn (Process process, final long timeout) throws OSHelperException {
		long endTime = Calendar.getInstance ().getTimeInMillis () + timeout;

		boolean terminatedByItself = false;

		ProcessReturn processReturn = new ProcessReturn ();
		while (Calendar.getInstance ().getTimeInMillis () < endTime) {
			try {
				InputStream stdoutIs = process.getInputStream ();
				processReturn.stdout = readInputStreamAsString (stdoutIs);

				InputStream stderrIs = process.getErrorStream ();
				processReturn.stderr = readInputStreamAsString (stderrIs);

				processReturn.exitCode = process.exitValue ();
				processReturn.exitStatus = ProcessReturn.osh_PROCESS_EXITED_BY_ITSELF;

				terminatedByItself = true;
			} catch (IllegalThreadStateException ex) {	// the process has not yet terminated
				terminatedByItself = false;
			}
			if (terminatedByItself) {
				break;
			}
		}
		if (!terminatedByItself) {
			process.destroy ();

			try {
				InputStream stdoutIs = process.getInputStream ();
				processReturn.stdout = readInputStreamAsString (stdoutIs);

				InputStream stderrIs = process.getErrorStream ();
				processReturn.stderr = readInputStreamAsString (stderrIs);

				processReturn.exitCode = process.waitFor ();
			} catch (InterruptedException ex) {
				throw new OSHelperException (ex);
			}

			processReturn.exitStatus = ProcessReturn.osh_PROCESS_KILLED_BY_TIMEOUT;
		}

		return processReturn;
	}

	/**
	 * Waits for a specified process to terminate
	 *
	 * @param process
	 * @throws OSHelperException
	 * @deprecated Use ProcessReturn procWaitWithProcessReturn () instead
	 */
	public static int procWait (Process process) throws OSHelperException {
		try {
			return process.waitFor ();
		} catch (InterruptedException ex) {
			throw new OSHelperException ("Received an InterruptedException when waiting for an external process to terminate.", ex);
		}
	}

	/**
	 * Waits for a specified process to terminate for the specified amount of
	 * milliseconds. If it is not terminated in the desired time, it is
	 * terminated explicitly.
	 *
	 * NOT TESTED!!!
	 *
	 * @param process
	 * @param timeout
	 * @return either <code>osh_PROCESS_EXITED_BY_ITSELF</code> * * *
	 * or <code>osh_PROCESS_KILLED_BY_TIMEOUT</code>
	 * @throws OSHelperException
	 * @deprecated Use ProcessReturn procWaitWithProcessReturn () instead
	 */
	public static int procWait (Process proc, final long timeout) throws OSHelperException {
		long endTime = Calendar.getInstance ().getTimeInMillis () + timeout;

		boolean terminatedByItself = false;
		int toReturn = 0;

		while (Calendar.getInstance ().getTimeInMillis () < endTime) {
			try {
				proc.exitValue ();
				terminatedByItself = true;
				toReturn = osh_PROCESS_EXITED_BY_ITSELF;
			} catch (IllegalThreadStateException ex) {
				terminatedByItself = false;
			}
			if (terminatedByItself) {
				break;
			}
		}
		if (!terminatedByItself) {
			proc.destroy ();

			try {
				proc.waitFor ();
			} catch (InterruptedException ex) {
				throw new OSHelperException (ex);
			}

			toReturn = osh_PROCESS_KILLED_BY_TIMEOUT;
		}

		return toReturn;
	}

	/**
	 * Read a file to a byte array
	 *
	 * @param fileName
	 * @return
	 * @throws OSHelperException
	 */
	public static byte[] readFile (String fileName) throws OSHelperException {
		File file = new File (fileName);
		if (!file.exists ()) {
			throw new OSHelperException ("File " + fileName + " does not exist");
		}
		if (!file.canRead ()) {
			throw new OSHelperException ("File " + fileName + " is not readable");
		}
		if (!file.isFile ()) {
			throw new OSHelperException ("File " + fileName + " is not a normal file");
		}

		RandomAccessFile raFile = null;
		try {
			raFile = new RandomAccessFile (fileName, "r");
		} catch (FileNotFoundException ex) {
			throw new OSHelperException ("File " + fileName + " not found");
		}

		long fileLengthLong;
		try {
			fileLengthLong = raFile.length ();
		} catch (IOException ex) {
			throw new OSHelperException (ex);
		}
		if (fileLengthLong > Integer.MAX_VALUE) {
			throw new OSHelperException ("File length exceeds " + Integer.MAX_VALUE + " bytes: " + fileLengthLong);
		}

		int fileLengthInt = (int) fileLengthLong;

		byte[] bytes = new byte[fileLengthInt];
		try {
			raFile.read (bytes);
		} catch (IOException ex) {
			throw new OSHelperException (ex);
		} finally {
			try {
				raFile.close ();
			} catch (IOException ex) {
				throw new OSHelperException (ex);
			}
		}

		return bytes;
	}

	/**
	 * Write a file
	 *
	 * @param bytes
	 * @param targetFileName
	 * @throws OSHelperException
	 */
	public static void writeFile (byte[] bytes, String targetFileName) throws OSHelperException {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream (targetFileName);
		} catch (FileNotFoundException ex) {
			throw new OSHelperException ("Received a FileNotFoundException when trying to open target file " + targetFileName + ".", ex);
		}
		try {
			fos.write (bytes);
		} catch (IOException ex) {
			throw new OSHelperException ("Received an IOException when trying to write to target file " + targetFileName + ".", ex);
		} finally {
			try {
				fos.close ();
			} catch (IOException ex) {
				throw new OSHelperException ("Received an IOException when trying to close FileOutputStream for target file " + targetFileName + ".", ex);
			}
		}
	}

	/**
	 * Copy files
	 *
	 * @param sourceFileName
	 * @param targetFileName
	 * @throws OSHelperException
	 */
	public static void copyFile (String sourceFileName, String targetFileName) throws OSHelperException {
		File sourceFile = new File (sourceFileName);
		File targetFile = new File (targetFileName);

		FileReader sourceReader;
		try {
			sourceReader = new FileReader (sourceFile);
		} catch (FileNotFoundException ex) {
			throw new OSHelperException ("Received an FileNotFoundException when trying to open source file " + sourceFileName + ".", ex);
		}

		FileWriter targetWriter;
		try {
			targetWriter = new FileWriter (targetFile);
		} catch (IOException ex) {
			throw new OSHelperException ("Received an IOException when trying to open target file " + targetFileName + ".", ex);
		}


		int readByteInInt = 0;
		while (readByteInInt != -1) {

			try {
				readByteInInt = sourceReader.read ();
			} catch (IOException ex) {
				throw new OSHelperException ("Received an IOException when trying to read from source file " + sourceFileName + ".", ex);
			}
			if (readByteInInt != -1) {
				try {
					targetWriter.write (readByteInInt);
				} catch (IOException ex) {
					throw new OSHelperException ("Received an IOException when trying to write to target file " + targetFileName + ".", ex);
				}
			}
		}

		try {
			sourceReader.close ();
		} catch (IOException ex) {
			throw new OSHelperException ("Received an IOException when trying to close reader for source file " + sourceFileName + ".", ex);
		}
		try {
			targetWriter.close ();
		} catch (IOException ex) {
			throw new OSHelperException ("Received an IOException when trying to close reader for target file " + targetFileName + ".", ex);
		}

	}

	/**
	 * Send a simple email
	 *
	 * @param target
	 * @param topic
	 * @param text
	 * @return The ProcessReturn structure of the mailing process. Normal are
	 * empty <code>stdout</code> and <code>stderr</code>, 0 	 * for <code>exitCode</code>
	 * and <code>osh_PROCESS_EXITED_BY_ITSELF</code> for <code>exitStatus</code>
	 * @see ProcessReturn
	 */
	public static ProcessReturn sendEmail_Unix (String target, String topic, String text) throws OSHelperException {
		topic = topic.replaceAll ("\"", "\\\"");
		String emailCommandAsString = "mail -s \"" + topic + "\" -S sendcharsets=utf-8 " + target;
		List<String> emailCommandAsList = OSHelper.constructCommandAsList_Unix_sh (emailCommandAsString);
		Process process = OSHelper.runCommand (emailCommandAsList);
		OutputStream processStdin = process.getOutputStream ();
		try {
			processStdin.write (text.getBytes ());
		} catch (IOException ex) {
			throw new OSHelperException (ex);
		}
		try {
			processStdin.close ();
		} catch (IOException ex) {
			throw new OSHelperException (ex);
		}
		ProcessReturn processReturn = OSHelper.procWaitWithProcessReturn (process, 10000);
		return processReturn;
	}

	/**
	 * Helper method to construct a UNIX-like-OS-compatible command that is
	 * using the SH shell syntax.
	 *
	 * @param command
	 * @return
	 */
	public static List<String> constructCommandAsList_Unix_sh (String command) {
		List<String> commandAsList = new ArrayList<String> ();

		commandAsList.add ("sh");
		commandAsList.add ("-c");
		commandAsList.add (command);

		return commandAsList;
	}

	/**
	 * Helper method to construct a Windows-compatible command that is using the
	 * CMD shell syntax.
	 *
	 * @param command
	 * @return
	 */
	public static List<String> constructCommandAsList_Windows_cmd (String command) {
		List<String> commandAsList = new ArrayList<String> ();

		commandAsList.add ("cmd");
		commandAsList.add ("/c");
		commandAsList.add (command);

		return commandAsList;
	}

	private static String readInputStreamAsString (InputStream is) throws OSHelperException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		int availableBytes;
		try {
			availableBytes = is.available ();
		} catch (IOException ex) {
			if (ex.getMessage () != null && ex.getMessage ().startsWith ("Stream closed")) {
				return null;
			} else {
				throw new OSHelperException (ex);
			}
		}
		while (availableBytes > 0) {
			try {
				baos.write (is.read ());
			} catch (IOException ex) {
				throw new OSHelperException (ex);
			}
			try {
				availableBytes = is.available ();
			} catch (IOException ex) {
				if (ex.getMessage () != null && ex.getMessage ().startsWith ("Stream closed")) {
					return null;
				} else {
					throw new OSHelperException (ex);
				}
			}
		}
		return baos.toString ();
	}
}
