package tests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import jpeg.Entry;
import jpeg.Jpeg;
import jpeg.JpegExif;
import jpeg.JpegOutputSet;
import jpeg.NotJpegException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OutputSetTest {
	
	@Test
	public void OutputSetTest() throws IOException, NotJpegException
	{
		double latitude = 50.0 + 30.0 / 60 + 55.77 / 3600;
		double longitude = 100.0 + 50.0 / 60 + 10.8 / 3600;
		
		File remove_results = new File("./assets/remove");
		if(!remove_results.exists())
			remove_results.mkdir();
		
		File update_results = new File("./assets/update");
		if(!update_results.exists())
			update_results.mkdir();
		
		File resource = new File("./assets");

		for(File f : resource.listFiles()) {
			try {
				if(f.isFile()) {
					File output = new File("./assets/remove/" + f.getName());
					Jpeg jpeg = new Jpeg(f);
					JpegOutputSet outputSet = new JpegOutputSet(jpeg);
					if (outputSet.removeGeoTag(output)) {
						testRemoveGeoTag(output);
						analyzeOutput(f, output);
					}
				}
			} catch (Exception e) {
				System.err.println("Failed to remove " + f.getName() + " because " + e.getMessage());
			}
		}
		
		for(File f : resource.listFiles()) {
			if(f.isFile()) {
				try {
					File output = new File("./assets/update/" + f.getName());
					Jpeg jpeg = new Jpeg(f);
					JpegOutputSet outputSet = new JpegOutputSet(jpeg);
					if (outputSet.updateGeoTag(output, latitude, longitude))
						analyzeOutput(f, output);
				} catch (Exception e) {
					System.err.println("Failed to update " + f.getName() + " because " + e.getMessage());
				}
			}
		}
	}
	
	//Analyze whether geotag in image is removed
	public void testRemoveGeoTag(File output) throws IOException, NotJpegException
	{
		Jpeg jpeg = new Jpeg(output);
		
		JpegExif exif = jpeg.exif;
		LinkedList<Entry> gps = null;
		
		if(exif != null) {
			gps = exif.getGpsIfd();
			
			if(gps != null) {
				Entry gps_tag = new Entry();
				byte[] tag = {0x00, 0x01};
				gps_tag.setTagNumber(tag);
				assertEquals(false, gps.contains(gps_tag));
				
				tag[1] = 0x02;
				gps_tag.setTagNumber(tag);
				assertEquals(false, gps.contains(gps_tag));

				tag[1] = 0x03;
				gps_tag.setTagNumber(tag);
				assertEquals(false, gps.contains(gps_tag));

				tag[1] = 0x04;
				gps_tag.setTagNumber(tag);
				assertEquals(false, gps.contains(gps_tag));
			}
		}
	}

	//Analyze difference between resources and output
	private void analyzeOutput(File f1, File f2) throws IOException, NotJpegException
	{
		Jpeg jpeg1 = new Jpeg(f1);
		Jpeg jpeg2 = new Jpeg(f2);
		
		JpegExif exif1 = jpeg1.exif;
		JpegExif exif2 = jpeg2.exif;
		
		if( exif1 == null && exif2 == null )
			return;
		
		//avoid null pointer issue.
		if( exif1 == null )
			exif1 = new JpegExif();
		if( exif2 == null )
			exif2 = new JpegExif();
		
		//check ifd 0
		LinkedList<Entry> ifd0_1 = exif1.getIfd0();
		LinkedList<Entry> ifd0_2 = exif2.getIfd0();
		
		if(ifd0_1 != null)
			checkIfd0(ifd0_1, ifd0_2);
		
		//compare sub_ifd result
		LinkedList<Entry> sub_ifd_1 = exif1.getSubIfd();
		LinkedList<Entry> sub_ifd_2 = exif2.getSubIfd();
		
		if(sub_ifd_1 != null)
			checkSubIfd(sub_ifd_1, sub_ifd_2);
		
		//compare IFD 1 result
		LinkedList<Entry> ifd1_1 = exif1.getIfd1();
		LinkedList<Entry> ifd1_2 = exif2.getIfd1();
		
		if(ifd1_1 != null)
			checkIfd1(ifd1_1, ifd1_2);
		
		LinkedList<Entry> gps1 = exif1.getGpsIfd();
		LinkedList<Entry> gps2 = exif2.getGpsIfd();
		
		if(gps1 != null)
			checkGps(gps1, gps2);
		
		LinkedList<Entry> inter1 = exif1.getInterIfd();
		LinkedList<Entry> inter2 = exif2.getInterIfd();
		
		if(inter1 != null)
			checkInter(inter1, inter2);
	}

	//Post: check all value in ifd0 using JUnit
	private void checkIfd0(LinkedList<Entry> ifd0_1, LinkedList<Entry> ifd0_2)
	{	
		//remove sub IFD offset tag
		Entry entry = new Entry();
		byte[] tag = { (byte) 0x87, (byte) 0x69 };
		entry.setTagNumber(tag);
		ifd0_1.remove(entry);
		ifd0_2.remove(entry);
		
		//remove GPS offset tag
		tag[0] = (byte) 0x88;
		tag[1] = (byte) 0x25;
		entry.setTagNumber(tag);
		ifd0_1.remove(entry);
		ifd0_2.remove(entry);
		
		assertEquals(ifd0_1.size(), ifd0_2.size());
		
		for(int i=0; i<ifd0_1.size(); i++) {
			Entry e1 = ifd0_1.remove();
			Entry e2 = ifd0_2.remove();
			assertEquals(true, e1.equals(e2));
		}
	}

	//Post: checking value in sub ifd using JUnit
	private void checkSubIfd(LinkedList<Entry> sub_ifd_1, LinkedList<Entry> sub_ifd_2)
	{
		assertEquals(sub_ifd_1.size(), sub_ifd_2.size());
		
		for(int i=0; i<sub_ifd_1.size(); i++) {
			Entry e1 = sub_ifd_1.remove();
			Entry e2 = sub_ifd_2.remove();
			assertEquals(true, e1.equals(e2));
		}
	}

	//Post: checking value in ifd1 using JUnit
	private void checkIfd1(LinkedList<Entry> ifd1_1, LinkedList<Entry> ifd1_2)
	{
		//remove offset to thumbnail image
		Entry entry = new Entry();
		byte[] tag = {0x02, 0x01};
		entry.setTagNumber(tag);
		ifd1_1.remove(entry);
		ifd1_2.remove(entry);
	
		tag[0] = 0x01;
		tag[1] = 0x11;
		entry.setTagNumber(tag);
		ifd1_1.remove(entry);
		ifd1_2.remove(entry);
	
		assertEquals(ifd1_1.size(), ifd1_2.size());
	
		for(int i=0; i<ifd1_1.size(); i++) {
			Entry e1 = ifd1_1.remove();
			Entry e2 = ifd1_2.remove();
			assertEquals(true, e1.equals(e2));
		}
	}

	//Post: checking value in GPS IFD using JUnit
	private void checkGps(LinkedList<Entry> gps1, LinkedList<Entry> gps2)
	{
		// remove latitude reference
		Entry entry = new Entry();
		byte[] tag = {0x00, 0x01};
		entry.setTagNumber(tag);
		gps1.remove(entry);
		gps2.remove(entry);
		
		// remove latitude data
		tag[1] = 0x02;
		entry.setTagNumber(tag);
		gps1.remove(entry);
		gps2.remove(entry);
		
		// remove longitude reference
		tag[1] = 0x03;
		entry.setTagNumber(tag);
		gps1.remove(entry);
		gps2.remove(entry);
		
		// remove longitude data
		tag[1] = 0x04;
		entry.setTagNumber(tag);
		gps1.remove(entry);
		gps2.remove(entry);

		assertEquals(gps1.size(), gps2.size());

		for(int i=0; i<gps1.size(); i++) {
			Entry e1 = gps1.remove();
			Entry e2 = gps2.remove();
			assertEquals(true, e1.equals(e2));
		}
	}
	
	//Post: checking value in interoperability IFD using JUnit
	private void checkInter(LinkedList<Entry> inter1, LinkedList<Entry> inter2)
	{
		assertEquals(inter1.size(), inter2.size());

		for(int i=0; i<inter1.size(); i++) {
			Entry e1 = inter1.remove();
			Entry e2 = inter2.remove();
			assertEquals(true, e1.equals(e2));
		}
	}
}
