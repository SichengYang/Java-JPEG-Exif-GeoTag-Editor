package jpeg;

import java.io.*;

/*
	This is an object that stores information of thumbnail image.
*/

public class Thumbnail {

	private int thumbnailFormat;
	private byte[] thumbnailData;

	//Post: Constructor of Thumbnail class
	public Thumbnail(byte[] thumbnailData, int thumbnailFormat) throws IOException
	{
		this.thumbnailFormat = thumbnailFormat;
		this.thumbnailData = thumbnailData;

		//This means thumbnail is in jpeg format
		if(thumbnailFormat == 6 && 
		   !( (thumbnailData[0] & 0xFF) == 0xFF && (thumbnailData[1] & 0xFF) == 0xD8 ) )
			throw new IOException("Error on thumbnail image header");

		if( !( (thumbnailData[ thumbnailData.length-2 ] & 0xFF) == 0xFF && 
			   (thumbnailData[ thumbnailData.length-1 ] & 0xFF) == 0xD9 ) )
			throw new IOException("Error on thumbnial image ending bytes");
	}

	//Return: thumbnial data is returned as a byte array
	public byte[] getThumbnailData()
	{
		return thumbnailData;
	}
	
	//Return: thumbnail format as an int
	public int getFormat()
	{
		return thumbnailFormat;
	}
}
