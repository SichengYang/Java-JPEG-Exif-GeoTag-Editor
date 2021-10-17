import java.io.*;
import java.nio.ByteBuffer;

public class JpegExif {
	
	private boolean bigEndian;
	private int position;
	private int[] ifd_offset = {0,0,0}; //0: GPS IFD offset, 1: sub IFD offset, 2: IFD1 offset
	private Entry[] gps_entry;
	
	private static final int HEADER_SIZE = 8;
	private static final int RATIONAL_SIZE = 8;
	private static final int[] DATA_SIZE = {1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
	
	public JpegExif(byte[] exif) throws IOException
	{
		position = 0;
		
		//read endian info
		if( (char)exif[position] == 'M' && (char)exif[position+1] == 'M' )
			bigEndian = true;
		else if( (char)exif[position] == 'I' && (char)exif[position+1] == 'I' )
			bigEndian = false;
		else throw new IOException("Error endian information");
		position += 2;
		
		//check tag mark
		if (bigEndian)
		{
			if ( !( (exif[position] & 0xFF) == 0x00 && (exif[position+1] & 0xFF) == 0x2A ))
				throw new IOException("Error on tag marker");
		}
		else if ( !( (exif[position] & 0xFF) == 0x2A && (exif[position+1] & 0xFF) == 0x00 ))
			throw new IOException("Error on tag marker");
		position += 2;
		
		//calculate offset to first IFD
		byte[] offset_data = new byte[4];
		for( int i=0; i<4; i++ )
			offset_data[i] = exif[position+i];
		long first_ifd_offset = getLong32(offset_data) - HEADER_SIZE;
		position += (4 + (int)first_ifd_offset);
		
		//read each IFD and find GPS IFD. 
		for(int i=1; i<4; i++)
		{
			read_ifd(exif);
			if(ifd_offset[0] != 0)
			{
				position = ifd_offset[0];
				gps_entry = read_ifd(exif);
				break;
			}
			//if not reading ifd1, which is the last IFD. we will sign a new offset to next IFD.
			if(i != 3) position = ifd_offset[i];
		}
	}
	
	//Return: a collection of Entry which is gps IFD
	public Entry[] getGpsIfd()
	{
		return gps_entry;
	}
	
	//Return: a collection of Entry after reading a IFD
	private Entry[] read_ifd (byte[] exif)
	{
		byte[] entry_count_info = new byte[2];
		for(int i=0; i<2; i++)
			entry_count_info[i] = exif[position+i];
		int entry_count = getInt16(entry_count_info);
		position += 2;
		
		Entry[] entry_collection = new Entry[(int)entry_count];
		for(int i=0; i<entry_count; i++)
		{
			entry_collection[i] = new Entry();
			
			//set tag number
			byte[] tag_number = new byte[2];
			if( bigEndian )
			{
				tag_number[0] = exif[position];
				tag_number[1] = exif[position+1];
			}
			else
			{
				tag_number[1] = exif[position];
				tag_number[0] = exif[position+1];
			}
			entry_collection[i].setTagNumber(tag_number);
			position += 2;
			
			//set data format
			byte[] data_format = new byte[2];
			data_format[0] = exif[position];
			data_format[1] = exif[position+1];
			entry_collection[i].setDataFormat( getInt16(data_format) );
			position += 2;
			
			//set number of components
			byte[] component_count = new byte[4];
			for(int j=0; j<4; j++)
				component_count[j] = exif[position+j];
			entry_collection[i].setComponentCount( getLong32(component_count) );
			position += 4;
			
			//set data offset
			byte[] offset = new byte[4];
			for(int j=0; j<4; j++)
				offset[j] = exif[position+j];
			entry_collection[i].setOffset(offset);
			position += 4;
			
			//set value
			Object value = getValue(offset, entry_collection[i].getDataFormat(), entry_collection[i].getComponentCount(), exif);
			entry_collection[i].setValue(value);
			
			//set endian
			entry_collection[i].setEndian(bigEndian);
			analyzeEntry(entry_collection[i]);
		}
		
		//read offset to IFD 1
		byte[] ifd1_offset_data = new byte[4];
		for(int i=0; i<4; i++)
			ifd1_offset_data[i] = exif[position + i];
		int offset = (int)getLong32(ifd1_offset_data);
		//if offset is 0 means this IFD does not link next IFD
		if (offset != 0) ifd_offset[2] = offset;
		
		return entry_collection;
	}
	
	//Analyze the entry to find the gps ifd
	private void analyzeEntry(Entry entry)
	{
		byte[] tag_number = entry.getTagNumber();
		//set offset to gps IFD
		if( (tag_number[0] & 0xFF) == 0x88 && (tag_number[1] & 0xFF) == 0x25 )
			ifd_offset[0] = (int)((long)(entry.getValue()));
		//set offset to subIFD
		if ( (tag_number[0] & 0xFF) == 0x87 && (tag_number[1] & 0xFF) == 0x69 )
			ifd_offset[1] = (int)((long)(entry.getValue()));
	}
	
	//Return: the value associate to data format and offset is returned as an Object
	private Object getValue(byte[] offset, int format, long component_count, byte[] exif)
	{
		int size = (int)component_count * DATA_SIZE[format];
		byte[] value = new byte[size];
		if (size > 4)
		{
			int value_address = (int)getLong32(offset);
			for(int i=0; i<size; i++)
				value[i] = exif[value_address+i];
		}
		else value = offset;
		switch(format)
		{
			case 1: //unsigned byte
				return value[3];
			case 2: //ASCII string
				return new String(value);
			case 3: //unsigned short
				return getInt16(value);
			case 4: //unsigned long
				return getLong32(value);
			case 5: //unsigned rational
				if(size == RATIONAL_SIZE)
				{
					byte[] numerator_data = new byte[4];
					byte[] denominator_data = new byte[4];
					for(int i=0; i<4; i++)
					{
						numerator_data[i] = value[i];
						denominator_data[i] = value[i+4];
					}
					long numerator = getLong32(numerator_data);
					long denominator = getLong32(denominator_data);
					double result = (double)(numerator) / denominator;
					return result;
				} 
				else
				{
					double[] result = new double[size/RATIONAL_SIZE];
					for(int i=0; i<size/RATIONAL_SIZE; i++)
					{
						byte[] numerator_data = new byte[4];
						byte[] denominator_data = new byte[4];
						for(int j=0; j<4; j++)
						{
							numerator_data[j] = value[j + RATIONAL_SIZE * i];
							denominator_data[j] = value[j+ + RATIONAL_SIZE * i + 4];
						}
						long numerator = getLong32(numerator_data);
						long denominator = getLong32(denominator_data);
						result[i] = (double)(numerator) / denominator; 
					}
					return result;
				}
			case 6: //signed byte
				return (char)value[3];
			case 7: //undefined
				return new String(value);
			case 8: //signed short
				return bigEndian ? 
					   BigEndian.getSignedShort(value[2], value[3]) : 
					   SmallEndian.getSignedShort(value[2], value[3]);
			case 9: //signed long
				return getInt32(value);
			case 10: //signed rational
				byte[] numerator_data = new byte[4];
				byte[] denominator_data = new byte[4];
				for(int i=0; i<4; i++)
				{
					numerator_data[i] = value[i];
					denominator_data[i] = value[i + 4];
				}
				double numerator = getInt32(numerator_data);
				double denominator = getInt32(denominator_data);
				return numerator / denominator;
			case 11: //single float
				if(!bigEndian)
					for(int i=0; i<2; i++)
					{
						byte temp = value[i];
						value[i] = value[4-i];
						value[4-i] = temp;
					}
				return ByteBuffer.wrap(value).getFloat();
			case 12: //single double
				if(!bigEndian)
					for(int i=0; i<4; i++)
					{
						byte temp = value[i];
						value[i] = value[4-i];
						value[4-i] = temp;
					}
				return ByteBuffer.wrap(value).getDouble();
			default:
				return "unknown data";
		}
	}
	
	//Return: an signed int which 4 bytes represent.
	private int getInt32(byte[] value)
	{
		return bigEndian ? BigEndian.getInt32(value) : SmallEndian.getInt32(value);
	}
	
	//Return: a long which 4 bytes represent 
	private long getLong32(byte[] value)
	{
		return bigEndian ? BigEndian.getLong32(value) : SmallEndian.getLong32(value);
	}
	
	//Return: an int which 2 bytes represent
	private int getInt16(byte[] value)
	{
		
		return bigEndian ? BigEndian.getInt16(value) : SmallEndian.getInt16(value);
	}
}