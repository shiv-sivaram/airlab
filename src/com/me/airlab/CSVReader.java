// $Id$

package com.me.airlab;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSVReader
{
	private static Logger logger = Logger.getLogger( CSVReader.class.getName() );

	private static final boolean DEBUGGING = true;

	/**
	 * Constructor
	 *
	 * @param r         input Reader source of CSV Fields to read.
	 *
	 * @param separator
	 *                  field separator character, usually ',' in North America,
	 *                  ';' in Europe and sometimes '\t' for tab.
	 * @param quote
	 *                  char to use to enclose fields containing a separator,
	 *                  usually '\"'
	 * @param allowMultiLineFields
	 *                  true if reader should allow
	 *                  quoted fields to span
	 *                  more than one line.  Microsoft Excel
	 *                  sometimes generates files like this.
	 * @param trim      true if reader should trim lead/trailing whitespace
	 *                  e.g. blanks, Cr, Lf. Tab off fields.
	 */
	public CSVReader(Reader r, char separator, char quote, boolean allowMultiLineFields, boolean trim)
	{
		/* convert Reader to BufferedReader if necessary */
		if ( r instanceof BufferedReader )
		{
			this.r = (BufferedReader) r;
		}
		else
		{
			this.r = new BufferedReader(r);
		}
		if ( this.r == null )
		{
			throw new IllegalArgumentException("invalid Reader");//No I18N
		}

		this.separator = separator;
		this.quote = quote;
		this.allowMultiLineFields = allowMultiLineFields;
		this.trim = trim;
	}

	/**
	 * convenience Constructor, default to comma separator, " for quote,
	 * no multiline fields, with trimming.
	 *
	 * @param r     input Reader source of CSV Fields to read.
	 */
	public CSVReader(Reader r)
	{
		this(r, ',', '\"', false, true );
	}


	/**
	 * Reader source of the CSV fields to be read.
	 */
	private BufferedReader r;

	/**
	 * field separator character, usually ',' in North America,
	 * ';' in Europe and sometimes '\t' for tab.
	 */
	private final char separator;

	/**
	 * quote character, usually '\"'
	 * '\'' for SOL used to enclose fields containing a separator character.
	 */
	private final char quote;

	/**
	 * true if reader should allow
	 * quoted fields to span
	 * more than one line.  Microsoft Excel
	 * sometimes generates files like this.
	 */
	private final boolean allowMultiLineFields;

	/**
	 * true if reader should trim lead/trail whitespace
	 * from fields returned.
	 */
	private final boolean trim;


	/**
	 * category of end of line char.
	 */
	private static final int EOL = 0;

	/**
	 * category of ordinary character
	 */
	private static final int ORDINARY = 1;

	/**
	 * categotory of the quote mark "
	 */
	private static final int QUOTE = 2;

	/**
	 * category of the separator, e.g. comma, semicolon
	 * or tab.
	 */
	private static final int SEPARATOR = 3;

	/**
	 * category of characters treated as white space.
	 */
	private static final int WHITESPACE = 4;

	private HashMap data = null;
	/**
	 * e.g. \n \r\n or \r, whatever system uses to separate lines in a text file.
	 */
	static  String lineSeparator = System.getProperty( "line.separator" );
	static
	{
		if ( lineSeparator == null )
		{
			lineSeparator = "\015012";//No I18N
		}
	}
	/**
	 * categorise a character for the finite state machine.
	 *
	 * @param c      the character to categorise
	 * @return integer representing the character's category.
	 */
	private int categorise( char c )
	{
		switch ( c )
		{
			case ' ':
			case '\r':
			case 0xff:
				return WHITESPACE;

			case '\n':
				return EOL; /* artificially applied to end of line */



			default:
				if ( c == quote )
				{
					return QUOTE;
				}
				else if ( c == separator /* dynamically determined so can't use as case label */ )
				{
					return SEPARATOR;
				}
				/* do our tests in crafted order, hoping for an early return */
				else if ( '!' <= c && c <= '~' )
				{
					return ORDINARY;
				}
				else if ( 0x00 <= c && c <= 0x20 )
				{
					return WHITESPACE;
				}
				else if ( Character.isWhitespace(c) )
				{
					return WHITESPACE;
				}
				else
				{
					return ORDINARY;
				}
		}
	}

	/**
	 * parser: We are in blanks before the field.
	 */
	private static final int SEEKING_START = 0;

	/**
	 * parser: We are in the middle of an ordinary field.
	 */
	private static final int IN_PLAIN = 1;

	/**
	 * parser: e are in middle of field surrounded in quotes.
	 */
	private static final int IN_QUOTED = 2;

	/**
	 * parser: We have just hit a quote, might be doubled
	 * or might be last one.
	 */
	private static final int AFTER_END_QUOTE = 3;

	/**
	 * parser: We are in blanks after the field looking for the separator
	 */
	private static final int SKIPPING_TAIL = 4;

	/**
	 * state of the parser's finite state automaton.
	 */

	/**
	 * The line we are parsing.
	 * null means none read yet.
	 * Line contains unprocessed chars. Processed ones are removed.
	 */
	private String line = null;

	/**
	 * How many lines we have read so far.
	 * Used in error messages.
	 */
	private int lineCount = 0;
	/**
	 * false means next EOL marks an empty field
	 * true means next EOL marks the end of all fields.
	 */
	private boolean allFieldsDone = true;


	public HashMap getCSVHeaders() throws Exception
	{
		if( data == null )
		{
			data = new LinkedHashMap();

			Vector headers = getAllFieldsInLine();

			for( Object header : headers )
			{
				data.put(header.toString(), "-");//No I18N
			}
		}
		else
		{
			throw new Exception("Can't traverse back to fetch the header information!!!");//No I18N
		}
		return data;
	}

	public HashMap getNextRow() throws Exception
	{
		if( data == null )
		{
			data = new LinkedHashMap();

			Vector headers = getAllFieldsInLine();

			for( Object header : headers )
			{
				data.put(header.toString(), "-");//No I18N
			}
		}
		else
		{
			String columnHeader = null;
			String columnValue = null;

			while( true )
			{
				boolean isNotNull = false;

				for( java.util.Iterator it = data.keySet().iterator(); it.hasNext(); )
				{
					columnValue = get();

					if( columnValue == null )
					{
						break;
					}
					isNotNull = true;
					columnHeader = (String) it.next();
					data.put(columnHeader, columnValue);
				}
				if( isNotNull )
				{
					break;
				}
			}
		}
		return data;
	}
	/**
	 * Get all fields in the line
	 *
	 * @return Array of strings, one for each field.
	 *         Possibly empty, but never null.
	 * @exception EOFException
	 * @exception IOException
	 */
	public Vector getAllFieldsInLine() throws EOFException, IOException
	{
		Vector fields = new Vector();
		do
		{
			String field = get();
			if ( field == null )
			{
				break;
			}
			fields.add( field );
		}
		while ( true );
		return fields;
	}
	/**
	 * Read one field from the CSV file
	 *
	 * @return String value, even if the field is numeric.  Surrounded
	 *         and embedded double quotes are stripped.
	 *         possibly "".  null means end of line.
	 *
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 *
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 */
	public String get() throws EOFException, IOException
	{

		StringBuffer field = new StringBuffer( allowMultiLineFields ? 512 : 64 );
		/* we implement the parser as a finite state automaton with five states. */


		int state = SEEKING_START; /* start seeking, even if partway through a line */
		/* don't need to maintain state between fields. */

lineLoop:
		while ( true )
		{

			getLineIfNeeded();

charLoop:
			/* loop for each char in the line to find a field */
			/* guaranteed to leave early by hitting EOL */
			for ( int i=0; i<line.length(); i++ )
			{
				char c = line.charAt(i);
				int category = categorise(c);
				switch ( state )
				{
					case SEEKING_START:
						{  /* in blanks before field */
							switch ( category )
							{
								case WHITESPACE:
									/* ignore */
									break;

								case QUOTE:
									state = IN_QUOTED;
									break;

								case SEPARATOR:
									/* end of empty field */
									line = line.substring(i+1);
									return "";

								case EOL:
									/* end of line */
									if ( allFieldsDone )
									{
										/* null to mark end of line */
										line = null;
										return null;
									}
									else
									{
										/* empty field, usually after a comma */
										allFieldsDone = true;
										line = line.substring(i);
										return "";
									}

								case ORDINARY:
									field.append(c);
									state = IN_PLAIN;
									break;
							}
							break;
						}
					case IN_PLAIN:
						{  /* in middle of ordinary field */
							switch ( category )
							{
								case QUOTE:
									field.append(c);
									break;
									//line = null;
									//throw new IOException("Malformed CSV stream. Missing quote at start of field on line "+lineCount);//No I18N

								case SEPARATOR:
									/* done */
									line = line.substring(i+1);
									return maybeTrim( field.toString() );

								case EOL:
									line = line.substring(i); /* push EOL back */
									allFieldsDone = true;
									return maybeTrim( field.toString() );

								case WHITESPACE:
									field.append(' ');
									break;

								case ORDINARY:
									field.append(c);
									break;
							}
							break;
						}

					case IN_QUOTED:
						{  /* in middle of field surrounded in quotes */
							switch ( category )
							{

								case QUOTE:
									state = AFTER_END_QUOTE;
									break;

								case EOL:
									if ( allowMultiLineFields )
									{
										field.append( lineSeparator );
										// we are done with that line, but not with the field.
										// We don't want to return a null
										// to mark the end of the line.
										line = null;
										// will read next line and seek the end of the quoted field.
										// with state = IN_QUOTED.
										break charLoop;
									}
									else
									{
										// no multiline fields allowed
										allFieldsDone = true;
										line = null;
										throw new IOException("Malformed CSV stream. Missing quote (\") after field on line "+lineCount);//No I18N
									}
								case WHITESPACE:
									field.append(' ');
									break;

								case SEPARATOR:
								case ORDINARY:
									field.append(c);
									break;
							}
							break;
						}

					case AFTER_END_QUOTE:
						{

							switch ( category )
							{

								case QUOTE:
									/* was a double quote, e.g. a literal " */
									field.append(c);
									state = IN_QUOTED;
									break;

								case SEPARATOR :
									/* we are done with field.*/
									line = line.substring(i+1);
									return maybeTrim( field.toString() );

								case EOL:
									line = line.substring(i); /* push back eol */
									allFieldsDone = true;
									return maybeTrim( field.toString() );

								case WHITESPACE:
									/* ignore trailing spaces up to separator */
									state = SKIPPING_TAIL;
									break;

								case ORDINARY:
									line = null;
									throw new IOException("Malformed CSV stream, missing separator after fieldon line "+lineCount);//No I18N
							}
							break;
						}

					case SKIPPING_TAIL:
						{
							/* in spaces after field seeking separator */

							switch ( category )
							{

								case SEPARATOR :
									/* we are done.*/
									line = line.substring(i+1);
									return maybeTrim( field.toString() );

								case EOL:
									line = line.substring(i); /* push back eol */
									allFieldsDone = true;
									return maybeTrim( field.toString() );

								case WHITESPACE:
									/* ignore trailing spaces up to separator */
									break;

								case QUOTE:
								case ORDINARY:
									line = null;
									throw new IOException("Malformed CSV stream, missing separator after field on line "+lineCount);//No I18N
							}
							break;
						}

				} // end switch(state)

			} // end charLoop
		} // end lineLoop
	} // end get

	/**
	 * Trim the string, but only if we are in
	 * trimming mode.
	 *
	 * @param s      String to be trimmed.
	 * @return String or trimmed string.
	 */
	private String maybeTrim(String s )
	{
		if ( trim )
		{
			return s.trim();
		}
		else
		{
			return s;
		}
	}

	/**
	 * Read one integer field from the CSV file
	 *
	 * @return int value, empty field returns 0, as does end of line.
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 * @exception NumberFormatException, if field does not contain a well-formed int.
	 */
	public int getInt() throws EOFException, IOException, NumberFormatException
	{
		String s = get();
		if ( s == null )
		{
			return 0;
		}
		if ( ! trim )
		{
			s = s.trim();
		}
		if ( s.length() == 0 )
		{
			return 0;
		}
		return Integer.parseInt( s );
	}


	/**
	 * Read one long field from the CSV file
	 *
	 * @return long value, empty field returns 0, as does end of line.
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 * @exception NumberFormatException, if field does not contain a well-formed int.
	 */
	public long getLong() throws EOFException, IOException, NumberFormatException
	{
		String s = get();
		if ( s == null )
		{
			return 0;
		}
		if ( ! trim )
		{
			s = s.trim();
		}

		if ( s.length() == 0 )
		{
			return 0;
		}
		return Long.parseLong( s );
	}


	/**
	 * Read one float field from the CSV file.
	 *
	 * @return float value, empty field returns 0, as does end of line.
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 * @exception NumberFormatException, if field does not contain a well-formed int.
	 */
	public float getFloat() throws EOFException, IOException, NumberFormatException
	{
		String s = get();
		if ( s == null )
		{
			return 0;
		}
		if ( ! trim )
		{
			s = s.trim();
		}
		if ( s.length() == 0 )
		{
			return 0;
		}
		return Float.parseFloat( s );
	}

	/**
	 * Read one double field from the CSV file.
	 *
	 * @return houble value, empty field returns 0, as does end of line.
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 * @exception NumberFormatException, if field does not contain a well-formed int.
	 */
	public double getDouble() throws EOFException, IOException, NumberFormatException
	{
		String s = get();
		if ( s == null )
		{
			return 0;
		}
		if ( ! trim )
		{
			s = s.trim();
		}
		if ( s.length() == 0 )
		{
			return 0;
		}
		return Double.parseDouble( s );
	}

	/**
	 * Make sure a line is available for parsing.
	 * Does nothing if there already is one.
	 *
	 * @exception EOFException
	 */
	private void getLineIfNeeded() throws EOFException, IOException
	{

		if ( line == null )
		{
			if ( r == null )
			{
				throw new IllegalArgumentException("attempt to use a closed CSVReader");//No I18N
			}
			allFieldsDone = false;
			line = r.readLine();  /* this strips platform specific line ending */
			if ( line == null )   /* null means EOF, yet another inconsistent Java convention. */
			{
				throw new EOFException();
			}
			else
			{
				line += '\n'; /* apply standard line end for parser to find */
				lineCount++;
			}
		}
	}

	/**
	 * Skip over fields you don't want to process.
	 *
	 * @param fields How many field you want to bypass reading.
	 *               The newline counts as one field.
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 */
	public void skip(int fields) throws EOFException, IOException
	{

		if ( fields <= 0 )
		{
			return;
		}
		for ( int i=0; i<fields; i++ )
		{
			// throw results away
			get();
		}
	}

	/**
	 * Skip over remaining fields on this line you don't want to process.
	 *
	 * @exception EOFException
	 *                   at end of file after all the fields have
	 *                   been read.
	 * @exception IOException
	 *                   Some problem reading the file, possibly malformed data.
	 */
	public void skipToNextLine() throws EOFException, IOException
	{

		if ( line == null )
		{
			getLineIfNeeded();
		}
		line = null;
	}

	/**
	 * Close the Reader.
	 */
	public void close() throws IOException
	{
		if ( r != null )
		{
			r.close();
			r = null;
			data = null;
		}
	}

	/**
	 * Test driver
	 *
	 * @param args   not used
	 */
	public static void main(String[] args)
	{
		if ( DEBUGGING )
		{
			try
			{
				// read test file
				CSVReader csv = new CSVReader(new FileReader("test.csv"), ',', '\"', true, false);//No I18N
				try
				{
					while ( true )
					{
						logger.log( Level.FINER, "--> " + csv.get());
					}
				}
				catch ( EOFException  e )
				{
					System.out.println(e.getMessage());
				}
				csv.close();
			}
			catch ( IOException  e )
			{
				logger.log( Level.SEVERE, e.getMessage() );
				System.out.println(e.getMessage());
				//                e.printStackTrace();
			}
		} // end if
	} // end main
} // end CSVReader class.

