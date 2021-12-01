/*
	Class Entry stores the information contained in a IFD entry.
	Each Entry knows its tagNumber, dataFormat, componentCount, offset, and value.
 */

package jpeg;

import java.util.Arrays;

import endian.BigEndian;

public class Entry 
{
	private byte[] tagNumber;
	private int dataFormat;
	private int componentCount;
	private byte[] offset;
	private Object value;

	//Post: endian info is set to endian
	public Entry()
	{
		tagNumber = null;
		dataFormat = 0;
		componentCount = 0;
		offset = new byte[4];
		value = null;
	}

	//Post: tagNumber, dataFormat, componentCount, and value are set to the passed values.
	public Entry(byte[] tagNumber, int dataFormat, int componentCount, Object value, byte[] offset)
	{
		this.tagNumber = tagNumber;
		this.dataFormat = dataFormat;
		this.componentCount = componentCount;
		this.value = value;
		this.offset = offset;
	}

	//Post: tagNumber is set to tagNumber
	public void setTagNumber(byte[] tagNumber)
	{
		this.tagNumber = tagNumber;
	}

	//Post: dataFormat is set to dataFormat
	public void setDataFormat(int dataFormat)
	{
		this.dataFormat = dataFormat;
	}

	//Post: componentCount is set to componentCount
	public void setComponentCount(int componentCount)
	{
		this.componentCount = componentCount;
	}

	//Pre; this could be null since it depends on the size format
	//Post: Directory value is set to value
	public void setValue(Object value)
	{
		this.value = value;
	}

	//Post: value_or_offset is set to the value in directory
	public void setOffset(byte[] offset)
	{
		this.offset = offset;
	}

	//Return: tagNumber of this Directory
	public byte[] getTagNumber()
	{
		return tagNumber;
	}

	//Return: dataFormat of this Directory
	public int getDataFormat()
	{
		return dataFormat;
	}

	//Return: number of components in this Directory
	public int getComponentCount()
	{
		return componentCount;
	}

	//Return: value in directory as a byte[]
	public Object getValue()
	{
		return value;
	}

	//Return: byte[] offset is returned
	public byte[] getOffset()
	{
		return offset;
	}

	//Return: true if both Entry contains same tag number and value; false otherwise.
	public boolean equals(Entry entry)
	{
		boolean sameTag = (tagNumber[0] == entry.getTagNumber()[0] && tagNumber[1] == entry.getTagNumber()[1]);
		if(!sameTag)
			return false;

		//compare data type
		if(dataFormat != entry.getDataFormat())
			return false;

		//compare value
		switch(dataFormat)
		{
		case 1: //unsigned byte
			return (byte)value == (byte)(entry.getValue());
		case 2: //ASCII string
			return ( (String)value ).equals( (entry.getValue()) );
		case 3: //unsigned short
			if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
				int[] value1 = (int[]) value;
				int[] value2 = (int[]) entry.getValue();
				return Arrays.equals(value1, value2);
			}
			else	
				return (int)value == (int)(entry.getValue());
		case 4: //unsigned long
		case 9: //signed long
			if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
				return Arrays.equals((long[])value, (long[])(entry.getValue()));
			} else
				return (long)value == (long)(entry.getValue());
		case 5: //unsigned rational
			if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
				int[] value1 = (int[]) value;
				int[] value2 = (int[]) entry.getValue();
				return Arrays.equals(value1, value2);
			} else {
				int value1 = (int) value;
				int value2 = (int) entry.getValue();
				return value1 == value2;
			}
		case 6: //signed byte
			return (char)value == (char)(entry.getValue());
		case 7: //undefined
			return Arrays.equals((byte[])value, (byte[])(entry.getValue()));
		case 8: //signed short
			return (short)value == (short)(entry.getValue());
		case 10: //signed rational
			if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
				int[] value1 = (int[]) value;
				int[] value2 = (int[]) entry.getValue();
				return Arrays.equals(value1, value2);
			} else {
				int value1 = (int) value;
				int value2 = (int) entry.getValue();
				return value1 == value2;
			}
		case 11: //single float
			return (float)value == (float)(entry.getValue());
		case 12: //single double
			return (double)value == (double)(entry.getValue());
		default:
			return false;
		}
	}

	//Return: true if both Entry contains same tag number; false otherwise.
	public boolean equals(Object obj)
	{
		if( obj instanceof Entry ) {
			Entry entry = (Entry)obj;

			//compare tagNumber
			boolean sameTag = tagNumber[0] == entry.getTagNumber()[0] && tagNumber[1] == entry.getTagNumber()[1];
			return sameTag;
		}
		else
			return false;
	}

	//return: tag number as hashcode
	public int hashCode()
	{
		return BigEndian.getInt16(tagNumber);
	}

	//Return: a String that represent the Directory. Format: tag number: **, data format: **, componentCount: **, offset value: **, value: **
	public String toString()
	{
		String result =  new String("tag number: " + String.format( "%02x ", (tagNumber[0] & 0xFF) ) + String.format( "%02x", (tagNumber[1] & 0xFF) ) +
				" data format: " + String.format("%-2d", dataFormat) +
				" component count: " + String.format("%-4d", componentCount) + 
				" offset value: " + 
				String.format( "%08x", BigEndian.getLong32(offset) ));
		//check whether it is latitude and longitude and provide different print String.
		if(dataFormat == 5 || dataFormat == 10) {
			int[] cast_value = (int[])value;
			result += " value: ";
			for (int i=0; i<cast_value.length/2; i++)
				result += (double)cast_value[2*i]/cast_value[2*i+1] + " ";
		} else if(dataFormat == 3 && value.getClass().isArray()) {
			int[] cast_value = (int[])value;
			result += " value: ";
			for (int i=0; i<cast_value.length; i++)
				result += cast_value[i] + " ";
		}
		else if(dataFormat == 7)
		{
			result += " value: unknown value";
		}
		else result += " value: " + value;
		return result;
	}
}