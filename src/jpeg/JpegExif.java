package jpeg;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import endian.BigEndian;
import endian.SmallEndian;

public class JpegExif {

	private boolean bigEndian;
	private int position;

	private int gps_offset = 0;
	private int sub_offset = 0;
	private int ifd1_offset = 0;
	private int interoperability_offset = 0;
	private int thumbnail_format = 0;
	private int thumbnail_offset = 0;
	private int thumbnail_length = 0;
	private int image_width = 0;
	private int image_height = 0;
	
	private LinkedList<Entry> gps_entry;
	private LinkedList<Entry> ifd0;
	private LinkedList<Entry> sub_ifd;
	private LinkedList<Entry> ifd1;
	private LinkedList<Entry> interoperability_ifd;
	private Thumbnail thumbnail;

	private static final int HEADER_SIZE = 8;
	private static final int RATIONAL_SIZE = 8;
	private static final int SHORT_SIZE = 2;
	private static final int LONG_SIZE = 4;
	private static final int[] DATA_SIZE = {1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
	
	//Post: everything is set to null.
	public JpegExif()
	{
		gps_entry = null;
		ifd0= null;
		sub_ifd= null;
		ifd1 = null;
		interoperability_ifd = null;
		thumbnail = null;
	}
	
	//Post: read exif data and assign to associate data fields.
	public JpegExif(byte[] exif) throws IOException
	{		
		if(exif == null)
			return;

		position = 0;

		//read endian info
		if( (char)exif[position] == 'M' && (char)exif[position+1] == 'M' )
			bigEndian = true;
		else if( (char)exif[position] == 'I' && (char)exif[position+1] == 'I' )
			bigEndian = false;
		else
			return; //This means image does not have EXIF data
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
		int first_ifd_offset = getInt32(offset_data) - HEADER_SIZE;
		position += (LONG_SIZE + first_ifd_offset); //java only support int to index, although document states that offset is a long

		//read each IFD and find GPS IFD. 
		//read IFD0
		ifd0 = new LinkedList<Entry>( Arrays.asList(read_ifd(exif)) );

		//read sub IFD
		if( sub_offset != 0 ) 
		{
			position = sub_offset;
			sub_ifd = new LinkedList<Entry>( Arrays.asList(read_ifd(exif)) );
			analyzeSubIfd();
		}

		//read IFD 1
		if( ifd1_offset != 0 )
		{
			position = ifd1_offset;
			ifd1 = new LinkedList<Entry>( Arrays.asList(read_ifd(exif)) );
		}

		//read GPS IFD
		if( gps_offset != 0 )
		{
			position = gps_offset;
			gps_entry = new LinkedList<Entry>( Arrays.asList(read_ifd(exif)) );
		}
		
		//read interoperability IFD
		if(interoperability_offset != 0)
		{
			position = interoperability_offset;
			interoperability_ifd = new LinkedList<Entry>( Arrays.asList(read_ifd(exif)) );
		}

		analyzeThumbnail();
		if( thumbnail_offset != 0 ) {
			position = thumbnail_offset;
			byte[] thumbnail_data = new byte[thumbnail_length];
			for(int i=0; i<thumbnail_length;i++)
				thumbnail_data[i] = exif[position + i];
			thumbnail = new Thumbnail(thumbnail_data, thumbnail_format);
		}
	}

	//Return: endian info represented by a boolean. If return value is true, exif is big endian. 
	//		  Exif is in small endian otherwise.
	public boolean isBigEndian()
	{
		return bigEndian;
	}

	//Return: Thumbnail object which contains thumbnail image
	public Thumbnail getThumbnail()
	{
		return thumbnail;
	}

	//Return: offset to thumbnail format
	public int getImageWidth()
	{
		return image_width;
	}

	//Return: offset to thumbnail format
	public int getImageHeight()
	{
		return image_height;
	}

	//Return: offset to thumbnail format
	public int getThunmnailFormat()
	{
		return thumbnail_format;
	}

	//Return: offset to thumbnail offset
	public int getThunmnailOffset()
	{
		return thumbnail_offset;
	}

	//Return: offset to thumbnail length
	public int getThunmnailLength()
	{
		return thumbnail_length;
	}

	//Return: a collection of Entry which is gps IFD
	public LinkedList<Entry> getGpsIfd()
	{
		return gps_entry;
	}

	//Return: true if gps IFD is set. It always return true.
	public boolean setGpsIfd(LinkedList<Entry> gps)
	{
		this.gps_entry = gps;
		return true;
	}

	//Return: a collection of Entry which is IFD0
	public LinkedList<Entry> getIfd0()
	{
		return ifd0;
	}

	//Return: true if IFD0 is set. It always return true.
	public boolean setIfd0(LinkedList<Entry> ifd0)
	{
		this.ifd0 = ifd0;
		return true;
	}

	//Return: a collection of Entry which is sub IFD
	public LinkedList<Entry> getSubIfd()
	{
		return sub_ifd;
	}
	
	//Return: a collection of Entry which is interoperability IFD
	public LinkedList<Entry> getInterIfd()
	{
		return interoperability_ifd;
	}

	//Return: a collection of Entry which is IFD1
	public LinkedList<Entry> getIfd1()
	{
		return ifd1;
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
			byte[] data_format_value = new byte[2];
			data_format_value[0] = exif[position];
			data_format_value[1] = exif[position+1];
			int data_format = getInt16(data_format_value);
			entry_collection[i].setDataFormat( data_format );
			position += 2;

			//set number of components
			byte[] component_count_value = new byte[4];
			for(int j=0; j<4; j++)
				component_count_value[j] = exif[position+j];
			int component_count = getInt32(component_count_value);
			entry_collection[i].setComponentCount( component_count );
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

			//set byte to big endian
			if(!bigEndian)
				switch(data_format)
				{
					case 4: case 5:
					case 9: case 10: case 11:
					case 12:
						swapByte(offset);
						break;
					case 8:
					case 3:
						if(component_count * DATA_SIZE[data_format] <= 4) {
							byte temp = offset[0];
							offset[0] = offset[1];
							offset[1]= temp;
						} else
							swapByte(offset);
						break;
					
					case 2:
						if(component_count * DATA_SIZE[data_format] > 4)
							swapByte(offset);
				}

			analyzeEntry(entry_collection[i]);
		}

		//read offset to IFD 1
		byte[] ifd1_offset_data = new byte[4];
		if(bigEndian)
			for(int i=0; i<4; i++)
				ifd1_offset_data[i] = exif[position + i];
		else
			for(int i=0; i<4; i++)
				ifd1_offset_data[i] = exif[position + 3 - i];
		int offset = BigEndian.getInt32(ifd1_offset_data);
		//if offset is 0 means this IFD does not link next IFD
		if (offset != 0) ifd1_offset = offset;

		return entry_collection;
	}

	//print all tag in exif
	public void print()
	{
		if(ifd0 != null){
			System.out.println("IFD0:");
			for(Entry e : ifd0)
				System.out.println(e);
		}

		if(sub_ifd != null){
			System.out.println("sub IFD:");
			for(Entry e : sub_ifd)
				System.out.println(e);
		}

		if(ifd1 != null){
			System.out.println("IFD1:");
			for(Entry e : ifd1)
				System.out.println(e);
		}

		if(gps_entry != null){
			System.out.println("GPS data:");
			for(Entry e : gps_entry)
				System.out.println(e);
		}
		
		if(interoperability_ifd != null) {
			System.out.println("Interoperability data:");
			for(Entry e : interoperability_ifd)
				System.out.println(e);
		}
	}

	//Analyze the entry to find the gps ifd
	private void analyzeEntry(Entry entry)
	{
		byte[] tag_number = entry.getTagNumber();
		//set offset to gps IFD
		if( (tag_number[0] & 0xFF) == 0x88 && (tag_number[1] & 0xFF) == 0x25 )
			gps_offset = getObjectValue(entry.getValue());
		//set offset to subIFD
		else if ( (tag_number[0] & 0xFF) == 0x87 && (tag_number[1] & 0xFF) == 0x69 )
			sub_offset = getObjectValue(entry.getValue());
	}

	//Return: the value associate to data format and offset is returned as an Object
	private Object getValue(byte[] offset, int format, int component_count, byte[] exif)
	{
		int size = component_count * DATA_SIZE[format];
		if(size<0) System.out.println(component_count);
		byte[] value = new byte[size];
		if (size > 4)
		{
			int value_address = getInt32(offset);
			for(int i=0; i<size; i++)
				value[i] = exif[value_address+i];
		}
		else value = offset;
		switch(format)
		{
		case 1: //unsigned byte
			return value[0];
		case 2: //ASCII string
			return new String(value);
		case 3: //unsigned short
			if(size == 2) {
				byte[] short_value = new byte[2];
				short_value[0] = value[0];
				short_value[1] = value[1];
				return getInt16(short_value);
			} else {
				int[] result = new int[size/SHORT_SIZE];
				for(int i=0; i < size / SHORT_SIZE ; i++) {
					byte[] short_value = new byte[2];
					short_value[0] = value[2 * i];
					short_value[1] = value[2 * i + 1];
					result[i] = getInt16(short_value);
				}
				return result;
			}
		case 4: //unsigned long
			if(size == LONG_SIZE)
				return getLong32(value);
			else{
				long[] result = new long[size/LONG_SIZE];
				for(int i=0; i < size / LONG_SIZE ; i++) {
					byte[] long_value = new byte[4];
					for(int j=0; j<4; j++)
						long_value[j] = value[2*i + j];
					result[i] = getLong32(long_value);
				}
				return result;
			}
		case 5: //unsigned rational
		case 10: //signed rational
			if(size == RATIONAL_SIZE)
			{
				byte[] numerator_data = new byte[4];
				byte[] denominator_data = new byte[4];
				for(int i=0; i<4; i++)
				{
					numerator_data[i] = value[i];
					denominator_data[i] = value[i+4];
				}
				int numerator = getInt32(numerator_data);
				int denominator = getInt32(denominator_data);
				int[] result = {numerator, denominator};
				return result;
			} 
			else
			{
				int[] result = new int[size / RATIONAL_SIZE * 2];
				for(int i=0; i<size/RATIONAL_SIZE; i++)
				{
					byte[] numerator_data = new byte[4];
					byte[] denominator_data = new byte[4];
					for(int j=0; j<4; j++)
					{
						numerator_data[j] = value[j + RATIONAL_SIZE * i];
						denominator_data[j] = value[j+ RATIONAL_SIZE * i + 4];
					}
					int numerator = getInt32(numerator_data);
					int denominator = getInt32(denominator_data);
					result[2*i] = numerator;
					result[2*i+1] = denominator;
				}
				return result;
			}
		case 6: //signed byte
			return (char)value[0];
		case 7: //undefined
			return value;
		case 8: //signed short
			if(size == 2) {
				byte[] short_value = new byte[2];
				short_value[0] = value[0];
				short_value[1] = value[1];
				return bigEndian ? BigEndian.getSignedShort(short_value) : SmallEndian.getSignedShort(short_value);
			} else {
				short[] result = new short[size/SHORT_SIZE];
				for(int i=0; i < size / SHORT_SIZE ; i++) {
					byte[] short_value = new byte[2];
					short_value[0] = value[2 * i];
					short_value[1] = value[2 * i + 1];
					result[i] = bigEndian ? BigEndian.getSignedShort(short_value) : SmallEndian.getSignedShort(short_value);
				}
				return result;
			}
		case 9: //signed long
			if(size == LONG_SIZE)
				return getInt32(value);
			else{
				int[] result = new int[size/LONG_SIZE];
				for(int i=0; i < size / LONG_SIZE ; i++) {
					byte[] long_value = new byte[4];
					for(int j=0; j<4; j++)
						long_value[j] = value[2*i + j];
					result[i] = getInt32(long_value);
				}
				return result;
			}
		case 11: //single float
			if(!bigEndian) {
				for(int i=0; i<2; i++)
				{
					byte temp = value[i];
					value[i] = value[4-i];
					value[4-i] = temp;
				}
			}
			return ByteBuffer.wrap(value).getFloat();
		case 12: //single double
			if(!bigEndian) {
				for(int i=0; i<4; i++)
				{
					byte temp = value[i];
					value[i] = value[4-i];
					value[4-i] = temp;
				}
			}
			return ByteBuffer.wrap(value).getDouble();
		default:
			return "unknown data";
		}
	}

	//Post: a byte[] which is swaped
	private void swapByte(byte[] b)
	{
		for(int i=0; i<b.length/2; i++ ) {
			byte temp = b[i];
			b[i] = b[b.length - i - 1];
			b[b.length - i - 1] = temp;
		}
	}

	//Post: read offset and data length of thumbnail image
	private void analyzeThumbnail()
	{
		if(ifd1 != null)
			for (Entry e : ifd1) {
				byte[] tag_number = e.getTagNumber();
				if ( (tag_number[0] & 0xFF) == 0x01 && (tag_number[1] & 0xFF) == 0x03 ) {
					thumbnail_format = getObjectValue(e.getValue());
				} else if ( (tag_number[0] & 0xFF) == 0x02 && (tag_number[1] & 0xFF) == 0x01 ) {
					thumbnail_offset = getObjectValue(e.getValue());
				} else if ( (tag_number[0] & 0xFF) == 0x02 && (tag_number[1] & 0xFF) == 0x02 ) {
					thumbnail_length = getObjectValue(e.getValue());
				} else if ( (tag_number[0] & 0xFF) == 0x01 && (tag_number[1] & 0xFF) == 0x11 ) {
					thumbnail_offset = getObjectValue(e.getValue());
				} else if ( (tag_number[0] & 0xFF) == 0x01 && (tag_number[1] & 0xFF) == 0x17 ) {
					thumbnail_length = getObjectValue(e.getValue());
				} else if ( (tag_number[0] & 0xFF) == 0x01 && (tag_number[1] & 0xFF) == 0x06 ) {
					thumbnail_format = getObjectValue(e.getValue());
				}
			}
	}

	//Post: image width and height would be available
	private void analyzeSubIfd()
	{
		for(Entry e : sub_ifd) {
			byte[] tag_number = e.getTagNumber();
			if ( (tag_number[0] & 0xFF) == 0xa0 && (tag_number[1] & 0xFF) == 0x02 ) {
				image_width = getObjectValue(e.getValue());
			} else if ( (tag_number[0] & 0xFF) == 0xa0 && (tag_number[1] & 0xFF) == 0x03 ) {
				image_height = getObjectValue(e.getValue());
			} else if ( (tag_number[0] & 0xFF) == 0xa0 && (tag_number[1] & 0xFF) == 0x05 ) {
				interoperability_offset = getObjectValue(e.getValue());
			}
		}
	}

	//Pre: Object should be int or long
	//Return: an int that represent by the ibject
	private int getObjectValue(Object obj)
	{
		if(obj instanceof Integer)
			return (int)(obj);
		else
			return (int)((long)obj);
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