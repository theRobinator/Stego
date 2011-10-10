import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class Stego {
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		if (args.length == 0) {
			System.out.println("Usage: java -jar Stego.jar [e|d|a]\n\te : encrypt\n\td : decrypt\n\ta : analyze");
			
			
		//Put a message into a file
		}else if (args[0].equals("e")) {
			System.out.print("Enter the path to the normal image: ");
			String infile = in.nextLine();
			System.out.print("Enter the path to the output image: ");
			String outfile = in.nextLine();
			if (!outfile.endsWith(".png")) outfile += ".png";
			System.out.print("Enter your passphrase to recover the data: ");
			String pass = in.nextLine();
			System.out.println("Enter the message to hide in the file. Press ctrl+d when done.");
			String msg = "";
			while (in.hasNext()) msg += in.nextLine()+"\n";
			DesEncrypter enc = new DesEncrypter(pass);
			msg = enc.encrypt(msg);
			
			System.out.print("Embedding...");
			try {
				StegImage.writeStego(infile, msg, outfile,pass);
			}catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
			
			System.out.println("Message hidden successfully!");
			
		//Grab a message from a file
		}else if (args[0].equals("d")) {
			System.out.print("Enter the path to the input image: ");
			String infile = in.nextLine();
			System.out.print("Enter your passphrase: ");
			String pass = in.nextLine();
			String msg = "";
			try {
				msg = StegImage.readStego(infile,pass);
				msg = new DesEncrypter(pass).decrypt(msg);
			}catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}catch (Exception e) { //This happens when no message can be found
				e.printStackTrace();
				System.out.println("No message could be found in the picture.");
				System.out.print("Dump the data in the LSBs to a file anyway [y/n]? ");
				String ans = in.nextLine();
				if (ans.equalsIgnoreCase("y")) {
					System.out.print("Enter the name of the file to save to: ");
					try {
						FileOutputStream out = new FileOutputStream(new File(in.nextLine()));
						out.write(StegImage.readDirect(infile));
						out.flush();
						out.close();
						out = null;
					} catch (IOException err) {
						System.err.println(err.getMessage());
						System.exit(1);
					}
				}else {
					System.exit(0);
				}
			}
			if (msg == null) { // incorrect passpharase
				System.out.println("Your passphrase was incorrect.");
			}else {
				System.out.println(msg);
			}
			
			
		}else if (args[0].equals("a")) {
			System.out.print("Enter the path to the input image: ");
			String infile = in.nextLine();
			try {
				StegImage.analyze(infile);
			}catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}else {
			System.out.println("Usage: java -jar Stego.jar [e|d|a]\n\te : encrypt\n\td : decrypt\n\ta : analyze");
		}
	}
}
