package io.metamask.nativeModules;

import java.util.*;
import java.io.*;

public class Util
{

	private final static char[] _TABHEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	/** Line break string of current system, same with <code>System.lineSeparator()</code> **/
	public final static String BR = System.lineSeparator();

	/**
	 * Same with <code>a2hex(null, arr, 0, arr.length)</code>
	 * @param arr       byte array to be converted
	 * @return          Hexadecimal text string
	 */
	public static String a2hex(byte[] arr                        ) {return a2hex(null, arr, 0, arr.length);}

	/**
	 * Same with <code>a2hex(d, arr, 0, arr.length)</code>
	 * @param d         Delimiter string, null for no delimiter between each byte text
	 * @param arr       byte array to be converted
	 * @return          Hexadecimal text string
	 */
	public static String a2hex(String d, byte[] arr              ) {return a2hex(d, arr, 0, arr.length);}

	/**
	 * Same with <code>a2hex(null, arr, offset, length)</code>
	 * @param arr       byte array to be converted
	 * @param offset    the first index to <code>arr</code> to be converted
	 * @param length    number of bytes to be converted
	 * @return          Hexadecimal text string
	 */
	public static String a2hex(byte[] arr, int offset, int length) {return a2hex(null, arr, offset, length);}

	/**
	 * Get hexadecimal formatted text representation of a byte array, with specified delimiter.
	 * Returned text will line up bytes one by one in hexadecimal, split by delimiter. Very useful
	 * delimiter is " ", ", 0x", ", (byte)0x". There is no delimiter after the last byte text.
	 * @param d         Delimiter string, null for no delimiter between each byte text
	 * @param arr       byte array to be converted
	 * @param offset    the first index to <code>arr</code> to be converted
	 * @param length    number of bytes to be converted
	 * @return          Hexadecimal text string
	 */
	public static String a2hex(String d, byte[] arr, int offset, int length)
	{
		if (length<0) return null;
		if (length==0) return "";
		if (d==null || d.length()==0) {
			char[] ca = new char[length * 2];
			int idx = 0;
			while (length-- > 0) {
				int v = arr[offset++] & 0xFF;
				ca[idx++] = _TABHEX[v >> 4];
				ca[idx++] = _TABHEX[v & 0x0F];
			}
			return new String(ca);
		}
		char[] dd = d.toCharArray();
		char[] ca = new char[length * (2+dd.length) - dd.length];
		int idx = 0;
		while (length-- > 0) {
			int v = arr[offset++] & 0xFF;
			ca[idx++] = _TABHEX[v >> 4];
			ca[idx++] = _TABHEX[v & 0x0F];
			if (length>0) for (int i=0; i<dd.length; i++) ca[idx++] = dd[i];
		}
		return new String(ca);
	}

	/**
	 * Get hexadecimal formatted text of a long value, unsigned, left pad with '0'
	 * @param l    long value
	 * @return     16 digits Hexadecimal formatted text string
	 */
	public static String hex(long l) {return padLeft(Long.toHexString(l).toUpperCase(), 16, '0');}

	/**
	 * Get hexadecimal formatted text of an integer value, specified digits, unsigned, left pad with
	 * '0', if hexadecimal text of integer value is longer than <code>w</code>, original hexadecimal
	 * text will be returned.
	 * @param i    integer value
	 * @param w    minimal number of digits
	 * @return     <code>w</code> or more digits Hexadecimal formatted text string
	 */
	public static String hex(int i, int w) {return padLeft(Integer.toHexString(i).toUpperCase(), w, '0');}

	/**
	 * Get hexadecimal formatted text of an integer value, unsigned, left pad with '0'
	 * @param i    integer value
	 * @return     8 digits Hexadecimal formatted text string
	 */
	public static String hex(int i) {return hex(i, 8);}

	/**
	 * Get hexadecimal formatted text of a byte value, unsigned, left pad with '0'
	 * @param b    byte value
	 * @return     2 digits Hexadecimal formatted text string
	 */
	public static String hex(byte b) {return hex(b&0xFF, 2);}

	/**
	 * Get hexadecimal formatted text of a short value, unsigned, left pad with '0'
	 * @param s    short value
	 * @return     4 digits Hexadecimal formatted text string
	 */
	public static String hex(short s) {return hex(s&0xFFFF, 4);}

	/**
	 * Same with call <code>hex2ba(s, 0, s.length())</code>
	 * @param s    String to be converted
	 * @return     converted byte array
	 */
	public static byte[] hex2ba(String s) {return hex2ba(s, 0, s.length());}

	/**
	 * Convert a hexadecimal formatted string to byte array, any character with ASCII code smaller
	 * than 0x20 (a Control Character such as \t, \n, \r) are treated as delimiter as well as space
	 * <code>' '</code>. A continuous hexadecimal characters group will be parsed to byte by byte,
	 * if number of characters in one group is odd, the first character will be treated as a single
	 * byte, for example "12345" will be pared to <code>0x01, 0x23, 0x45</code>. "1234" will be
	 * parsed to <code>0x12, 0x34</code>. "AbC 12 3\n456\r78\t9F" will be parsed to <code>0x0A,
	 * 0xBC, 0x12, 0x03, 0x04, 0x56, 0x78, 0x9F</code>
	 * @param s         Text to be converted
	 * @param offset    position of <code>s</code> to be parsed starts with
	 * @param length    character numbers to be parsed
	 * @return          converted byte array, null if text contains invalid character
	 */
	public static byte[] hex2ba(String s, int offset, int length)
	{
		if (s==null) return null;
		char[] cc = s.toCharArray();
		byte[] bb = new byte[(cc.length+1)/2];
		int ic = cc.length;
		int ib = bb.length-1;
		int b = -1;
		while (ic-->0)
		{
			char c = cc[ic];
			if (c>'f') return null;
			if (c<=' ') {if (b>=0) {bb[ib--] = (byte)b; b = -1;} continue;}

			if (c>='0' && c<='9') c -= '0'; else
			if (c>='A' && c<='F') c -= ('A'-10); else
			if (c>='a') c -= ('a'-10); else return null;

			if (b>=0) {bb[ib--] = (byte)((c<<4)+b); b = -1;}
			else b = c;
		}
		if (b>=0) bb[ib] = (byte)b; else ib++;

		byte[] r = new byte[bb.length-ib];
		System.arraycopy(bb, ib, r, 0, r.length);
		return r;
	}

	/**
	 * <H1>Left Alignment Version</H1>
	 * Convert a hexadecimal formatted string to byte array, any character with ASCII code smaller
	 * than 0x20 (a Control Character such as \t, \n, \r) are treated as delimiter as well as space
	 * <code>' '</code>. A continuous hexadecimal characters group will be parsed to byte by byte,
	 * <B>if number of characters in one group is odd, the first 2 characters will be treated as a
	 * byte, and the last single character will be treated as single byte,</B>
	 * for example "12345" will be pared to <code>0x12, 0x34, 0x05</code>. "1234" will be
	 * parsed to <code>0x12, 0x34</code>. "AbC 12 3\n456\r78\t9F" will be parsed to <code>0xAB,
	 * 0x0C, 0x12, 0x03, 0x45, 0x06, 0x78, 0x9F</code>
	 * @param s         Text to be converted
	 * @param offset    position of <code>s</code> to be parsed starts with
	 * @param length    character numbers to be parsed
	 * @return          converted byte array, null if text contains invalid character
	 */
	public static byte[] hex2baLA(String s, int offset, int length)
	{
		if (s==null) return null;
		char[] cc = s.toCharArray();
		byte[] bb = new byte[(cc.length+1)/2];
		int lc = cc.length;
		int lb = bb.length-1;
		int b = -1;
		int ic = 0;
		int ib = 0;
		while (ic<lc)
		{
			char c = cc[ic++];
			if (c>'f') return null;
			if (c<=' ') {if (b>=0) {bb[ib++] = (byte)b; b = -1;} continue;}

			if (c>='0' && c<='9') c -= '0'; else
			if (c>='A' && c<='F') c -= ('A'-10); else
			if (c>='a') c -= ('a'-10); else return null;

			if (b>=0) {bb[ib++] = (byte)((b<<4)+c); b = -1;}
			else b = c;
		}
		if (b>=0) bb[ib++] = (byte)b;
		byte[] r = new byte[ib];
		System.arraycopy(bb, 0, r, 0, ib);
		return r;
	}

	/**
	 * Get display-width of a string. Display-with is how many english
	 * characters width of the text string. For example a chinese character will occupy 2 english
	 * characters width. The unit is english character width.
	 * @param s        String to be counted
	 * @return         Display-width of <code>s</code>, 0 if <code>s</code> is null
	 */
	public static int getStringWidth(String s)
	{
		int w;
		if (s==null) return 0;
		char[] cc = s.toCharArray();
		w = cc.length;
		for (int i=0; i<cc.length; i++)
		{
			int c = cc[i]&0xFFFF;
			switch (c)
			{
				case 162:
				case 163:
				case 167:
				case 168:
				case 171:
				case 172:
				case 175:
				case 176:
				case 177:
				case 180:
				case 181:
				case 182:
				case 183:
				case 184:
				case 187:
				case 215:
				case 247:
				case 65381:
					w++;
					continue;
			}
			if (c>255 && (c<65377 || c>65439)) w++;
		}
		return w;
	}

	/**
	 * Left padding with specific char to defined display-width. Display-with is how many english
	 * characters width of the text string. For example a chinese character will occupy 2 english
	 * characters width. If the padding character is double-with character, the returned text may be
	 * one english character longer than expected <code>width</code> because character integrity
	 * cannot be broken. If the original width is longer than expected <code>width</code>, original
	 * text string will be returned.
	 * @param s        String to be padded
	 * @param width    Expected display-width
	 * @param c        Padding character
	 * @return         Left padded text
	 */
	public static String padLeft(String s, int width, char c) {String d=("A").replace('A', c); int x=getStringWidth(d); width-=getStringWidth(s); while(width>0) {s = d + s; width-=x;} return s;}

	/**
	 * Left padding with <code>' '</code> to defined display-width. Display-with is how many english
	 * characters width of the text string. For example a chinese character will occupy 2 english
	 * characters width. If the original width is longer than expected <code>width</code>, original
	 * text string will be returned.
	 * @param s        String to be padded
	 * @param width    Expected display-width
	 * @return         Left padded text
	 */
	public static String padLeft(String s, int width) {return padLeft(s, width, ' ');}

	/**
	 * Right padding with specific char to defined display-width. Display-with is how many english
	 * characters width of the text string. For example a chinese character will occupy 2 english
	 * characters width. If the padding character is double-with character, the returned text may be
	 * one english character longer than expected <code>width</code> because character integrity
	 * cannot be broken. If the original width is longer than expected <code>width</code>, original
	 * text string will be returned.
	 * @param s        String to be padded
	 * @param width    Expected display-width
	 * @param c        Padding character
	 * @return         Right padded text
	 */
	public static String padRight(String s, int width, char c) {String d=("A").replace('A', c); int x=getStringWidth(d); width-=getStringWidth(s); while(width>0) {s += d; width-=x;} return s;}

	/**
	 * Right padding with <code>' '</code> to defined display-width. Display-with is how many english
	 * characters width of the text string. For example a chinese character will occupy 2 english
	 * characters width. If the original width is longer than expected <code>width</code>, original
	 * text string will be returned.
	 * @param s        String to be padded
	 * @param width    Expected display-width
	 * @return         Right padded text
	 */
	public static String padRight(String s, int width) {return padRight(s, width, ' ');}

	/**
	 * Delay specific milli-seconds in the current thread, without exception throw out.
	 * @param ms    Milli-seconds to be delayed
	 */
	public static void delay(long ms)
	{
		try{Thread.sleep(ms);}catch(Exception e){}
	}


	/**
	 * Get text string of a <code>Throwable</code> call stack
	 * @param t    <code>Throwable</code> object
	 * @return     Text of call stack, null if <code>t</code> is null
	 */
	public static String getStackTrace(Throwable t)
	{
		if (t==null) return null;
		StringWriter w = new StringWriter();
		t.printStackTrace(new PrintWriter(w));
		return w.toString();
	}


	/**
	 * Single forward link structure
	 **/
	public interface SingleLink
	{
		/**
		 * Get next link node of the current link node
		 * @return    Next link node, null if no next node
		 */
		public SingleLink getNext();
		/**
		 * Set next link node of the current link node
		 * @param next    Next link node, null if no next node
		 */
		public void setNext(SingleLink next);
	}

	/**
	 * Add a link node to tail of the specific link
	 * @param start    The ling node that the link starts with
	 * @param link     The link node to be added
	 * @return         New start link node of the link
	 **/
	public static SingleLink append(SingleLink start, SingleLink link)
	{
		if (start==null) return link;
		SingleLink s = start;
		while (s.getNext()!=null) s = s.getNext();
		s.setNext(link);
		return start;
	}

	/**
	 * Find a link node in a specific link
	 * @param start    The ling node that the link starts with
	 * @param link     The link node to be added
	 * @return         If not found returns <code>null</code>;<br>
	 *                 returns previous link node if found;<br>
	 *                 if <code>start</code> is <code>link</code> returns <code>start</code><br>
	 * <B>Note</B><br>
	 * Caller MUST check if <code>start</code> is <code>link</code> before call this method
	 **/
	public static SingleLink seek(SingleLink start, SingleLink link)
	{
		if (start==link) return start;
		SingleLink s = start;
		while (s.getNext()!=null) if (s.getNext()==link) return s; else s = s.getNext();
		return null;
	}

	/**
	 * Remove a link node from a link
	 * @param start    The ling node that the link starts with
	 * @param link     The link node to be removed
	 * @return         New start link node of the link
	 **/
	public static SingleLink remove(SingleLink start, SingleLink link)
	{
		if (start==null || link==null) return start;
		SingleLink s = start;
		SingleLink p = null;
		while(s!=link)
		{
			p = s;
			s = s.getNext();
			if (s==null) return start;
		}
		if (p==null) return link.getNext();
		p.setNext(link.getNext());
		return start;
	}

	/**
	 * A milli-second level of stop clock like utility
	 */
	public static class Tiktak
	{
		private long _t0;

		/**
		 * Create an instance and make a tick, "<I>Sounds like press the stop clock button</I>"
		 */
		public Tiktak(){tik();}

		/**
		 * Make a tick, "<I>Sounds like press the stop clock button</I>", mark the current time and
		 * replace the last mark
		 */
		public void tik() {_t0 = System.currentTimeMillis();}

		/**
		 * Make a tak, "<I>Sounds like release the stop clock button</I>", calculate milli-seconds
		 * of distance between current time and last marked time, mark the current time.
		 * @return    The time distance in milli-second
		 */
		public long tak() {long t = _t0; _t0=System.currentTimeMillis(); return (long)(_t0-t);}

		/**
		 * Make a tak and returns quartile formatted milli-seconds unit distance text string, such
		 * as "1,234,567 ms"
		 * @return    Quartile format mill-seconds time distance string
		 */
		public String takms() {return quartile((""+tak()).toCharArray()) + " ms";}

		/**
		 * Make a tak and returns quartile formatted seconds unit distance text string, such
		 * as "1,234.007 s"
		 * @return    Quartile format mill-seconds time distance string
		 */
		public String taks()
		{
			String s = quartile((""+tak()).toCharArray());
			if (s.length()<4) return ("0.00").substring(0, 5-s.length()) + s + " s";
			return s.substring(0, s.length()-4) + "." + s.substring(s.length()-3) + " s";
		}
	}

	/**
	 * Make a quartile formatted text for an integer value, for example "2", "1,234", "-5", "-1,234"
	 * @param v    integer value
	 * @return     Quartile formatted string
	 */
	public static String quartile(int v) {return quartile((""+v).toCharArray());}

	/**
	 * Make a quartile formatted text for a long value, for example "1,234,567", "-1,234,567"
	 * @param v    long value
	 * @return     Quartile formatted string
	 */
	public static String quartile(long v) {return quartile((""+v).toCharArray());}

	/**
	 * Convert to quartile formatted text for a decimal number text stored in a char array, for
	 * example "1,234,567", "-1,234,567", invalid format of number text will cause return null.
	 * @param cc    char array contains number text
	 * @return      Quartile formatted string
	 */
	public static String quartile(char[] cc)
	{
		if (cc==null || cc.length==0 || (cc.length==1 && cc[0]=='-')) return null;
		int n = (cc[0]=='-')?1:0;
		char[] cs = new char[cc.length + ((cc.length-n-1) / 3)];
		int xc = cc.length;
		int xs = cs.length;
		int x = 0;
		while(xc-->n)
		{
			if (x==3) {cs[--xs] = ','; x = 0;}
			char c = cc[xc];
			if (c<'0' || c>'9') return null;
			cs[--xs] = c;
			x++;
		}
		if (n==1) cs[0] = '-';
		return (new String(cs));
	}

	/**
	 * Make a formatted text for an integer, with specified digits, if not sufficient left padding
	 * with "0", if digits of integer decimal format is longer than specified number, the original
	 * format of integer will be returned. This method support negative integer, but the "-" is not
	 * counted in digit number.
	 * @param width    Expected digits number
	 * @param v        integer value
	 * @return         Formatted text
	 */
	public static String digits(int width, int v)
	{
		String s = "" + v;
		String r = "";
		if (v<0) {r = "-"; s = s.substring(1);}
		if (s.length()>=width) return r+s;
		s = "000000000" + s;
		return r + s.substring(s.length()-width);
	}

	/**
	 * Get the simplified Time Stamp text for current time, format is "TMZ YYYYMMDD-HH:Mi:SS:MLS",
	 * TMZ is 3-characters short name of Time zone, MLS is 3-digits milli-second.
	 * @return    Formatted Time Stamp text
	 */
	public static String getCompactTimeStamp() {return getCompactTimeStamp(null);}

	/**
	 * Get the simplified Time Stamp text for a given time, format is "TMZ YYYYMMDD-HH:Mi:SS:MLS",
	 * TMZ is 3-characters short name of Time zone, MLS is 3-digits milli-second.
	 * @param c    Time to be represented
	 * @return     Formatted Time Stamp text
	 */
	public static String getCompactTimeStamp(Calendar c)
	{
		if (c==null) c = Calendar.getInstance();
		String s = c.getTimeZone().getID() + " ";
		s += digits(4, c.get(Calendar.YEAR));
		s += digits(2, c.get(Calendar.MONTH));
		s += digits(2, c.get(Calendar.DAY_OF_MONTH)) + "-";
		s += digits(2, c.get(Calendar.HOUR_OF_DAY)) + ":";
		s += digits(2, c.get(Calendar.MINUTE)) + ":";
		s += digits(2, c.get(Calendar.SECOND)) + ":";
		s += digits(3, c.get(Calendar.MILLISECOND));
		return s;
	}

	/**
	 * Copy bytes from <code>dat</code> to <code>buf</code>, start index is 0, copied length is
	 * minimal one between <code>dat.length</code> and <code>buf.length</code>.
	 * @param dat    Source byte array
	 * @param buf    Destination byte array
	 * @return       Copied bytes length, returns 0 if any of parameters is null
	 */
	public static int fetchBytes(byte[] dat, byte[] buf)
	{
		if (dat==null || buf==null) return 0;
		System.arraycopy(dat, 0, buf, 0, dat.length);
		return dat.length;
	}



	/**
	 * Get hexadecimal formatted text representation in form of LV structure from a byte array.
	 * Returned text will line up bytes one by one in hexadecimal, L is length of converted
	 * data, 0 to 256 are supported. Same with <code>LV(data, 0, data.length)</code>
	 * @param data      byte array to be converted
	 * @return          Hexadecimal text string, if <code>data.length</code> is greater than
	 *                  256, null will be returned. If <code>data.length</code> is 256, L byte
	 *                  will be set to zero.
	 */
	public static String LV(byte[] data) {return LV(data, 0, data.length);}

	/**
	 * Get hexadecimal formatted text representation in form of LV structure from a byte array.
	 * Returned text will line up bytes one by one in hexadecimal, L is length of converted
	 * data, 0 to 256 are supported.
	 * @param data      byte array to be converted
	 * @param offset    the first index to <code>arr</code> to be converted
	 * @param length    number of bytes to be converted
	 * @return          Hexadecimal text string, if <code>length</code> is negative or greater
	 *                  than 256, null will be returned. If <code>length</code> is 256, L byte
	 *                  will be set to zero.
	 */
	public static String LV(byte[] data, int offset, int length)
	{
		if (data==null || length<0 || length>256) return null;
		String s = a2hex(data, offset, length);
		if (s==null) return null;
		if (s.length()!=(length*2)) return null;
		return hex((byte)length) + s;
	}

	/**
	 * Get hexadecimal formatted text in BER-TLV structure from a byte array.
	 * Returned text will line up bytes one by one in hexadecimal, L is length of converted
	 * data. Same with <code>BERLV(data, 0, data.length)</code>
	 * @param data      byte array to be converted
	 * @return          Hexadecimal text string of BER-TLV format
	 */
	public static String BERLV(byte[] data) {return BERLV(data, 0, data.length);}

	/**
	 * Get hexadecimal formatted text in BER-TLV structure from a byte array.
	 * Returned text will line up bytes one by one in hexadecimal, L is length of converted
	 * data.
	 * @param data      byte array to be converted
	 * @param offset    the first index to <code>arr</code> to be converted
	 * @param length    number of bytes to be converted
	 * @return          Hexadecimal text string of BER-TLV format, null if
	 *                  <code>length</code> is negative.
	 */
	public static String BERLV(byte[] data, int offset, int length)
	{
		if (data==null || length<0) return null;
		String s = a2hex(data, offset, length);
		if (s==null) return null;
		if (s.length()!=(length*2)) return null;
		String h = hex(length); // 8 digits
		if (length<0x100) h = ((length<0x80)?"":"81") + h.substring(6); else
		if (length<0x10000) h = ((length<0x8000)?"":"82") + h.substring(4); else
		if (length<0x1000000) h = ((length<0x800000)?"":"83") + h.substring(2); else
		h = "84" + h;
		return h + s;
	}

	public static void main(String[] args){
		byte[] bb = hex2ba(args[0]);
		System.out.println(a2hex(" ", bb));
		bb = hex2baLA(args[0], 0, args[0].length());
		System.out.println(a2hex(" ", bb));
	}

}