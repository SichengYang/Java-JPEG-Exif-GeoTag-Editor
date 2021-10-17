import java.io.*;
import java.util.*;

public class Test
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
			Entry[] gps = exif.getGpsIfd();
			if (gps != null)
				for(int i=0; i<gps.length; i++)
					System.out.println(gps[i]);
			else System.out.println("GPS info not found");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		f.close();
		input.close();
	}
}