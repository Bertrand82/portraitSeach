package bg.faceRecognition;

import java.io.File;
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Computes an "face space" used for face recognition.
 *
 * This idea/algorhitm was derieved from Matthew A. Turn and Alex P. Pentland
 * paper, titled: "Face Recognition using Eigenfaces" (<a href=
 * "http://www.cs.ucsb.edu/~mturk/Papers/mturk-CVPR91.pdf">http://www.cs.ucsb.edu/~mturk/Papers/mturk-CVPR91.pdf</a>)
 * <br>
 * <br>
 * The way this algorhitm works is by treating face recognition as a
 * "two-dimensional recognition problem, taking advantage of the fact that faces
 * are normally upright and thus may be described by a small set of 2-D
 * characterisits views. Face images are projected onto a feature space ("face
 * space") that best encodes the variation among known face images. The face
 * space is defined by the "eigenfaces", which are the eigenvectors of the set
 * of faces; they do not necessarily correspond to isolated features such as
 * eyes, ears, and noses." (quoted from the paper's abstract) <br>
 * <br>
 * This work is released to the public with no license and no warranties. Do
 * with the code as you wish. <br>
 * <b>NOTE</b>: This package uses Jama for computing eigenvalues and
 * eigenvectors.
 *
 * Which is available at: <a href=
 * "http://math.nist.gov/javanumerics/jama/">http://math.nist.gov/javanumerics/jama/</a><br>
 * <br>
 * And in case you are worreid about its copyright states:<br>
 * <br>
 *
 * This software is a cooperative product of The MathWorks and the National
 * Institute of Standards and Technology (NIST) which has been released to the
 * public domain. Neither The MathWorks nor NIST assumes any responsibility
 * whatsoever for its use by other parties, and makes no guarantees, expressed
 * or implied, about its quality, reliability, or any other characteristic.
 *
 *
 *
 * <br>
 * <br>
 * 
 * @author Konrad Rzeszutek
 * @version 1.0
 */
public class EigenFaceComputation {

	/**
	 * Our "heuresticly" determined value of how many faces we want to compute. Just
	 * picked the value at random. You can fool around with it.
	 */
	private final static int MAGIC_NR = 11;

	public static FaceBundle submit(double[][] face_v, int width, int height, List<File> listFiles) {
		return submit(face_v, width, height, listFiles, false);
	}

	/**
	 * Compute the "face space" used for face recognition. The recognition is
	 * actually being carried out in the FaceBundle object, but the preparation of
	 * such object requires to do lots of computations. The steps are:
	 * <ol>
	 * <li>Compute an average face.
	 * <li>Build an covariance matrix.
	 * <li>Compute eigenvalues and eigenvector
	 * <li>Select only {@link MAGIC_NR} largest eigenvalues (and its corresponding
	 * eigenvectors)
	 * <li>Compute the faces using our eigenvectors
	 * <li>Compute our eigenspace for our given images.
	 * </ol>
	 * From then the rest of the algorithm (trying to match a face) has to be called
	 * in {@link FaceBundle}.
	 *
	 * @param face_v
	 * @param width
	 * @param height
	 * @return An FaceBundle usable for recognition.
	 *
	 */
	public static FaceBundle submit(double[][] face_v, int width, int height, List<File> listFiles, boolean debug) {

		int length = width * height;
		int nrfaces = face_v.length;
		int i, j, col, rows, pix, image;
		double temp = 0.0;
		double[][] faces = new double[nrfaces][length];

		double[] avgF = new double[length];

		/*
		 * Compute average face of all of the faces. 1xN^2
		 */
		for (pix = 0; pix < length; pix++) {
			temp = 0;
			for (image = 0; image < nrfaces; image++) {
				temp += face_v[image][pix];
			}
			avgF[pix] = temp / nrfaces;
		}

		/*
		 * Compute difference.
		 */

		for (image = 0; image < nrfaces; image++) {

			for (pix = 0; pix < length; pix++) {
				face_v[image][pix] = face_v[image][pix] - avgF[pix];
			}
		}
		/* Copy our face vector (MxN^2). We will use it later */
		System.arraycopy(face_v, 0, faces, 0, face_v.length);

		/*
		 * Build covariance matrix. MxM
		 */

		Matrix faceM = new Matrix(face_v, nrfaces, length);
		Matrix faceM_transpose = faceM.transpose();

		/*
		 * Covariance matrix - its MxM (nrfaces x nrfaces)
		 */
		Matrix covarM = faceM.times(faceM_transpose);

		/*
		 * Compute eigenvalues and eigenvector. Both are MxM
		 */
		EigenvalueDecomposition E = covarM.eig();

		double[] eigValue = diag(E.getD().getArray());
		double[][] eigVector = E.getV().getArray();

		/*
		 * We only need the largest associated values of the eigenvalues. Thus we sort
		 * them (and keep an index of them)
		 */
		int[] index = new int[nrfaces];
		double[][] tempVector = new double[nrfaces][nrfaces]; /* Temporary new eigVector */

		for (i = 0; i < nrfaces; i++) /* Enumerate all the entries */
			index[i] = i;

		doubleQuickSort(eigValue, index, 0, nrfaces - 1);

		// Put the index in inverse
		int[] tempV = new int[nrfaces];
		for (j = 0; j < nrfaces; j++)
			tempV[nrfaces - 1 - j] = index[j];
		/*
		 * for (int j = 0; j< nrfaces; j++) {
		 * System.out.println(temp[j]+" (was: "+index[j]+") "+eigValue[temp[j]]); }
		 */
		index = tempV;

		/*
		 * Put the sorted eigenvalues in the appropiate columns.
		 */
		for (col = nrfaces - 1; col >= 0; col--) {
			for (rows = 0; rows < nrfaces; rows++) {
				tempVector[rows][col] = eigVector[rows][index[col]];
			}
		}
		eigVector = tempVector;
		tempVector = null;
		eigValue = null;
		/*
		 * From this point on we don't care about our eigenvalues anymore.
		 *
		 *
		 * We multiply our faceM (MxN^2) with our new eigenvector (MxM), getting
		 * eigenfaces (MxN^2)
		 */
		Matrix eigVectorM = new Matrix(eigVector, nrfaces, nrfaces);
		eigVector = eigVectorM.times(faceM).getArray();

		/* Normalize our eigen vector matrix. */

		for (image = 0; image < nrfaces; image++) {
			temp = max(eigVector[image]); // Our max
			for (pix = 0; pix < eigVector[0].length; pix++)
				// Normalize
				eigVector[image][pix] = Math.abs(eigVector[image][pix] / temp);
		}

		/*
		 * And now the computation of wk (face space), which is a vector and is of some
		 * heuristically determind length. How does 11 sound?
		 *
		 * This is where we are using our copied faces vector (look at the the beginning
		 * of the method) - the reason is b/c the faceM made the array internally an
		 * covariance matrix.
		 */

		double[][] wk = new double[nrfaces][MAGIC_NR]; // M rows, 11 columns

		/*
		 * Compute our wk.
		 */

		for (image = 0; image < nrfaces; image++) {
			for (j = 0; j < MAGIC_NR; j++) {
				temp = 0.0;
				for (pix = 0; pix < length; pix++)
					temp += eigVector[j][pix] * faces[image][pix];
				wk[image][j] = Math.abs(temp);
			}
		}

		FaceBundle faceB = new FaceBundle(avgF, wk, eigVector, listFiles);

		return faceB;
	}

	/**
	 * Find the diagonal of an matrix.
	 *
	 * @param m
	 *            the number of rows and columns must be the same
	 * @return the diagonal of the matrix
	 */
	static double[] diag(double[][] m) {

		double[] d = new double[m.length];
		for (int i = 0; i < m.length; i++)
			d[i] = m[i][i];
		return d;
	}

	/**
	 * Divide each element in <code>v</code> by <code>b</code> No checking for
	 * division by zero.
	 *
	 * @param v
	 *            vector containing numbers.
	 * @param b
	 *            scalar used to divied each element in the v vector
	 *
	 * @return a vector having each element divided by <code>b</code> scalar.
	 *
	 */
	static void divide(double[] v, double b) {

		for (int i = 0; i < v.length; i++) {
			v[i] = v[i] / b;
		}

	}

	/**
	 * The sum of the vector.
	 *
	 * @param a
	 *            vector with numbers
	 * @return a scalar with the sum of each element in the <code>a</code> vector
	 */
	static double sum(double[] a) {

		double b = a[0];
		for (int i = 0; i < a.length; i++) {
			b += a[i];
		}
		return b;

	}

	/**
	 * The max of the vector a.
	 *
	 * @param a
	 *            the vector
	 *
	 * @return the sum of all the elements on <code>a</code>
	 */
	static double max(double[] a) {
		double b = a[0];
		for (int i = 0; i < a.length; i++) {
			if (a[i] > b) {
				b = a[i];
			}
		}

		return b;
	}

	/**
	 * Quick sort on a vector with an index.
	 *
	 * @param a
	 *            the array of numbers. This will be modified and sorted ascendingly
	 *            (smalles to highest)
	 * @param index
	 *            the index of the numbers as related to original location.
	 * @param lo
	 *            the index where to start from. Usually 0.
	 * @param hi
	 *            the index where to stop. Usually a.length()
	 */
	static void doubleQuickSort(double a[], int index[], int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		double mid;

		if (hi0 > lo0) {

			/*
			 * Arbitrarily establishing partition element as the midpoint of the array.
			 */
			mid = a[(lo0 + hi0) / 2];
			// loop through the array until indices cross
			while (lo <= hi) {
				/*
				 * find the first element that is greater than or equal to the partition element
				 * starting from the left Index.
				 */
				while ((lo < hi0) && (a[lo] < mid)) {
					++lo;
				}

				/*
				 * find an element that is smaller than or equal to the partition element
				 * starting from the right Index.
				 */
				while ((hi > lo0) && (a[hi] > mid)) {
					--hi;
				}

				// if the indexes have not crossed, swap
				if (lo <= hi) {
					swap(a, index, lo, hi);
					++lo;
					--hi;
				}
			}
			/*
			 * If the right index has not reached the left side of array must now sort the
			 * left partition.
			 */
			if (lo0 < hi) {
				doubleQuickSort(a, index, lo0, hi);
			}
			/*
			 * If the left index has not reached the right side of array must now sort the
			 * right partition.
			 */
			if (lo < hi0) {
				doubleQuickSort(a, index, lo, hi0);
			}
		}
	}

	static private void swap(double a[], int[] index, int i, int j) {
		double T;
		T = a[i];
		a[i] = a[j];
		a[j] = T;
		// Index
		index[i] = i;
		index[j] = j;
	}
}
