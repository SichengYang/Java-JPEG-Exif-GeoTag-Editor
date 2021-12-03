package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import endian.SmallEndian;

public class SmallEndianTest {
	
	@Test
	public void SmallEndianTest()
	{
		byte data1 = (byte)(0xF0);
		byte data2 = (byte)(0x0F);
		byte data3 = (byte)(0xA0);
		byte data4 = (byte)(0x0A);

		assertEquals(178262000, SmallEndian.getLong32(data1, data2, data3, data4));
		assertEquals(4080, SmallEndian.getInt16(data1, data2));
		assertEquals(178262000, SmallEndian.getInt32(data1, data2, data3, data4));
		assertEquals(4080, SmallEndian.getSignedShort(data1, data2));
	}
}
