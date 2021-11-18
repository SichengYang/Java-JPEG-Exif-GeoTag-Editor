/*
	Class Entry stores the information contained in a IFD entry.
	Each Entry knows its tagNumber, dataFormat, componentCount, offset, and value.
*/

package jpeg;

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