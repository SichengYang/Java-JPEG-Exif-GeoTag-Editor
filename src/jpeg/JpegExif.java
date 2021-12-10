package lib.jpeg;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import lib.endian.BigEndian;
import lib.endian.LittleEndian;

/*
	JpegExif will process a byte[] exif data and gather the informaton.
	User can access JpegExif through class Jpeg.
*/

public class JpegExif {

	private boolean bigEndian;
	private int position;

	private int gpsOffset = 0;
	private int subOffset = 0;
	private int ifd1Offset = 0;
	private int interoperabilityOffset = 0;
	private int thumbnailFormat = 0;
	private int thumbnailOffset = 0;
	private int thumbnailLength = 0;
	private int imageWidth = 0;
	private int imageHeight = 0;
	
	private LinkedList<Entry> gpsEntry;
	private LinkedList<Entry> ifd0;
	private LinkedList<Entry> subIfd;
	private LinkedList<Entry> ifd1;
	private LinkedList<Entry> interoperabilityIfd;
	private Thumbnail thumbnail;

	private static final int HEADER_SIZE = 8;
	private static final int RATIONAL_SIZE = 8;
	private static final int SHORT_SIZE = 2;
	private static final int LONG_SIZE = 4;
	private static final int[] DATA_SIZE = {1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
	
	//Post: everything is set to null.
	public JpegExif()
	{
		gpsEntry = null;
		ifd0= null;
		subIfd= null;
		ifd1 = null;
		interoperabilityIfd = null;
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
		byte[] offsetData = new byte[4];
		for( int i=0; i<4; i++ )
			offsetData[i] = exif[position+i];
		int firstIfdOffset = getInt32(offsetData) - HEADER_SIZE;
		position += (LONG_SIZE + firstIfdOffset); //java only support int to index, although document states that offset is a long

		//read each IFD and find GPS IFD. 
		//read IFD0
		ifd0 = new LinkedList<Entry>( Arrays.asList(readIfd(exif)) );

		//read sub IFD
		if( subOffset != 0 ) 
		{
			position = subOffset;
			subIfd = new LinkedList<Entry>( Arrays.asList(readIfd(exif)) );
			analyzeSubIfd();
		}

		//read IFD 1
		if( ifd1Offset != 0 )
		{
			position = ifd1Offset;
			ifd1 = new LinkedList<Entry>( Arrays.asList(readIfd(exif)) );
		}

		//read GPS IFD
		if( gpsOffset != 0 )
		{
			position = gpsOffset;
			gpsEntry = new LinkedList<Entry>( Arrays.asList(readIfd(exif)) );
		}
		
		//read interoperability IFD
		if(interoperabilityOffset != 0)
		{
			position = interoperabilityOffset;
			interoperabilityIfd = new LinkedList<Entry>( Arrays.asList(readIfd(exif)) );
		}

		analyzeThumbnail();
		if( thumbnailOffset != 0 ) {
			position = thumbnailOffset;
			byte[] thumbnailData = new byte[thumbnailLength];
			for(int i=0; i<thumbnailLength;i++)
				thumbnailData[i] = exif[position + i];
			thumbnail = new Thumbnail(thumbnailData, thumbnailFormat);
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
		return imageWidth;
	}

	//Return: offset to thumbnail format
	public int getImageHeight()
	{
		return imageHeight;
	}

	//Return: offset to thumbnail format
	public int getThunmnailFormat()
	{
		return thumbnailFormat;
	}

	//Return: offset to thumbnail offset
	public int getThunmnailOffset()
	{
		return thumbnailOffset;
	}

	//Return: offset to thumbnail length
	public int getThunmnailLength()
	{
		return thumbnailLength;
	}

	//Return: a collection of Entry which is gps IFD
	public LinkedList<Entry> getGpsIfd()
	{
		return gpsEntry;
	}

	//Return: true if gps IFD is set. It always return true.
	public boolean setGpsIfd(LinkedList<Entry> gps)
	{
		this.gpsEntry = gps;
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
		return subIfd;
	}
	
	//Return: a collection of Entry which is interoperability IFD
	public LinkedList<Entry> getInterIfd()
	{
		return interoperabilityIfd;
	}

	//Return: a collection of Entry which is IFD1
	public LinkedList<Entry> getIfd1()
	{
		return ifd1;
	}

	//Return: a collection of Entry after reading a IFD
	private Entry[] readIfd (byte[] exif)
	{
		byte[] entryCountData = new byte[2];
		
		for(int i=0; i<2; i++)
			entryCountData[i] = exif[position + i];
		int entryCount = getInt16(entryCountData);
		position += 2;

		Entry[] entryCollection = new Entry[entryCount];
		for(int i=0; i<entryCount; i++)
		{
			entryCollection[i] = new Entry();

			//set tag number
			byte[] tagNumber = new byte[2];
			if( bigEndian )
			{
				tagNumber[0] = exif[position];
				tagNumber[1] = exif[position+1];
			}
			else
			{
				tagNumber[1] = exif[position];
				tagNumber[0] = exif[position+1];
			}
			entryCollection[i].setTagNumber(tagNumber);
			position += 2;

			//set data format 
			byte[] dataFormatValue = new byte[2];
			dataFormatValue[0] = exif[position];
			dataFormatValue[1] = exif[position+1];
			int dataFormat = getInt16(dataFormatValue);
			entryCollection[i].setDataFormat( dataFormat );
			position += 2;

			//set number of components
			byte[] componentCountValue = new byte[4];
			for(int j=0; j<4; j++)
				componentCountValue[j] = exif[position+j];
			int componentCount = getInt32(componentCountValue);
			entryCollection[i].setComponentCount( componentCount );
			position += 4;

			//set data offset
			byte[] offset = new byte[4];
			for(int j=0; j<4; j++)
				offset[j] = exif[position+j];
			entryCollection[i].setOffset(offset);
			position += 4;

			//set value
			Object value = getValue(offset, entryCollection[i].getDataFormat(), entryCollection[i].getComponentCount(), exif);
			entryCollection[i].setValue(value);

			//set byte to big endian
			if(!bigEndian)
				switch(dataFormat)
				{
					case 4, 5, 9, 10, 11, 12:
						swapByte(offset);
						break;
					case 3,8:
						if(componentCount * DATA_SIZE[dataFormat] <= 4) {
							byte temp = offset[0];
							offset[0] = offset[1];
							offset[1]= temp;
						} else
							swapByte(offset);
						break;
					case 2:
						if(componentCount * DATA_SIZE[dataFormat] > 4)
							swapByte(offset);
						break;
					default:
						break;
				}

			analyzeEntry(entryCollection[i]);
		}

		//read offset to IFD 1
		byte[] ifd1OffsetData = new byte[4];
		if(bigEndian)
			for(int i=0; i<4; i++)
				ifd1OffsetData[i] = exif[position + i];
		else
			for(int i=0; i<4; i++)
				ifd1OffsetData[i] = exif[position + 3 - i];
		int offset = BigEndian.getInt32(ifd1OffsetData);
		//if offset is 0 means this IFD does not link next IFD
		if (offset != 0) ifd1Offset = offset;

		return entryCollection;
	}

	//print all tag in exif
	public void print()
	{
		if(ifd0 != null){
			System.out.println("IFD0:");
			for(Entry e : ifd0)
				System.out.println(e);
		}

		if(subIfd != null){
			System.out.println("sub IFD:");
			for(Entry e : subIfd)
				System.out.println(e);
		}

		if(ifd1 != null){
			System.out.println("IFD1:");
			for(Entry e : ifd1)
				System.out.println(e);
		}

		if(gpsEntry != null){
			System.out.println("GPS data:");
			for(Entry e : gpsEntry)
				System.out.println(e);
		}
		
		if(interoperabilityIfd != null) {
			System.out.println("Interoperability data:");
			for(Entry e : interoperabilityIfd)
				System.out.println(e);
		}
	}

	//Analyze the entry to find the gps ifd
	private void analyzeEntry(Entry entry)
	{
		byte[] tagNumber = entry.getTagNumber();
		//set offset to gps IFD
		if( (tagNumber[0] & 0xFF) == 0x88 && (tagNumber[1] & 0xFF) == 0x25 )
			gpsOffset = getObjectValue(entry.getValue());
		//set offset to subIFD
		else if ( (tagNumber[0] & 0xFF) == 0x87 && (tagNumber[1] & 0xFF) == 0x69 )
			subOffset = getObjectValue(entry.getValue());
	}

	//Return: the value associate to data format and offset is returned as an Object
	private Object getValue(byte[] offset, int format, int componentCount, byte[] exif)
	{
		int size = componentCount * DATA_SIZE[format];
		byte[] value = new byte[size];
		if (size > 4)
		{
			int valueAddress = getInt32(offset);
			for(int i=0; i<size; i++)
				value[i] = exif[valueAddress+i];
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
				byte[] shortValue = new byte[2];
				shortValue[0] = value[0];
				shortValue[1] = value[1];
				return getInt16(shortValue);
			} else {
				int[] result = new int[size/SHORT_SIZE];
				for(int i=0; i < size / SHORT_SIZE ; i++) {
					byte[] shortValue = new byte[2];
					shortValue[0] = value[2 * i];
					shortValue[1] = value[2 * i + 1];
					result[i] = getInt16(shortValue);
				}
				return result;
			}
		case 4: //unsigned long
			if(size == LONG_SIZE)
				return getLong32(value);
			else{
				long[] result = new long[size/LONG_SIZE];
				for(int i=0; i < size / LONG_SIZE ; i++) {
					byte[] longValue = new byte[4];
					for(int j=0; j<4; j++)
						longValue[j] = value[2*i + j];
					result[i] = getLong32(longValue);
				}
				return result;
			}
		case 5: //unsigned rational
		case 10: //signed rational
			if(size == RATIONAL_SIZE)
			{
				byte[] numeratorData = new byte[4];
				byte[] denominatorData = new byte[4];
				for(int i=0; i<4; i++)
				{
					numeratorData[i] = value[i];
					denominatorData[i] = value[i+4];
				}
				int[] result = new int[2];
				result[0] = getInt32(numeratorData);
				result[1] = getInt32(denominatorData);
				return result;
			} 
			else
			{
				int[] result = new int[size / RATIONAL_SIZE * 2];
				for(int i=0; i<size/RATIONAL_SIZE; i++)
				{
					byte[] numeratorData = new byte[4];
					byte[] denominatorData = new byte[4];
					for(int j=0; j<4; j++)
					{
						numeratorData[j] = value[j + RATIONAL_SIZE * i];
						denominatorData[j] = value[j+ RATIONAL_SIZE * i + 4];
					}
					int numerator = getInt32(numeratorData);
					int denominator = getInt32(denominatorData);
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
				byte[] shortValue = new byte[2];
				shortValue[0] = value[0];
				shortValue[1] = value[1];
				return bigEndian ? BigEndian.getSignedShort(shortValue) : LittleEndian.getSignedShort(shortValue);
			} else {
				short[] result = new short[size/SHORT_SIZE];
				for(int i=0; i < size / SHORT_SIZE ; i++) {
					byte[] shortValue = new byte[2];
					shortValue[0] = value[2 * i];
					shortValue[1] = value[2 * i + 1];
					result[i] = bigEndian ? BigEndian.getSignedShort(shortValue) : LittleEndian.getSignedShort(shortValue);
				}
				return result;
			}
		case 9: //signed long
			if(size == LONG_SIZE)
				return getInt32(value);
			else{
				int[] result = new int[size/LONG_SIZE];
				for(int i=0; i < size / LONG_SIZE ; i++) {
					byte[] longValue = new byte[4];
					for(int j=0; j<4; j++)
						longValue[j] = value[2*i + j];
					result[i] = getInt32(longValue);
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
				byte[] tagNumber = e.getTagNumber();
				if ( (tagNumber[0] & 0xFF) == 0x01 && (tagNumber[1] & 0xFF) == 0x03 ) {
					thumbnailFormat = getObjectValue(e.getValue());
				} else if ( (tagNumber[0] & 0xFF) == 0x02 && (tagNumber[1] & 0xFF) == 0x01 ) {
					thumbnailOffset = getObjectValue(e.getValue());
				} else if ( (tagNumber[0] & 0xFF) == 0x02 && (tagNumber[1] & 0xFF) == 0x02 ) {
					thumbnailLength = getObjectValue(e.getValue());
				} else if ( (tagNumber[0] & 0xFF) == 0x01 && (tagNumber[1] & 0xFF) == 0x11 ) {
					thumbnailOffset = getObjectValue(e.getValue());
				} else if ( (tagNumber[0] & 0xFF) == 0x01 && (tagNumber[1] & 0xFF) == 0x17 ) {
					thumbnailLength = getObjectValue(e.getValue());
				} else if ( (tagNumber[0] & 0xFF) == 0x01 && (tagNumber[1] & 0xFF) == 0x06 ) {
					thumbnailFormat = getObjectValue(e.getValue());
				}
			}
	}

	//Post: image width and height would be available
	private void analyzeSubIfd()
	{
		for(Entry e : subIfd) {
			byte[] tagNumber = e.getTagNumber();
			if ( (tagNumber[0] & 0xFF) == 0xa0 && (tagNumber[1] & 0xFF) == 0x02 ) {
				imageWidth = getObjectValue(e.getValue());
			} else if ( (tagNumber[0] & 0xFF) == 0xa0 && (tagNumber[1] & 0xFF) == 0x03 ) {
				imageHeight = getObjectValue(e.getValue());
			} else if ( (tagNumber[0] & 0xFF) == 0xa0 && (tagNumber[1] & 0xFF) == 0x05 ) {
				interoperabilityOffset = getObjectValue(e.getValue());
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
		return bigEndian ? BigEndian.getInt32(value) : LittleEndian.getInt32(value);
	}

	//Return: a long which 4 bytes represent 
	private long getLong32(byte[] value)
	{
		return bigEndian ? BigEndian.getLong32(value) : LittleEndian.getLong32(value);
	}

	//Return: an int which 2 bytes represent
	private int getInt16(byte[] value)
	{
		return bigEndian ? BigEndian.getInt16(value) : LittleEndian.getInt16(value);
	}
}