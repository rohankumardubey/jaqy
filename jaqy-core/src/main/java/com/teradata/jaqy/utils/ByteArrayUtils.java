/*
 * Copyright (c) 2017 Teradata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.jaqy.utils;

import java.io.PrintStream;

/**
 * @author	Heng Yuan
 */
public class ByteArrayUtils
{
	private final static String[] s_byteString = new String[256];

	private static String getByteString (byte b)
	{
		int index = b & 0xff;
		if (s_byteString[index] == null)
		{
			char[] ch = new char[3];
			ch[0] = ' ';
			int v = index >> 4;
			ch[1] = (char) (v < 10 ? ('0' + v) : ('a' + v - 10));
			v = index & 0x0f;
			ch[2] = (char) (v < 10 ? ('0' + v) : ('a' + v - 10));
			s_byteString[index] = new String (ch);
		}
		return s_byteString[index];
	}

	private static void printAscii (PrintStream pw, byte[] bytes, int start)
	{
		int num = bytes.length - start;
		char[] ch = new char[num + 4];
		ch[0] = ' ';
		ch[1] = ' ';
		ch[2] = '|';
		ch[num + 3] = '|';
		int j = 3;
		for (int i = start; i < bytes.length && i < (start + 16); ++i)
		{
			byte b = bytes[i - start];
			if (b >= ' ' && b < 0x7f)
				ch[j] = (char)b;
			else
				ch[j] = '.';
			++j;
		}
		pw.print (new String (ch));
	}

	private static String getAddress (long address)
	{
		String str = Long.toHexString (address);
		if (str.length () < 8)
		{
			str += "00000000".substring (str.length ());
		}
		return str;
	}

	public static void print (PrintStream pw, byte[] bytes, long startAddress)
	{
		int rowStart = 0;
		for (int i = 0; i < bytes.length; ++i)
		{
			if (i % 16 == 0)
			{
				if (i > 0)
				{
					printAscii (pw, bytes, rowStart);
					pw.println ();
				}
				rowStart = i;
				pw.print (getAddress (startAddress + i));
			}
			if (i % 8 == 0)
			{
				pw.print (' ');
			}
			pw.print (getByteString (bytes[i]));
		}

		// even the last line
		int remain = bytes.length % 16;
		if (remain > 0)
		{
			for (int i = remain; i < 16; ++i)
			{
				if (i % 8 == 0)
				{
					pw.print (' ');
				}
				pw.print ("   ");
			}
			printAscii (pw, bytes, rowStart);
		}

		pw.println ();
	}
}
