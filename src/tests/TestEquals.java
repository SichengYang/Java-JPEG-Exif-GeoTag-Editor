package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jpeg.Entry;

public class TestEquals {
	@Test
	public void testEquals()
	{
		Entry entry1 = new Entry();
		Entry entry2 = new Entry();

		byte[] tagNumber1 = new byte[2];
		byte[] tagNumber2 = new byte[3];

		tagNumber1[0] = (byte)( 0xFF );
		tagNumber1[1] = (byte)( 0x11 );

		tagNumber2[0] = (byte)( 0x11 );
		tagNumber2[1] = (byte)( 0xFF );

		entry1.setTagNumber(tagNumber1);
		entry2.setTagNumber(tagNumber1);

		entry1.setDataFormat(2);
		entry2.setDataFormat(2);

		entry1.setValue("test");
		entry2.setValue("test");

		assertEquals(entry1.equals(entry2), true);

		entry2.setValue("Test");
		assertEquals(entry1.equals(entry2), false);
	}
}
