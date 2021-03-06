package bg.faceRecognition;

import java.io.File;

import bg.portrait.util.UtilFile;

public class ProcessFaceRecognition {

	private static File dir = new File("D:\\imagesTemp");

	public static File processImageNormalized(File imageNormalized) {

		try {
			EigenFaceProcessor processor = new EigenFaceProcessor();
			System.out.println("Constructing face-spaces from " + dir + " ...");
			processor.initProcessor(dir);
			System.out.println(" " + imageNormalized.exists() + "  " + imageNormalized.getAbsolutePath());
			FaceBundle result = processor.process(imageNormalized);
			System.out.println("result : " + result);
			if (result == null) {
				System.out.println("No result ! ");
				return null;
			} else {
				File fResult = new File(dir, result.getFileName());
				System.out.println("Most closly reseambling: " + result + " with " + processor.distanceBest + " distance.");
				System.out.println("Exists : "+fResult.exists()+"  "+fResult.getAbsolutePath());
				String hashMd5 = UtilFile.getHashFromFile(fResult);
				File dir = UtilFile.getDirRootFromHashMd5(hashMd5);
				System.out.println(" dir : "+dir.getAbsolutePath());
				return fResult;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
