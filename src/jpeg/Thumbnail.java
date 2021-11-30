package jpeg;

import java.io.*;

public class Thumbnail {
	
	private int thumbnail_format;
	private byte[] thumbnail_data;
	
	//Post: Constructor of Thumbnail class
	public Thumbnail(byte[] thumbnail_data, int thumbnail_format) throws IOException
	{
		this.thumbnail_format = thumbnail_format;
		this.thumbnail_data = thumbnail_data;
		
		//This means thumbnail is in jpeg format
		if(thumbnail_format == 6)
			if( !( (thumbnail_data[0] & 0xFF) == 0xFF && (thumbnail_data[1] & 0xFF) == 0xD8 ) )
				throw new IOException("Error on thumbnail image header");
		
		if( !( (thumbnail_data[ thumbnail_data.length-2 ] & 0xFF) == 0xFF && (thumbnail_data[ thumbnail_data.length-1 ] & 0xFF) == 0xD9 ) )
			throw new IOException("Error on thumbnial image ending bytes");
	}
	
	//Return: thumbnial data is returned as a byte array
	public byte[] getThumbnailData()
	{
		return thumbnail_data;
	}
}
