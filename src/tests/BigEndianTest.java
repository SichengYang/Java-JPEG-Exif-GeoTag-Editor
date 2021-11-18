package tests;

import endian.BigEndian;

public class BigEndianTest {

	public static void main(String[] args) {
		byte data1 = (byte)(0xF0);
		byte data2 = (byte)(0x0F);
		byte data3 = (byte)(0xA0);
		byte data4 = (byte)(0x0A);
		
		System.out.println(BigEndian.getLong32(data1, data2, data3, data4));
		System.out.println(BigEndian.getInt16(data1, data2));
		System.out.println(BigEndian.getInt32(data1, data2, data3, data4));
		System.out.println(BigEndian.getSignedShort(data1, data2));
	}

}
