/*
	Class Entry stores the information contained in a IFD entry.
	Each Entry knows its tagNumber, dataFormat, componentCount, offset, and value.
*/

package jpeg;

import java.util.Arrays;

import endian.BigEndian;
import endian.SmallEndian;

public class Entry
{
	private boolean bigEndian;
	private byte[] tagNumber;
	private int dataFormat;
	private long componentCount;
	private byte[] offset;
	private Object value;
	
	//Post: endian info is set to endian
	public Entry()
	{
		tagNumber = null;
		dataFormat = 0;
		componentCount = (long)0;
		offset = new byte[4];
		value = null;
		bigEndian = true;
	}
	
	//Post: tagNumber, dataFormat, componentCount, and value are set to the passed values.
	public Entry(byte[] tagNumber, int dataFormat, long componentCount, Object value, byte[] offset, boolean bigEndian)
	{
		this.tagNumber = tagNumber;
		this.dataFormat = dataFormat;
		this.componentCount = componentCount;
		this.value = value;
		this.offset = offset;
		this.bigEndian = bigEndian;
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
	public void setComponentCount(long componentCount)
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
	
	//Post: bigEndian is true if exif is in bigEndian, false otherwise
	public void setEndian(boolean b)
	{
		this.bigEndian = b;
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
	public long getComponentCount()
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
	
	//Return: true if data is in big endian, false otherwise.
	public boolean getEndian()
	{
		return bigEndian;
	}
	
	//Return: true if both Entry contains same tag number and value; false otherwise.
	public boolean equals(Object obj)
	{
		if( obj instanceof Entry ) {
			Entry entry = (Entry) obj;
			
			//compare tagNumber
			boolean sameTag = tagNumber[0] == entry.getTagNumber()[0] && tagNumber[1] == entry.getTagNumber()[1];
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
					return ( (String)value ).equals( (String)(entry.getValue()) );
				case 3: //unsigned short
					return (int)value == (int)(entry.getValue());
				case 4: //unsigned long
					return (long)value == (long)(entry.getValue());
				case 5: //unsigned rational
					if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
						double[] value1 = (double[]) value;
						double[] value2 = (double[]) entry.getValue();
						return Arrays.equals(value1, value2);
					} else {
						double value1 = (double) value;
						double value2 = (double) entry.getValue();
						return value1 == value2;
					}
				case 6: //signed byte
					return (char)value == (char)(entry.getValue());
				case 7: //undefined
					return true;
				case 8: //signed short
					return (short)value == (short)(entry.getValue());
				case 9: //signed long
					return (long)value == (long)(entry.getValue());
				case 10: //signed rational
					if(value.getClass().isArray() && entry.getValue().getClass().isArray()) {
						double[] value1 = (double[]) value;
						double[] value2 = (double[]) entry.getValue();
						return Arrays.equals(value1, value2);
					} else {
						double value1 = (double) value;
						double value2 = (double) entry.getValue();
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
		else
			return false;
	}
	
	//Return: a String that represent the Directory. Format: tag number: **, data format: **, componentCount: **, offset value: **, value: **
	public String toString()
	{
		String result =  new String("tag number: " + String.format( "%02x ", (tagNumber[0] & 0xFF) ) + String.format( "%02x", (tagNumber[1] & 0xFF) ) +
						  " data format: " + dataFormat +
						  " component count: " + componentCount + 
						  " offset value: " + (bigEndian ? BigEndian.getLong32(offset) : SmallEndian.getLong32(offset)) );
		//check whether it is latitude and longitude and provide different print String.
		if(dataFormat == 5 && value.getClass().isArray())
		{
			double[] cast_value = (double[])value;
			result += " value: ";
			for (int i=0; i<cast_value.length; i++)
				result += cast_value[i] + " ";
		}
		else result += " value: " + value;
		return result;
	}
}