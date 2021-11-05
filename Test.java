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
			new JpegExif(f);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		System.out.println("Finish Reading");
		f.close();
		input.close();
	}
}