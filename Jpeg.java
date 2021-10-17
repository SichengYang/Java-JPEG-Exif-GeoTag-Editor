import java.io.*;

public class Jpeg
{
	public byte[] jfif;
	public byte[] exif_marker;
	public byte[] exif;
	public byte[] remain_data;
	
	private static final int HEADER_SIZE = 10;
	
	//Post: read jpeg file and divide data area into jfif.
	//Throw: IOException if the file contains wrong or unreadable information
	public Jpeg(BufferedInputStream f) throws IOException
	{
		//read the marker to make sure it is a jpeg
		byte[] file_marker = new byte[2];
		f.read(file_marker);
		
		//check the file type
		if( (file_marker[0] & 0xFF) == 0xFF && (file_marker[1] & 0xFF) == 0xD8 )
		{
			byte[] segment = readSegment(f);
			if ( (segment[0] & 0xFF) == 0xFF && (segment[1] & 0xFF) == 0xE0 )
			{
				jfif = segment;
				byte[] exif_segment = readSegment(f); 
				process_exif(exif_segment);
			}
			else if ( (segment[0] & 0xFF) == 0xFF && (segment[1] & 0xFF) == 0xE1 )
				process_exif(segment);
			else throw new IOException("Error on reading exif segment");
		}
		else throw new IOException("It is not a jpeg/jpg file");
	}
	
	//Post: exif segment is divided into marker and content.
	private void process_exif(byte[] exif_segment)
	{	
		//copy the marker part
		exif_marker = new byte[HEADER_SIZE];
		exif = new byte[exif_segment.length - HEADER_SIZE];
		for(int i=0; i<HEADER_SIZE; i++)
			exif_marker[i] = exif_segment[i];
		for(int i=0; i<exif_segment.length - HEADER_SIZE; i++ )
			exif[i] = exif_segment[i + HEADER_SIZE];
	}
	
	//Return: a byte array which contains an segment
	private static byte[] readSegment (BufferedInputStream f) throws IOException
	{
		byte[] header = new byte[4];
		f.read(header);
		int size = BigEndian.getInt16(header[2], header[3]);
		
		byte[] content = new byte[size+2]; //content will include header information
		for(int i=0; i<4; i++)
			content[i] = header[i];
		for(int i=4; i<size+2; i++)
			content[i] = (byte)(f.read());
		return content;
	}
}