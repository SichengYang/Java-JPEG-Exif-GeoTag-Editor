package org.commons;

import java.io.*;
import java.util.*;
import org.commons.jpeg.JpegExif;
import org.commons.jpeg.Entry;

public class App
{
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		Scanner input = new Scanner (System.in);
		System.out.print("File name:");
		String filename = input.next();
		File photo = new File(filename);
		BufferedInputStream f = new BufferedInputStream(new FileInputStream (photo));
		try 
		{
			JpegExif exif = new JpegExif(f);
			
			Entry[] ifd0 = exif.getIfd0();
			System.out.println("IFD0:");
			for(Entry e : ifd0)
				System.out.println(e);
			ifd0 = null;
			
			Entry[] sub_ifd = exif.getSubIfd();
			System.out.println("sub IFD:");
			for(Entry e : sub_ifd)
				System.out.println(e);
			sub_ifd = null;
			
			Entry[] ifd1 = exif.getIfd1();
			System.out.println("IFD1:");
			for(Entry e : ifd1)
				System.out.println(e);
			ifd1 = null;
			
			Entry[] gps_ifd = exif.getGpsIfd();
			System.out.println("GPS data:");
			for(Entry e : gps_ifd)
				System.out.println(e);
			gps_ifd = null;
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		f.close();
		input.close();
	}
}