package tests;

import endian.BigEndian;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BigEndianTest {
	
	@Test
	public void BigEndianTest() {
		byte data1 = (byte)(0xF0);
		byte data2 = (byte)(0x0F);
		byte data3 = (byte)(0xA0);
		byte data4 = (byte)(0x0A);
		
		assertEquals(4027555850L, BigEndian.getLong32(data1, data2, data3, data4));
		assertEquals(61455, BigEndian.getInt16(data1, data2));
		assertEquals(-267411446, BigEndian.getInt32(data1, data2, data3, data4));
		assertEquals(-4081, BigEndian.getSignedShort(data1, data2));
	}

}
