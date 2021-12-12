package jpeg;

import java.io.*;
import java.util.*;

/*
	JpegOutputSet is the output routine. To create a JpegOutputSet, the user should pass  Jpeg object
	to the constructor. This output routine offers removeGeotag() and updateGeotag().
*/

public class JpegOutputSet {
	private byte[] jfif;
	private LinkedList<byte[]> remainSegment = new LinkedList<>();
	private byte[] compressedData;

	private JpegExif exif;
	private Thumbnail thumbnail;

	private int ifd0Size;
	private int subIfdSize;
	private int ifd1Size;
	private int gpsIfdSize;
	private int interIfdSize;

	private static final int DATA_HEADER = 8;
	private static final int NEXT_IFD_OFFSET = 4;
	private static final int ENTRY_COUNT_SIZE = 2;
	private static final int ENTRY_SIZE = 12;
	private static final int[] DATA_SIZE = {1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};

	//Post: Use Jpeg to create a output set. everything is copied
	public JpegOutputSet(Jpeg jpeg)
	{
		this.jfif = jpeg.jfif;
		this.remainSegment = jpeg.remainSegment;
		this.compressedData = jpeg.compressedData;
		this.exif = jpeg.exif;
		this.thumbnail = jpeg.thumbnail;
	}

	//Output: file output would be a jpeg with updated geotag
	public boolean updateGeoTag(File output, double latitude, double longitude) throws IOException
	{
		removeGeoTag();
		updateGeoTag(latitude, longitude);
		return write(output);
	}

	//Output: file output would be a jpeg with geotag removed
	public boolean removeGeoTag(File output) throws IOException
	{
		removeGeoTag();
		return write(output);
	}

	//Return: true if geotag is successfully updated
	private boolean updateGeoTag(double latitude, double longitude)
	{
		String latitudeRef;
		int[] latitudeData = {0, 1000, 0 , 1000, 0, 1000};
		String longitudeRef;
		int[] longitudeData = {0, 1000, 0 , 1000, 0, 1000};

		latitudeRef = latitude < 0 ? "S" : "N";
		longitudeRef = longitude < 0 ? "W" : "E";
		
		latitude = Math.abs(latitude);
		longitude = Math.abs(longitude);
		
		//set up latitude
		latitudeData[0] = (int)latitude * 1000;
		latitude %= 1;
		latitude *= 60;
		latitudeData[2] = (int)latitude * 1000;
		latitude %= 1;
		latitude *= 60;
		latitudeData[4] = (int)(latitude * 1000);

		//set up longitude
		longitudeData[0] = (int)longitude * 1000;
		longitude %= 1;
		longitude *= 60;
		longitudeData[2] = (int)longitude * 1000;
		longitude %= 1;
		longitude *= 60;
		longitudeData[4] = (int)(longitude * 1000);

		final int REF_DATA_TYPE = 2;
		final int REF_COMPONENT_COUNT = 2;
		final int VALUE_DATA_TYPE = 5;
		final int VALUE_COMPONENT_COUNT = 3;

		//set up latitude reference Entry
		byte[] latitudeRefTag = new byte[2];
		latitudeRefTag[0] = 0x00;
		latitudeRefTag[1] = 0x01;
		byte[] latitudeRefOffset = new byte[4];
		latitudeRefOffset[1] = 0x00;
		latitudeRefOffset[2] = 0x00;
		latitudeRefOffset[3] = 0x00;
		if(latitudeRef.equals("S"))
			latitudeRefOffset[0] = (byte)0x53;
		else
			latitudeRefOffset[0] = (byte)0x4E;
		Entry latitudeRefEntry = new Entry(latitudeRefTag, REF_DATA_TYPE, 
				REF_COMPONENT_COUNT, latitudeRef,
				latitudeRefOffset);

		//set up latitude value Entry
		byte[] latitudeDataTag = new byte[2];
		latitudeDataTag[0] = 0x00;
		latitudeDataTag[1] = 0x02;
		byte[] latitudeDataOffset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
		Entry latitudeDataEntry = new Entry(latitudeDataTag, VALUE_DATA_TYPE, 
				VALUE_COMPONENT_COUNT, latitudeData,
				latitudeDataOffset);

		//set up longitude reference Entry
		byte[] longitudeRefTag = new byte[2];
		longitudeRefTag[0] = 0x00;
		longitudeRefTag[1] = 0x03;
		byte[] longitudeRefOffset = new byte[4];
		longitudeRefOffset[1] = 0x00;
		longitudeRefOffset[2] = 0x00;
		longitudeRefOffset[3] = 0x00;
		if(longitudeRef.equals("W"))
			longitudeRefOffset[0] = 0x57;
		else
			longitudeRefOffset[0] = 0x45;
		Entry longitudeRefEntry = new Entry(longitudeRefTag, REF_DATA_TYPE, 
				REF_COMPONENT_COUNT, longitudeRef,
				longitudeRefOffset);

		//set up longitude value Entry
		byte[] longitudeDataTag = new byte[2];
		longitudeDataTag[0] = 0x00;
		longitudeDataTag[1] = 0x04;
		byte[] longitudeDataOffset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
		Entry longitudeDataEntry = new Entry(longitudeDataTag, VALUE_DATA_TYPE, 
				VALUE_COMPONENT_COUNT, longitudeData,
				longitudeDataOffset);
		
		LinkedList<Entry> gps = null;
		//if there is no exif available, the program will create one
		if(exif != null)
			gps = exif.getGpsIfd();
		else 
			exif = new JpegExif();

		if(gps == null) {
			//create GPS IFD if it does not present in resources file
			gps = new LinkedList<>();
			exif.setGpsIfd(gps);

			//create pointer to GPS IFD
			LinkedList<Entry> ifd0 = exif.getIfd0();
			if(ifd0 == null) {
				ifd0 = new LinkedList<>();
				exif.setIfd0(ifd0);
			}

			//add a reference to GPS IFD
			byte[] pointerTag = { (byte) 0x88, 0x25 };
			byte[] pointerDataOffset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
			final int POINTER_FORMAT = 4;
			final int POINTER_COMPONENT_COUNT = 1;
			Entry gpsPointer = new Entry(pointerTag, POINTER_FORMAT, 
					POINTER_COMPONENT_COUNT, ((long)0),
					pointerDataOffset);
			ifd0.add(gpsPointer);
		}
		
		//add GPS data to GPS IFD
		gps.addFirst(longitudeDataEntry);
		gps.addFirst(longitudeRefEntry);
		gps.addFirst(latitudeDataEntry);
		gps.addFirst(latitudeRefEntry);

		return true;
	}

	//Return: true if geotag is successfully removed
	private boolean removeGeoTag()
	{
		if(exif == null)
			return true;
		
		//Get GPS data
		LinkedList<Entry> gps = exif.getGpsIfd();

		if(gps == null) 
			return true;

		Entry temp = new Entry();

		//remove latitude reference
		byte[] tagNumber = {0x00, 0x01};
		temp.setTagNumber(tagNumber);
		gps.remove(temp);
		
		//remove latitude numeric data
		tagNumber[1] = 0x02;
		temp.setTagNumber(tagNumber);
		gps.remove(temp);

		//remove longitude reference
		tagNumber[1] = 0x03;
		temp.setTagNumber(tagNumber);
		gps.remove(temp);

		//remove longitude numeric data
		tagNumber[1] = 0x04;
		temp.setTagNumber(tagNumber);
		gps.remove(temp);

		return true;
	}

	//Output: a jpeg is written based on the information in output set
	private boolean write(File output) throws IOException
	{
		DataOutputStream outputStream = new DataOutputStream (new BufferedOutputStream (new FileOutputStream(output)));

		//write header
		final byte[] header = {(byte)0xFF, (byte)0xD8};
		outputStream.write(header);

		//write jfif
		if(jfif != null)
			outputStream.write(jfif);

		//write exif
		if(exif != null)
			writeExif(outputStream);

		//write other segment
		for(byte[] segment : remainSegment)
			outputStream.write(segment);

		//write data after start of scan
		outputStream.write(compressedData);

		outputStream.close();
		return true;
	}

	//Output: exif segment in associate buffered output stream
	private void writeExif(DataOutputStream outputStream) throws IOException
	{
		calculateIfdSize();

		writeStableData(outputStream);

		LinkedList<Entry> ifd0 = exif.getIfd0();
		if(ifd0 != null) {
			writeEntrySize(outputStream, ifd0);
			writeIfd0(outputStream, ifd0);
		}

		LinkedList<Entry> subIfd = exif.getSubIfd();
		if(subIfd != null) {
			writeEntrySize(outputStream, subIfd);
			writeSubIfd(outputStream, subIfd);
		}

		LinkedList<Entry> ifd1 = exif.getIfd1();
		if(ifd1 != null) {
			writeEntrySize(outputStream, ifd1);
			writeIfd1(outputStream, ifd1);
		}

		LinkedList<Entry> gpsIfd = exif.getGpsIfd();
		if(gpsIfd != null) {
			writeEntrySize(outputStream, gpsIfd);
			writeGps(outputStream, gpsIfd);
		}
		
		LinkedList<Entry> interoperabilityIfd = exif.getInterIfd();
		if(interoperabilityIfd != null) {
			writeEntrySize(outputStream, interoperabilityIfd);
			writeInterIfd(outputStream, interoperabilityIfd);
		}

		if(thumbnail != null)
			outputStream.write(thumbnail.getThumbnailData());
	}

	//Post: calculate the size of each IFD and set to associate data field
	private void calculateIfdSize()
	{
		LinkedList<Entry> ifd0 = exif.getIfd0();
		if(ifd0 == null)
			ifd0Size = 0;
		else
			ifd0Size = getEntrySize(ifd0) + getEntryDataSize(ifd0);

		LinkedList<Entry> subIfd = exif.getSubIfd();
		if(subIfd == null)
			subIfdSize = 0;
		else
			subIfdSize = getEntrySize(subIfd) + getEntryDataSize(subIfd);

		LinkedList<Entry> ifd1 = exif.getIfd1();
		if(ifd1 == null)
			ifd1Size = 0;
		else
			ifd1Size = getEntrySize(ifd1) + getEntryDataSize(ifd1);

		LinkedList<Entry> gps = exif.getGpsIfd();
		if(gps == null)
			gpsIfdSize = 0;
		else
			gpsIfdSize = getEntrySize(gps) + getEntryDataSize(gps);
	
		LinkedList<Entry> inter = exif.getInterIfd();
		if(inter == null)
			interIfdSize = 0;
		else
			interIfdSize = getEntrySize(inter) + getEntryDataSize(inter);
	}

	//Output: Write an entry using passed buffered output stream and interoperability data
	private void writeInterIfd(DataOutputStream outputStream, LinkedList<Entry> interoperabilityIfd) throws IOException
	{
		int externalDataSize = 0;
		for(Entry e : interoperabilityIfd) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt(e.getComponentCount());

			//write offset
			if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = DATA_HEADER + ifd0Size + subIfdSize + ifd1Size + gpsIfdSize;
				
				offset += getEntrySize(interoperabilityIfd);

				offset += externalDataSize;

				//calculate how many data is in external data field
				externalDataSize += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		outputStream.writeInt(0);

		writeExternalData(outputStream, interoperabilityIfd);
	}

	//Output: Write an entry using passed buffered output stream and ifd1 data
	private void writeGps(DataOutputStream outputStream, LinkedList<Entry> gpsIfd) throws IOException
	{
		int externalDataSize = 0;
		for(Entry e : gpsIfd) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt(e.getComponentCount());

			//write offset
			if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = DATA_HEADER + ifd0Size + subIfdSize + ifd1Size;

				offset += getEntrySize(gpsIfd);

				offset += externalDataSize;

				//calculate how many data is in external data field
				externalDataSize += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		outputStream.writeInt(0);

		writeExternalData(outputStream, gpsIfd);
	}

	//Output: Write an entry using passed buffered output stream and ifd1 data
	private void writeIfd1(DataOutputStream outputStream, LinkedList<Entry> ifd1) throws IOException
	{
		int externalDataSize = 0;
		for(Entry e : ifd1) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());			

			//write component count
			outputStream.writeInt(e.getComponentCount());

			//write offset
			if( ((tagNumber[0] & 0xFF) == 0x02 && (tagNumber[1] & 0xFF) == 0x01) ||
				((tagNumber[0] & 0xFF) == 0x01 && (tagNumber[1] & 0xFF) == 0x11) ) { //write offset to thumbnail image
				
				int offset = DATA_HEADER + ifd0Size + subIfdSize + ifd1Size + gpsIfdSize + interIfdSize;

				outputStream.writeInt(offset);
			} else if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = DATA_HEADER + ifd0Size + subIfdSize;

				offset += getEntrySize(ifd1);

				offset += externalDataSize;

				externalDataSize += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		//write four byte 0 means end of linking
		outputStream.writeInt(0);

		writeExternalData(outputStream, ifd1);
	}

	//Output: Write an entry using passed buffered output stream and sub-ifd data
	private void writeSubIfd(DataOutputStream outputStream, LinkedList<Entry> subIfd) throws IOException
	{
		int externalDataSize = 0;
		for(Entry e : subIfd) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt(e.getComponentCount());

			//write offset
			if( (tagNumber[0] & 0xFF) == 0xA0 && (tagNumber[1] & 0xFF) == 0x05 ) { //write Interoperability offset
				
				int offset = DATA_HEADER + ifd0Size + subIfdSize + ifd1Size + gpsIfdSize;
				
				outputStream.writeInt(offset);
			} else if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = DATA_HEADER + ifd0Size;

				offset += getEntrySize(subIfd);

				offset += externalDataSize;			

				//calculate how many data is in external data field
				externalDataSize += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		//because we do not link other IFD, so just write a 0 here.
		outputStream.writeInt(0);

		writeExternalData(outputStream, subIfd);
	}

	//Write an entry using passed buffered output stream and ifd0 data
	private void writeIfd0(DataOutputStream outputStream, LinkedList<Entry> ifd0) throws IOException
	{
		int externalDataSize = 0;
		for(Entry e : ifd0) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt(e.getComponentCount());

			//write offset
			if( (tagNumber[0] & 0xFF) == 0x87 && (tagNumber[1] & 0xFF) == 0x69 ) { //write sub-ifd offset
				int offset = DATA_HEADER + getEntrySize(ifd0) + getEntryDataSize(ifd0);
				outputStream.writeInt(offset);
			} else if( (tagNumber[0] & 0xFF) == 0x88 && (tagNumber[1] & 0xFF) == 0x25 ) { //write gpsIfd offset
				int offset = DATA_HEADER + ifd0Size + subIfdSize + ifd1Size;

				outputStream.writeInt(offset);
			} else { //general tag
				if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
					//Just copy data if no external data
					outputStream.write(e.getOffset());
				else {
					int offset = DATA_HEADER + getEntrySize(ifd0) + externalDataSize;

					//calculate how many data is in external data field
					externalDataSize += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

					//write offset
					outputStream.writeInt(offset);
				}
			}
		}

		//write link to IFD 1
		int offset = offsetToIfd1();
		outputStream.writeInt(offset);

		writeExternalData(outputStream, ifd0);
	}

	//Return: offset to IFD 1 as an int
	private int offsetToIfd1()
	{
		if(exif.getIfd1() != null)
			return DATA_HEADER + ifd0Size + subIfdSize;
		else
			return 0;
	}

	//Output: write external data for passed IFD
	private void writeExternalData(DataOutputStream outputStream, LinkedList<Entry> ifd) throws IOException
	{
		//write external data
		for(Entry e : ifd) {
			if(e.getComponentCount() * DATA_SIZE[e.getDataFormat()] > 4) {
				switch(e.getDataFormat())
				{
					case 2: //ASCII string
						outputStream.write( ((String)(e.getValue())).getBytes() );
						break;
					case 3: //unsigned short
						int[] data = (int[])(e.getValue());
						for(int short_value : data)
							outputStream.writeShort(short_value);
						break;
					case 4: //unsigned long
						long[] longData = (long[]) (e.getValue());
						for(long long_value : longData) {
							outputStream.write((byte)(long_value >> 24 & 0xFF));
							outputStream.write((byte)(long_value >> 16 & 0xFF));
							outputStream.write((byte)(long_value >> 8 & 0xFF));
							outputStream.write((byte)(long_value & 0xFF));
						}
						break;
					case 5: //unsigned rational
					case 9: //signed long
					case 10: //signed rational
						int[] intData = (int[]) e.getValue();
						for(int d : intData)
							outputStream.writeInt(d);
						break;
					case 7: //undefined
						outputStream.write( (byte[])(e.getValue()) );
						break;
					case 8: //signed short
						short[] shortData = (short[])(e.getValue());
						for(int short_value : shortData)
							outputStream.writeShort(short_value);
						break;
					default:
						throw new IOException("Illegal data format" + e.getDataFormat());
				}
			}
		}
	}

	//Output: Write an entry size using passed buffered output stream and ifd data
	private void writeEntrySize(DataOutputStream outputStream, LinkedList<Entry> ifd) throws IOException
	{
		int entryCount = ifd.size();
		outputStream.writeShort(entryCount);
	}

	//Output: write data that is always the same for jpeg
	private void writeStableData(DataOutputStream outputStream) throws IOException
	{
		final byte[] header = {(byte)0xFF, (byte)0xE1};
		outputStream.write(header);

		outputStream.writeShort(getExifSize());

		//write exif tag
		outputStream.write('E');
		outputStream.write('x');
		outputStream.write('i');
		outputStream.write('f');

		//write 2 bytes 0
		outputStream.write(0x00);
		outputStream.write(0x00);

		//write endian information
		outputStream.write('M');
		outputStream.write('M');


		//write tag marker
		outputStream.write(0x00);
		outputStream.write(0x2A);


		//write offset to first IFD
		outputStream.write(0x00);
		outputStream.write(0x00);
		outputStream.write(0x00);
		outputStream.write(0x08);
	}

	//Return: the size of exif segment
	private int getExifSize()
	{
		//if exif does not exit
		if(exif == null)
			return 0;
		
		//size of each part
		final int EXIF_HEADER = 16;

		int size = EXIF_HEADER + ifd0Size + subIfdSize + ifd1Size + gpsIfdSize + interIfdSize;

		if(thumbnail != null)
			size += thumbnail.getThumbnailData().length;

		return size;
	}

	//Return: the size of IFD as an int. If IFD is null, this method will return 0;
	private int getEntrySize(LinkedList<Entry> ifd)
	{
		if(ifd != null)	
			return ENTRY_COUNT_SIZE + ENTRY_SIZE * ifd.size() + NEXT_IFD_OFFSET;
		else 
			return 0;
	}

	//Return: return the size of data field of passed entry. If IFD is null, this method will return 0;
	private int getEntryDataSize(LinkedList<Entry> ifd)
	{
		int size = 0;

		if(ifd != null)
			for(Entry e : ifd) {
				int dataSize = e.getComponentCount() * DATA_SIZE[e.getDataFormat()];
				if(dataSize > 4)
					size += dataSize;
			}

		return size;
	}
}