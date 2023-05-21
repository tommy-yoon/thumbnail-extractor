import java.io.*;

public class Extractor {
	private static final byte[] JPG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
	private static final byte[] JPG_FOOTER = {(byte) 0xFF, (byte) 0xD9};
	private static final byte[] PNG_HEADER = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};
	private static final byte[] PNG_FOOTER = {(byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44, (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82};
    
    public static void main(String[] args) {
    	 String sourceFileName = "";
    	if (args.length < 1) {
        	sourceFileName = "Thumbs.db";
        } else {
        	sourceFileName = args[0];
        }

        final String fileNamePrefix = "foundImage";
        int fileNameIndex = 0;
        final String fileNameExtension = "jpg";
        long currentPosition = 0;
        int bytesRead = 0;

        try {
            RandomAccessFile file = new RandomAccessFile(sourceFileName, "r");
            byte[] buffer = new byte[1024];
            byte[] header = JPG_HEADER;
            byte[] footer = JPG_FOOTER;
            long fileLength = file.length();
            long headerPosition = -1;

            // if the file is not empty and the current pointer(plus header&tail's length) didn't reach EOF
            while (currentPosition + header.length + footer.length -1 < fileLength) { // EOF?
                file.seek(currentPosition);
                bytesRead = file.read(buffer);

                // find header
            	int headerIndex = findSignature(buffer, bytesRead, header, 0);
            	if (headerIndex != -1) { // found header?
            		currentPosition += headerIndex;
            		headerPosition = currentPosition;
            		
            		while (headerPosition > 0 && currentPosition < fileLength) { // EOF?
						file.seek(currentPosition);
						bytesRead = file.read(buffer);
						
						// find tail
						int tailIndex = findSignature(buffer, bytesRead, footer, header.length);
						if (tailIndex != -1) { // found tail?
							// Save the image data between the header and tail
							saveImageData(sourceFileName, fileNamePrefix + fileNameIndex++ + "." + fileNameExtension,
									headerPosition, currentPosition + tailIndex + footer.length);
							// reset header position
							headerPosition = -1;
							// push forward the current position
							currentPosition += tailIndex + footer.length;
						} else {
							// The tail bytes can be fragmented across two buffer windows
		            		// To solve the issue, push back the starting pointer 
							currentPosition += bytesRead;
							currentPosition -= footer.length - 1;
						} 
					}
            	} else {
            		currentPosition += bytesRead;
            		// The header bytes can be fragmented across two buffer windows
            		// To solve the issue, push back the starting pointer 
            		currentPosition -= header.length - 1;                	
            	}
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int findSignature(byte[] buffer, int bytesRead, byte[] tail, int startIndex) {
        for (int i = startIndex; i < bytesRead - tail.length; i++) {
            boolean match = true;
            for (int j = 0; j < tail.length; j++) {
                if (buffer[i + j] != tail[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static void saveImageData(String sourceFilename, String targetFileName, long startIndex, long endIndex) {
    	System.out.println(sourceFilename + ":" + targetFileName + ":" + startIndex + ":" + endIndex);
    	try {
            RandomAccessFile file = new RandomAccessFile(sourceFilename, "r");
            FileOutputStream fos = new FileOutputStream(targetFileName);

            file.seek(startIndex);
            byte[] buffer = new byte[1024];
            long remainingBytes = endIndex - startIndex;

            while (remainingBytes > 0) {
                int bytesRead = file.read(buffer, 0, (int) Math.min(buffer.length, remainingBytes));
                if (bytesRead == -1) {
                    break;
                }

                fos.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }

            file.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
