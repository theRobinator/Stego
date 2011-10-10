import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;


/**
 * This class is used to do the actual translation of the file. The
 * writeStego and readStego methods do all the work of the program.
 * 
 * It is STRONGLY recommended that you view this in a program that colors code.
 * 
 * @author Robert Keller, rkeller@ucsc.edu
 */
public class StegImage {
	
	//Prevent instantiation
	private StegImage() {}
	
	/**
	 * writeStego opens an image file, writes a stego message to it, and then writes it out
	 * to a specified file.
	 * @param infile The input image
	 * @param message The message to write
	 * @param outfile The file to be created
	 * @throws IOException
	 */
	public static void writeStego(String infile, String message, String outfile, String passphrase) throws IOException {
		//Break the String message into bytes, then append the length to the beginning of the array
		byte[] str = message.getBytes();
		byte[] msg = new byte[str.length+3];

		msg[0] = (byte)((str.length >> 16) & 0xFF);
		msg[1] = (byte)((str.length >>  8) & 0xFF);
		msg[2] = (byte)((str.length      ) & 0xFF);
		for (int i = 0; i < str.length; ++i) {
			msg[i+3] = str[i];
		}

		// Open the input file and set up the arrays to read into
		BufferedImage source = ImageIO.read(new File(infile));
		int w = source.getWidth(), h = source.getHeight();
		int curr, mcurr; 		// the current pixel and message bits
		int mcounter = 0; 		// the current byte of the message
		int subcounter = 0; 	// the current position in bits of the current byte of the message
		int mask = 3; 			// the bit mask used for setting values. This is 00000011.
		boolean done = false;

		// Before doing anything, check and see if the image is large enough to hold the message
		if (msg.length*4 > w*h*3) {
			throw new IOException("The picture is too small to hold the message! Please choose a larger picture.");
		}

		//Set up the variables for jumping around the image pixels
		Random rand = new Random(passphrase.hashCode());
		SortedList visited = new SortedList();
		
		//Jump through pseudorandom pixels and write bits to their LSBs
		int size = w*h, pos;
		while (visited.size < size && !done) {
			//Figure out what pixel to write to next
			pos = rand.nextInt(size);
			while (visited.contains(pos)) { pos = (pos+1)%size; } //If we've already been here, just go to the next available pixel
			visited.insert(pos);
			
			curr = source.getRGB(pos%w, pos/w);
			for (int shift = 16; shift >= 0; shift -= 8) {
				/* each pixel is a 32-bit integer value of the format aaaaaaaarrrrrrrrggggggggbbbbbbbb,
				 * with a=alpha, r=red, g=green, b=blue. Changing the alpha values would be too obvious,
				 * so we change only the 2 least significant bits of the rgb values. This means that it
				 * takes 4 pixels to store 3 message bytes, or 4/3 times the number of bytes in the message.
				 */
				mcurr = (msg[mcounter] >> subcounter) & mask; //sets all bits but the two we want to 0
				curr = (~(mask << shift) & curr) | (mcurr << shift); // unset both bits we want, then OR the new ones in
				
				subcounter = (subcounter+2) % 8; // increment the bit offset in the current message byte
				if (subcounter == 0) mcounter++; // if we've gone through all the bits in this byte, move to the next
				if (mcounter == msg.length) { // if we're done with the message, exit the loop
					done = true;
					break;
				}
			}
			source.setRGB(pos%w, pos/w, curr);
		}
		
		ImageIO.write(source, "png", new File(outfile));
	}
	
	/**
	 * readStegod reads a stego message out of a file that was written with writeStego.
	 * @param infile The file to be read
	 * @return A string containing the message that was stored in the file.
	 */
	public static String readStego(String infile, String passphrase) throws Exception {
		BufferedImage source = ImageIO.read(new File(infile));
		
		int w = source.getWidth(), h = source.getHeight();
		int curr;
		int mcounter = 0, subcounter = 0;
		int mask = 3; //see the above method for descriptions of these variables
		
		//Set up the PRNG to tell what pixels to look at
		int size = w*h, pos;
		Random rand = new Random(passphrase.hashCode());
		SortedList visited = new SortedList();
		
		//Read the first 4 pixels specially to get the message length
		int l1 = rand.nextInt(size);
		int l2 = rand.nextInt(size);
		int l3 = rand.nextInt(size);
		int l4 = rand.nextInt(size);
		l1 = source.getRGB(l1%w, l1/w);
		l2 = source.getRGB(l2%w, l2/w);
		l3 = source.getRGB(l3%w, l3/w);
		l4 = source.getRGB(l4%w, l4/w);

		byte[] lenarry = new byte[3];
		lenarry[0] |= (l1 >> 16) & mask;
		lenarry[0] |= ((l1 >> 8) & mask) << 2;
		lenarry[0] |= (l1 & mask) << 4;
		lenarry[0] |= ((l2 >> 16) & mask) << 6;

		lenarry[1] |= (l2 >> 8) & mask;
		lenarry[1] |= (l2 & mask) << 2;
		lenarry[1] |= ((l3 >> 16) & mask) << 4;
		lenarry[1] |= ((l3 >> 8) & mask) << 6;

		lenarry[2] |= l3 & mask;
		lenarry[2] |= ((l4 >> 16) & mask) << 2;
		lenarry[2] |= ((l4 >> 8) & mask) << 4;
		lenarry[2] |= (l4 & mask) << 6;

		int mlen = ((lenarry[0] << 16)&0xFF0000) | ((lenarry[1] << 8)&0xFF00) | (lenarry[2] & 0xFF);

		// Check if the image is large enough to hold this message before doing anything else
		if (mlen > w*h*3/4) throw new Exception("Reported message length is longer than the file.");
		
		byte[] msg = new byte[mlen];
		
		while (mcounter < mlen && visited.size < size) {
			pos = rand.nextInt(size);
			while (visited.contains(pos)) { pos = (pos+1)%size; } //See writeStego
			visited.insert(pos);
			
			curr = source.getRGB(pos%w, pos/w);
			for (int shift = 16; shift >= 0; shift -= 8) {
				msg[mcounter] = (byte)( ((((curr >> shift) & mask) << subcounter) | msg[mcounter] )&0xFF);
				
				subcounter = (subcounter + 2) % 8;
				if (subcounter == 0) mcounter++;
				if (mcounter == mlen) break;
			}
		}
		
		return new String(msg);
	}
	
	/**
	 * writeBlind opens an image file, writes a stego message WITHOUT scrambling,
	 * and then writes the image out. This is here for debug purposes, or if somebody in the future
	 * wants to call it to see how bits are modified without having to search the whole image.
	 * 
	 * @param infile The input image
	 * @param message The message to write
	 * @param outfile The file to be created
	 * @throws IOException
	 */
	public static void writeBlind(String infile, String message, String outfile) throws IOException {
		//Break the String message into bytes, then append the length to the beginning of the array
		byte[] str = message.getBytes();
		byte[] msg = new byte[str.length+3];

		msg[0] = (byte)((str.length >> 16) & 0xFF);
		msg[1] = (byte)((str.length >>  8) & 0xFF);
		msg[2] = (byte)((str.length      ) & 0xFF);
		for (int i = 0; i < str.length; ++i) {
			msg[i+3] = str[i];
		}

		// Open the input file and set up the arrays to read into
		BufferedImage source = ImageIO.read(new File(infile));
		int w = source.getWidth(), h = source.getHeight();
		BufferedImage dest = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[w];
		int curr, mcurr;
		int mcounter = 0;
		int subcounter = 0;
		int mask = 3;
		boolean done = false;

		// Before doing anything, check and see if the image is large enough to hold the message
		if (msg.length*4 > w*h*3) {
			throw new IOException("The picture is too small to hold the message! Please choose a larger picture.");
		}

		// Write the message to the LSBs of the pixel values
		for (int i = 0; i < h; ++i) {
			source.getRGB(0,i,w,1,pixels,0,w); // Read the next line of the image

			if (!done) { //modify the pixels in this line
				for (int j = 0; j < w; ++j) {
					curr = pixels[j];

					for (int shift = 16; shift >= 0; shift -= 8) {
						mcurr = (msg[mcounter] >> subcounter) & mask; //sets all bits but the two we want to 0
						curr = (~(mask << shift) & curr) | (mcurr << shift); // unset both bits we want, then OR the new ones in
						
						
						
						subcounter = (subcounter+2) % 8; // increment the bit offset in the current message byte
						if (subcounter == 0) mcounter++; // if we've gone through all the bits in this byte, move to the next
						if (mcounter == msg.length) { // if we're done with the message, exit the loop
							done = true;
							break;
						}
					}

					pixels[j] = curr;

					if (done) break;
				}
			}
			dest.setRGB(0,i,w,1,pixels,0,w);
		}
		ImageIO.write(dest, "png", new File(outfile));
	}
	
	/**
	 * readBlind reads an unscrambled stego message out of a file. Like writeBlind,
	 * it's only here for debugging/fun purposes.
	 * @param infile The file to be read
	 * @return A string containing the message that was stored in the file.
	 */
	public static String readBlind(String infile) throws Exception {
		BufferedImage source = ImageIO.read(new File(infile));
		
		int w = source.getWidth(), h = source.getHeight();
		int[] pixels = new int[w];
		int curr;
		int mcounter = 0, subcounter = 0;
		int mask = 3; //see the above method for descriptions of these variables
		
		//Read the first line specially to get the message length
		source.getRGB(0,0,w,1,pixels,0,w);

		byte[] lenarry = new byte[3];
		lenarry[0] |= (pixels[0] >> 16) & mask;
		lenarry[0] |= ((pixels[0] >> 8) & mask) << 2;
		lenarry[0] |= (pixels[0] & mask) << 4;
		lenarry[0] |= ((pixels[1] >> 16) & mask) << 6;

		lenarry[1] |= (pixels[1] >> 8) & mask;
		lenarry[1] |= (pixels[1] & mask) << 2;
		lenarry[1] |= ((pixels[2] >> 16) & mask) << 4;
		lenarry[1] |= ((pixels[2] >> 8) & mask) << 6;

		lenarry[2] |= pixels[2] & mask;
		lenarry[2] |= ((pixels[3] >> 16) & mask) << 2;
		lenarry[2] |= ((pixels[3] >> 8) & mask) << 4;
		lenarry[2] |= (pixels[3] & mask) << 6;

		int mlen = ((lenarry[0] << 16)&0xFF0000) | ((lenarry[1] << 8)&0xFF00) | (lenarry[2] & 0xFF);

		// Check if the image is large enough to hold this message before doing anything else
		if (mlen > w*h*3/4) throw new Exception("Reported message length is longer than the file.");
		
		byte[] msg = new byte[mlen];
		// We also need a loop to read the rest of the line
		
		for (int j = 4; j < w; ++j) {
			curr = pixels[j];
			
			for (int shift = 16; shift >= 0; shift -= 8) {
				msg[mcounter] = (byte)( ((((curr >> shift) & mask) << subcounter) | msg[mcounter] )&0xFF);
				
				subcounter = (subcounter + 2) % 8;
				if (subcounter == 0) mcounter++;
				if (mcounter == mlen) break;
			}
			if (mcounter == mlen) break;
		}


		// Now finish the rest of the lines normally
		for (int i = 1; i < h; ++i) {
			source.getRGB(0,i,w,1,pixels,0,w);
			
			if (mcounter < mlen) {
				
				for (int j = 0; j < w; ++j) {
					curr = pixels[j];
					
					for (int shift = 16; shift >= 0; shift -= 8) {
						msg[mcounter] = (byte)( ((((curr >> shift) & mask) << subcounter) | msg[mcounter] )&0xFF);
						
						subcounter = (subcounter + 2) % 8;
						if (subcounter == 0) mcounter++;
						if (mcounter == mlen) break;
					}
					if (mcounter == mlen) break;
				}
				
			}
		}
		
		return new String(msg);
	}
	
	/**
	 * readDirect just reads the whole image and returns the aggregated bytes by looking
	 * at the LSBs of the pixels. It's used for both analysis and for when no message is found.
	 * @param infile
	 * @return
	 * @throws IOException
	 */
	public static byte[] readDirect(String infile) throws IOException {
		return readDirect(ImageIO.read(new File(infile)));
	}
	public static byte[] readDirect(BufferedImage source) {
		int w = source.getWidth(), h = source.getHeight();
		int[] pixels = new int[w];
		int curr;
		int mcounter = 0, subcounter = 0;
		int mask = 3; //see the writeStego method for descriptions of these variables
		
		int mlen = w*h*3/4; // we hold 6/8 bits = 3/4 of a byte in each pixel
		if ((w*h)%4 != 0) mlen++; //adds one to account for int rounding

		byte[] msg = new byte[mlen];

		// Just read all the LSBs and put them in an array.
		for (int i = 0; i < h; ++i) {
			source.getRGB(0,i,w,1,pixels,0,w);
			
			if (mcounter < mlen) {
				
				for (int j = 0; j < w; ++j) {
					curr = pixels[j];
					
					for (int shift = 16; shift >= 0; shift -= 8) {
						msg[mcounter] = (byte)( ((((curr >> shift) & mask) << subcounter) | msg[mcounter] )&0xFF);
						
						subcounter = (subcounter + 2) % 8;
						if (subcounter == 0) mcounter++;
						if (mcounter == mlen) break;
					}
					if (mcounter == mlen) break;
				}
				
			}
		}
		
		return msg;
	}
	
	/**
	 * This calculates the percentage of bit transitions (0 to 1 and vice versa) in adjacent pixels.
	 * The program counts a bit transition as one where two of the four neighboring pixels do not share their LSB.
	 * Normal images have a much lower rate of bit transitions than their stego counterparts.
	 * 
	 * At the same time, we count the number of 1's that appear in the datastream. If they make up about 50%
	 * of the data, then the LSBs are random and should be checked out.
	 * 
	 * At the end of the method, variance is also calculated and displayed.
	 * 
	 * Most steganography tools must be trained before they can determine a priori which files have messages
	 * hidden inside them. Due to time constraints, this program only prints the relevant data.
	 * 
	 * @param infile The image file to analyze
	 * @throws IOException
	 */
	public static void analyze(String infile) throws IOException {
		//
		BufferedImage source = ImageIO.read(new File(infile));
		int width = source.getWidth(), height = source.getHeight();
		
		int n=0, s=0, e=0, w=0, c=0; //north, south....center
		int transitions = 0, nmatch = 0, temp;
		int bit8ones = 0, bit7ones = 0;
		
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				c = source.getRGB(x, y);
				if (y != 0) n = source.getRGB(x, y-1);
				if (y != height-1) s = source.getRGB(x, y+1);
				if (x != width-1) e = source.getRGB(x+1, y);
				if (x != 0) w = source.getRGB(x-1,y);
				
				//Go through the R, G, and B values of each pixel
				for (int shift = 16; shift >= 0; shift -= 8) {
					nmatch = 0;
					if (((c >> (shift+1)) & 1) == 1) bit7ones++;
					temp = (c >> shift) & 1;
					if (temp == 1) bit8ones++;
					if (temp != ((n >> shift) & 1)) nmatch++;
					if (temp != ((s >> shift) & 1)) nmatch++;
					if (temp != ((w >> shift) & 1)) nmatch++;
					if (temp != ((e >> shift) & 1)) nmatch++;
					if (nmatch >= 3) transitions++;
				}
			}
		}
		double transpercent = 100*(double)transitions / (double)(width*height*3);
		System.out.println("Percentage of bit transitions: "+transpercent);
		
		double leastbitpercent = 100*(double)bit8ones / (double)(width*height*3);
		System.out.println("Percentage of 1's in least bits: "+leastbitpercent);
		
		double least2percent = 100*(double)(bit8ones+bit7ones) / (double)(width*height*6);
		System.out.println("Percentage of 1's in least two bits: "+least2percent);
		
		double n0 = width*height*3 - bit8ones, N = width*height*3;
		double variance = (2*n0*bit8ones*(2*n0*bit8ones-N))/(N*N*(N-1));
		System.out.println("LSB variance: "+variance);
		
	}
}
