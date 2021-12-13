package jpeg;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

import endian.BigEndian;

/*
	Jpeg class is used to store all information of a jpeg file. It contains all structures of a jpeg.
	If user want to use OutputSet, it is required to create a Jpeg before creating OutputSet
*/

public class Jpeg
{
	public byte[] jfif;
	public byte[] exifMarker;
	public byte[] exifData;
	public LinkedList<byte[]> remainSegment;
	public byte[] compressedData;

	public JpegExif exif;
	public Thumbnail thumbnail;

	private boolean notFinishSegmentReading;
	private boolean notReadExif;
	private boolean notReadJfif;
	
	private static final int HEADER_SIZE = 10;

	//Post: read jpeg file and divide data area into jfif.
	//Throw: IOException if the file contains wrong or unreadable information
	public Jpeg(File f) throws IOException, NotJpegException
	{
		BufferedInputStream buff = new BufferedInputStream(new FileInputStream (f));

		//read the marker to make sure it is a jpeg
		byte[] fileMarker = new byte[2];
		if (buff.read(fileMarker) == -1){
			buff.close();
			throw new IOException("File does not have enough data on marker");
		}

		//check the file type
		if( !((fileMarker[0] & 0xFF) == 0xFF && (fileMarker[1] & 0xFF) == 0xD8) ) {
			buff.close();
			throw new NotJpegException(f.getName() + " is not a jpeg/jpg file");
		}
		
		notReadExif = true;
		notReadJfif = true;
		remainSegment = new LinkedList<>();
		
		//process first 2 segments to find potential JFIF and EXIF segment.
		for(int i=0; i<2; i++) {
			byte[] segment = readSegment(buff);
			if (  notReadJfif && (segment[0] & 0xFF) == 0xFF && (segment[1] & 0xFF) == 0xE0 ) {// FFE0 means we find a JFIF segment
					jfif = segment;
					notReadJfif = false;
			}
			else if ( notReadExif && (segment[0] & 0xFF) == 0xFF && (segment[1] & 0xFF) == 0xE1 ) { //FFE1 means we find a EXIF segment
					processExif(segment);
					exif = new JpegExif(exifData);
					thumbnail = exif.getThumbnail();
					notReadExif = false;
			}
			else 
				remainSegment.add(segment);
		}
		
		//finish remaining segment reading
		notFinishSegmentReading = true;
		while (notFinishSegmentReading) {
			byte[] segment = readSegment(buff);
			remainSegment.add(segment);
		}

		//read compressed jpeg data
		compressedData = new byte[buff.available()];
		buff.read(compressedData);

		buff.close();
	}

	//Output: test whether all data could be write back
	public void write(File output) throws IOException
	{
		FileOutputStream buff = new FileOutputStream(output);
		buff.write(0xFF);
		buff.write(0xD8);
		if(jfif != null) buff.write(jfif);
		buff.write(exifMarker);
		buff.write(exifData);
		for(byte[] segment : remainSegment)
			buff.write(segment);
		buff.write(compressedData);
		buff.close();
	}

	//Post: exif segment is divided into marker and content.
	private void processExif(byte[] exifSegment)
	{	
		//copy the marker part
		exifMarker = Arrays.copyOfRange(exifSegment, 0, HEADER_SIZE);
		exifData = Arrays.copyOfRange(exifSegment, HEADER_SIZE, exifSegment.length);
	}

	//Return: a byte array which contains an segment
	private byte[] readSegment (BufferedInputStream f) throws IOException
	{
		byte[] header = new byte[2];
		if(f.read(header) == -1)
			throw new IOException("There is not sufficient data on reading segment header");

		byte[] sizeData = new byte[2];
		if(f.read(sizeData) == -1)
			throw new IOException("There is not sufficient data on reading segment size");
		int size = BigEndian.getInt16(sizeData[0], sizeData[1]);

		if( (header[0] & 0xFF) == 0xFF ) {
			if( (header[1] & 0xFF) == 0xDA )
				notFinishSegmentReading = false;
		} else {
			System.out.printf("Error segment %02x %02x %n", header[0], header[1]);
			return null;
		}

		byte[] content = new byte[size+2]; //content will include header information
		content[0] = header[0];
		content[1] = header[1];
		content[2] = sizeData[0];
		content[3] = sizeData[1];
		for(int i=4; i<size+2; i++)
			content[i] = (byte)f.read();

		return content;		
	}
}