package org.commons.tests;

import org.commons.endian.SmallEndian;

public class SmallEndianTest {
	public static void main(String[] agrs)
	{
		byte data1 = (byte)(0xF0);
		byte data2 = (byte)(0x0F);
		byte data3 = (byte)(0xA0);
		byte data4 = (byte)(0x0A);
		
		System.out.println(SmallEndian.getLong32(data1, data2, data3, data4));
		System.out.println(SmallEndian.getInt16(data1, data2));
		System.out.println(SmallEndian.getInt32(data2, data1, data4, data3));
		System.out.println(SmallEndian.getSignedShort(data1, data2));
	}
}
