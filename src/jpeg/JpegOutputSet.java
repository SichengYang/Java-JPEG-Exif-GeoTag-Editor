package jpeg;

import java.io.*;
import java.util.*;

public class JpegOutputSet {
	private byte[] jfif;
	private LinkedList<byte[]> remain_segment = new LinkedList<byte[]>();
	private byte[] compressed_data;

	private JpegExif exif;
	private Thumbnail thumbnail;

	private final int HEADER = 8;
	private final int NEXT_IFD_OFFSET = 4;
	private final int ENTRY_COUNT = 2;
	private final int ENTRY_SIZE = 12;
	private final int[] DATA_SIZE = {1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};

	//Post: Use Jpeg to create a output set. everything is copied
	public JpegOutputSet(Jpeg jpeg)
	{
		this.jfif = jpeg.jfif;
		this.remain_segment = jpeg.remain_segment;
		this.compressed_data = jpeg.compressed_data;
		this.exif = jpeg.exif;
		this.thumbnail = jpeg.thumbnail;
	}

	//Output: file output would be a jpeg with updated geotag
	public boolean updateGeoTag(File output, double latitude, double longitude)
	{
		removeGeoTag();
		updateGeoTag(latitude, longitude);
		return write(output);
	}

	//Output: file output would be a jpeg with geotag removed
	public boolean removeGeoTag(File output)
	{
		removeGeoTag();
		return write(output);
	}

	//Return: true if geotag is successfully updated
	private boolean updateGeoTag(double latitude, double longitude)
	{
		removeGeoTag();

		String latitude_ref;
		int[] latitude_data = {0, 1000, 0 , 1000, 0, 1000};
		String longitude_ref;
		int[] longitude_data = {0, 1000, 0 , 1000, 0, 1000};

		latitude_ref = latitude < 0 ? "S " : "N ";
		longitude_ref = longitude < 0 ? "W " : "E ";

		//set up latitude
		latitude_data[0] = (int)latitude * 1000;
		latitude %= 1;
		latitude *= 60;
		latitude_data[2] = (int)latitude * 1000;
		latitude %= 1;
		latitude *= 60;
		latitude_data[4] = (int)(latitude * 1000);

		//set up longitude
		longitude_data[0] = (int)longitude * 1000;
		longitude %= 1;
		longitude *= 60;
		longitude_data[2] = (int)longitude * 1000;
		longitude %= 1;
		longitude *= 60;
		longitude_data[4] = (int)(longitude * 1000);

		final int REF_DATA_TYPE = 2;
		final int REF_COMPONENT_COUNT = 2;
		final int VALUE_DATA_TYPE = 5;
		final int VALUE_COMPONENT_COUNT = 3;

		//set up latitude reference Entry
		byte[] latitude_ref_tag = new byte[2];
		latitude_ref_tag[0] = 0x00;
		latitude_ref_tag[1] = 0x01;
		byte[] latitude_ref_offset = new byte[4];
		if(latitude < 0)
			latitude_ref_offset[3] = (byte)'S';
		else
			latitude_ref_offset[3] = (byte)'N';
		Entry latitude_ref_entry = new Entry(latitude_ref_tag, REF_DATA_TYPE, 
				REF_COMPONENT_COUNT, (Object)latitude_ref,
				latitude_ref_offset);

		//set up latitude value Entry
		byte[] latitude_data_tag = new byte[2];
		latitude_data_tag[0] = 0x00;
		latitude_data_tag[1] = 0x02;
		byte[] latitude_data_offset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
		Entry latitude_data_entry = new Entry(latitude_data_tag, VALUE_DATA_TYPE, 
				VALUE_COMPONENT_COUNT, (Object)latitude_data,
				latitude_data_offset);

		//set up longitude reference Entry
		byte[] longitude_ref_tag = new byte[2];
		longitude_ref_tag[0] = 0x00;
		longitude_ref_tag[1] = 0x03;
		byte[] longitude_ref_offset = new byte[4];
		if(longitude < 0)
			longitude_ref_offset[3] = (byte)'W';
		else
			longitude_ref_offset[3] = (byte)'E';
		longitude_ref_offset[0] = longitude_ref_offset[1] = 0x00;
		Entry longitude_ref_entry = new Entry(longitude_ref_tag, REF_DATA_TYPE, 
				REF_COMPONENT_COUNT, (Object)longitude_ref,
				longitude_ref_offset);

		//set up longitude value Entry
		byte[] longitude_data_tag = new byte[2];
		longitude_data_tag[0] = 0x00;
		longitude_data_tag[1] = 0x04;
		byte[] longitude_data_offset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
		Entry longitude_data_entry = new Entry(longitude_data_tag, VALUE_DATA_TYPE, 
				VALUE_COMPONENT_COUNT, (Object)longitude_data,
				longitude_data_offset);

		LinkedList<Entry> gps = exif.getGpsIfd();

		if(gps == null) {
			//create GPS IFD if it does not present in resources file
			gps = new LinkedList<Entry>();
			exif.setGpsIfd(gps);

			//create pointer to GPS IFD
			LinkedList<Entry> ifd0 = exif.getIfd0();
			if(ifd0 == null) {
				ifd0 = new LinkedList<Entry>();
				exif.setIfd0(ifd0);
			}

			//add a reference to GPS IFD
			byte[] pointer_tag = { (byte) 0x88, 0x25 };
			byte[] pointer_data_offset = {0x00, 0x00, 0x00, 0x00}; //this is undetermined
			final int POINTER_FORMAT = 4;
			final int POINTER_COMPONENT_COUNT = 1;
			Entry gps_pointer = new Entry(pointer_tag, POINTER_FORMAT, 
					POINTER_COMPONENT_COUNT, (Object)((long)0),
					pointer_data_offset);
			ifd0.add(gps_pointer);
		}

		gps.addFirst(longitude_data_entry);
		gps.addFirst(longitude_ref_entry);
		gps.addFirst(latitude_data_entry);
		gps.addFirst(latitude_ref_entry);

		return true;
	}

	//Return: true if geotag is successfully removed
	private boolean removeGeoTag()
	{
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

	//Output: a jpeg is written based on the information in outpu set
	private boolean write(File output)
	{
		DataOutputStream outputStream = null;
		try {
			outputStream = new DataOutputStream (new BufferedOutputStream (new FileOutputStream(output)));

			//write header
			final byte[] header = {(byte)0xFF, (byte)0xD8};
			outputStream.write(header);

			//write jfif
			if(jfif != null)
				outputStream.write(jfif);

			//write exif
			writeExif(outputStream);

			//write other segment
			for(byte[] segment : remain_segment)
				outputStream.write(segment);

			//write data after start of scan
			outputStream.write(compressed_data);

			outputStream.close();
			return true;
		} catch (Exception e) {
			output.delete();
			e.printStackTrace();
			return false;
		}
	}

	//Output: exif segment in associate buffered output stream
	private void writeExif(DataOutputStream outputStream) throws IOException
	{
		writeStableData(outputStream);

		LinkedList<Entry> ifd0 = exif.getIfd0();
		if(ifd0 != null) {
			writeEntrySize(outputStream, ifd0);
			writeIfd0(outputStream, ifd0);
		}

		LinkedList<Entry> sub_ifd = exif.getSubIfd();
		if(sub_ifd != null) {
			writeEntrySize(outputStream, sub_ifd);
			writeSubIfd(outputStream, sub_ifd);
		}

		LinkedList<Entry> ifd1 = exif.getIfd1();
		if(ifd1 != null) {
			writeEntrySize(outputStream, ifd1);
			writeIfd1(outputStream, ifd1);
		}

		LinkedList<Entry> gps_ifd = exif.getGpsIfd();
		if(gps_ifd != null) {
			writeEntrySize(outputStream, gps_ifd);
			writeGps(outputStream, gps_ifd);
		}

		if(thumbnail != null)
			outputStream.write(thumbnail.getThumbnailData());
	}

	//Output: Write an entry using passed buffered output stream and ifd1 data
	private void writeGps(DataOutputStream outputStream, LinkedList<Entry> gps_ifd) throws IOException
	{
		int external_data_size = 0;
		for(Entry e : gps_ifd) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt((int)(e.getComponentCount()));

			//write offset
			if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = HEADER;

				LinkedList<Entry> ifd0 = exif.getIfd0();
				if(ifd0 != null) {
					offset += getEntrySize(ifd0);
					offset += getEntryDataSize(ifd0);
				}

				LinkedList<Entry> sub_ifd = exif.getSubIfd();
				if(sub_ifd != null) {
					offset += getEntrySize(sub_ifd);
					offset += getEntryDataSize(sub_ifd);
				}

				LinkedList<Entry> ifd1 = exif.getIfd1();
				if(ifd1 != null) {
					offset += getEntrySize(ifd1);
					offset += getEntryDataSize(ifd1);
				}

				if(gps_ifd != null)
					offset += getEntrySize(gps_ifd);

				offset += external_data_size;

				//calculate how many data is in external data field
				external_data_size += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		outputStream.writeInt(0);

		writeExternalData(outputStream, gps_ifd);
	}

	//Output: Write an entry using passed buffered output stream and ifd1 data
	private void writeIfd1(DataOutputStream outputStream, LinkedList<Entry> ifd1) throws IOException
	{
		int external_data_size = 0;
		for(Entry e : ifd1) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());			

			//write component count
			outputStream.writeInt((int)(e.getComponentCount()));

			//write offset
			if((tagNumber[0] & 0xFF) == 0x02 && (tagNumber[1] & 0xFF) == 0x01) { //write offset to thumbnail image
				int offset = HEADER;

				LinkedList<Entry> ifd0 = exif.getIfd0();
				if(ifd0 != null) {
					offset += getEntrySize(ifd0);
					offset += getEntryDataSize(ifd0);
				}

				LinkedList<Entry> sub_ifd = exif.getSubIfd();
				if(sub_ifd != null) {
					offset += getEntrySize(sub_ifd);
					offset += getEntryDataSize(sub_ifd);
				}

				if(ifd1 != null) {
					offset += getEntrySize(ifd1);
					offset += getEntryDataSize(ifd1);
				}

				LinkedList<Entry> gps = exif.getGpsIfd();
				if(gps != null) {
					offset += getEntrySize(gps);
					offset += getEntryDataSize(gps);
				}

				outputStream.writeInt(offset);
			} else if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = HEADER;

				LinkedList<Entry> ifd0 = exif.getIfd0();
				if(ifd0 != null) {
					offset += getEntrySize(ifd0);
					offset += getEntryDataSize(ifd0);
				}

				LinkedList<Entry> sub_ifd = exif.getSubIfd();
				if(sub_ifd != null) {
					offset += getEntrySize(sub_ifd);
					offset += getEntryDataSize(sub_ifd);
				}

				offset += getEntrySize(ifd1);

				offset += external_data_size;

				external_data_size += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		//write four byte 0 means end of linking
		outputStream.writeInt(0);

		writeExternalData(outputStream, ifd1);
	}

	//Output: Write an entry using passed buffered output stream and sub-ifd data
	private void writeSubIfd(DataOutputStream outputStream, LinkedList<Entry> sub_ifd) throws IOException
	{
		int external_data_size = 0;
		for(Entry e : sub_ifd) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt((int)(e.getComponentCount()));

			//write offset
			if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
				//Just copy data if no external data
				outputStream.write(e.getOffset());
			else {
				int offset = HEADER;

				LinkedList<Entry> ifd0 = exif.getIfd0();
				if(ifd0 != null) {
					offset += getEntrySize(ifd0);
					offset += getEntryDataSize(ifd0);
				}

				offset += getEntrySize(sub_ifd);

				offset += external_data_size;			

				//calculate how many data is in external data field
				external_data_size += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

				//write offset
				outputStream.writeInt(offset);
			}
		}

		//because we do not link other IFD, so just write a 0 here.
		outputStream.writeInt(0);

		writeExternalData(outputStream, sub_ifd);
	}

	//Write an entry using passed buffered output stream and ifd0 data
	private void writeIfd0(DataOutputStream outputStream, LinkedList<Entry> ifd0) throws IOException
	{
		int external_data_size = 0;
		for(Entry e : ifd0) {
			//write tag number
			byte[] tagNumber = e.getTagNumber();
			outputStream.write(tagNumber);

			//write data format
			outputStream.writeShort(e.getDataFormat());

			//write component count
			outputStream.writeInt((int)e.getComponentCount());

			//write offset
			if( (tagNumber[0] & 0xFF) == 0x87 && (tagNumber[1] & 0xFF) == 0x69 ) { //write sub-ifd offset
				int offset = HEADER + getEntrySize(ifd0) + getEntryDataSize(ifd0);
				outputStream.writeInt(offset);
			} else if( (tagNumber[0] & 0xFF) == 0x88 && (tagNumber[1] & 0xFF) == 0x25 ) { //write gps_ifd offset
				int offset = HEADER;

				offset += getEntrySize(ifd0);
				offset += getEntryDataSize(ifd0);

				LinkedList<Entry> sub_ifd = exif.getSubIfd();
				if(sub_ifd != null) {
					offset += getEntrySize(sub_ifd);
					offset += getEntryDataSize(sub_ifd);
				}

				LinkedList<Entry> ifd1 = exif.getIfd1();
				if(ifd1 != null) {
					offset += getEntrySize(ifd1);
					offset += getEntryDataSize(ifd1);
				}

				outputStream.writeInt(offset);
			} else { //general tag
				if( e.getComponentCount() * DATA_SIZE[e.getDataFormat()] <= 4 )
					//Just copy data if no external data
					outputStream.write(e.getOffset());
				else {
					int offset = HEADER + getEntrySize(ifd0) + external_data_size;

					//calculate how many data is in external data field
					external_data_size += e.getComponentCount() * DATA_SIZE[e.getDataFormat()];

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
		if(exif.getIfd1() != null) {
			int offset = HEADER;

			LinkedList<Entry> ifd0 = exif.getIfd0();
			if(ifd0 != null) {	
				offset += getEntrySize(ifd0);
				offset += getEntryDataSize(ifd0);
			}

			LinkedList<Entry> sub_ifd = exif.getSubIfd();
			if(sub_ifd != null) {
				offset += getEntrySize(sub_ifd);
				offset += getEntryDataSize(sub_ifd);
			}

			return offset;

		} else
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
						long[] long_data = (long[]) (e.getValue());
						for(long long_value : long_data)
							outputStream.writeInt((int)long_value);
						break;
					case 5: //unsigned rational
					case 9: //signed long
					case 10: //signed rational
						int[] rational_data = (int[]) e.getValue();
						for(int d : rational_data)
							outputStream.writeInt(d);
						break;
					case 7: //undefined
						outputStream.write( (byte[])(e.getValue()) );
						break;
					case 8: //signed short
						short[] short_data = (short[])(e.getValue());
						for(int short_value : short_data)
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
		int entry_count = ifd.size();
		outputStream.writeShort(entry_count);
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
		//size of each part
		final int HEADER = 16;

		int size = HEADER;

		LinkedList<Entry> ifd0 = exif.getIfd0();
		if(ifd0 != null) {	
			size += getEntrySize(ifd0);
			size += getEntryDataSize(ifd0);
		}

		LinkedList<Entry> sub_ifd = exif.getSubIfd();
		if(sub_ifd != null)	{
			size += getEntrySize(sub_ifd);
			size += getEntryDataSize(sub_ifd);
		}

		LinkedList<Entry> ifd1 = exif.getIfd1();
		if(ifd1 != null){
			size += getEntrySize(ifd1);
			size += getEntryDataSize(ifd1);
		}

		LinkedList<Entry> gps_ifd = exif.getGpsIfd();
		if(gps_ifd != null)	{
			size += getEntrySize(gps_ifd);
			size += getEntryDataSize(gps_ifd);
		}

		if(thumbnail != null)
			size += thumbnail.getThumbnailData().length;

		return size;
	}

	//Return: the size of IFD as an int. If IFD is null, this method will return 0;
	private int getEntrySize(LinkedList<Entry> ifd)
	{
		if(ifd != null)	
			return ENTRY_COUNT + ENTRY_SIZE * ifd.size() + NEXT_IFD_OFFSET;
		else 
			return 0;
	}

	//Return: return the size of data field of passed entry. If IFD is null, this method will return 0;
	private int getEntryDataSize(LinkedList<Entry> ifd)
	{
		int size = 0;

		if(ifd != null)
			for(Entry e : ifd) {
				int data_size = (int) (e.getComponentCount() * DATA_SIZE[e.getDataFormat()]);
				if(data_size > 4)
					size += data_size;
			}

		return size;
	}
}
