package tests;

import jpeg.*;
import java.io.File;

public class JpegExifTest {
	
	public static void main(String[] args)
	{
		JpegExif exif = null;
		
		try {
			exif = new JpegExif(new File("./assets/Internet.jpg"));
		} catch (Exception e) {
			System.out.println(e.getMessage());
			
		}
		
		if(exif != null) {
			System.out.println("Internet.jpg:");
			
			//test get ifd0 function
			System.out.println("IFD0:");
			Entry[] ifd0 = exif.getIfd0();
			if(ifd0 != null)
				for(Entry e : ifd0)
					System.out.println(e);
			else
				System.out.println("This jpeg does not have ifd0");
			
			//test get sub_ifd function
			System.out.println("sub IFD:");
			Entry[] sub_ifd = exif.getSubIfd();
			if(ifd0 != null)
				for(Entry e : sub_ifd)
					System.out.println(e);
			else
				System.out.println("This jpeg does not have sub_ifd");
			
			//test get ifd1 function
			System.out.println("IFD1:");
			Entry[] ifd1 = exif.getIfd1();
			if(ifd1 != null)
				for(Entry e : ifd1)
					System.out.println(e);
			else
				System.out.println("This jpeg does not have ifd1");
			
			//Test get GPS functions
			System.out.println("GPS data:");
			Entry[] gps = exif.getGpsIfd();
			if(gps != null)
				for(Entry e : gps)
					System.out.println(e);
			else
				System.out.println("This jpeg does not have GPS data");
		}
		
		//test print function
		try {
			exif = new JpegExif(new File("./assets/iPhone12-no-geotag.jpg"));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		if(exif != null) {
			System.out.println("\nNo geotag image data:");
			exif.print();
		}
		
	}
}
