package picture;

import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_EPS;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_ITER;
import static org.bytedeco.javacpp.opencv_core.CV_TERMCRIT_NUMBER;
import static org.bytedeco.javacpp.opencv_core.CV_WHOLE_SEQ;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateSeq;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvInRangeS;
import static org.bytedeco.javacpp.opencv_core.cvMerge;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvSetZero;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvSplit;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FILLED;
import static org.bytedeco.javacpp.opencv_imgproc.CV_LINK_RUNS;
import static org.bytedeco.javacpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_TOZERO;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindCornerSubPix;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetCentralMoment;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetSpatialMoment;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;
import static org.bytedeco.javacpp.opencv_imgproc.cvMinAreaRect2;
import static org.bytedeco.javacpp.opencv_imgproc.cvMoments;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_video.cvCalcOpticalFlowPyrLK;
import picture.PictureController;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvBox2D;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvMoments;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;
import org.bytedeco.javacv.OpenCVFrameConverter;

import helper.Vector;

public class OpticalFlowCalculator {

	private static final int MAX_CORNERS = 5;
	OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	private CvMemStorage storage = CvMemStorage.create();
	static int maxRed = 242;
	static int maxGreen = 99;
	static int maxBlue = 255;
	static int minRed = 0;
	static int minGreen = 0;
	static int minBlue = 134;
	static int smoother = 11;
	private int minThresh = 30;

	private int maxThresh = 5;
	private CvScalar rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
	private CvScalar rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom;

	CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);

	public OpticalFlowCalculator() {
	}

//	public int getMinThresh() {
//		return minThresh;
//	}
//
//	public void setMinThresh(int minThresh) {
//		this.minThresh = minThresh;
//	}
//
//	public int getMaxThresh() {
//		return maxThresh;
//	}
//
//	public void setMaxThresh(int maxThresh) {
//		this.maxThresh = maxThresh;
//	}

	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	public IplImage findMoments(IplImage img) {

		IplImage imghsv, imgbin;
		CvScalar Bminc = cvScalar(95, 150, 75, 0), Bmaxc = cvScalar(145, 255, 255, 0);
		CvScalar Rminc = cvScalar(150, 150, 75, 0), Rmaxc = cvScalar(190, 255, 255, 0);

		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax, areaC = 0;

		return null;
	}

	public IplImage findContoursBlue(IplImage img) {

		IplImage imghsv, imgbin;
		// Blue
		CvScalar minc = cvScalar(95, 150, 75, 0), maxc = cvScalar(145, 255, 255, 0);

		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv, CV_BGR2HSV);
		cvInRangeS(imghsv, minc, maxc, imgbin);

		cvFindContours(imgbin, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}

		cvSmooth(imgbin, imgbin, 2, smoother, 0, 0, 0);
		return imgbin;

	}
	
	public IplImage findContoursBlack(IplImage img) {
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;
		IplImage grayImage = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(img, grayImage, CV_BGR2GRAY);
		
		cvThreshold(grayImage, grayImage, 100,255, CV_THRESH_BINARY); 

		cvFindContours(grayImage, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(grayImage, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}
//		cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return grayImage;
	}


	public IplImage findContoursRed(IplImage img) {

		int hueLowerR = 120; // for red
		int hueUpperR = 180;
		IplImage imghsv, imgbin;

//		img = balanceWhite(img);
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv, CV_BGR2HSV);
		cvInRangeS(imghsv, cvScalar(hueLowerR, 100, 100, 0), cvScalar(hueUpperR, 255, 255, 0), imgbin);

		cvFindContours(imgbin, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}
//		cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return imgbin;
	}

	public IplImage findContoursGreen(IplImage img) {

		IplImage imghsv, imgbin;

		// Green
		CvScalar minc = cvScalar(40, 130, 75, 0), maxc = cvScalar(80, 255, 255, 0);
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv, CV_BGR2HSV);
		cvInRangeS(imghsv, minc, maxc, imgbin);

		cvFindContours(imgbin, storage, contour1, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS,
				cvPoint(0, 0));

		contour2 = contour1;

		while (contour1 != null && !contour1.isNull()) {
			areaC = cvContourArea(contour1, CV_WHOLE_SEQ, 1);
			if (areaC > areaMax)
				areaMax = areaC;
			contour1 = contour1.h_next();

		}

		while (contour2 != null && !contour2.isNull()) {
			areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
			if (areaC < areaMax) {
				cvDrawContours(imgbin, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			}
			contour2 = contour2.h_next();
		}
//		cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return imgbin;

	}

	public synchronized IplImage drawAndCalc(IplImage oldImg, IplImage newImg) {
		cvClearMemStorage(storage);
		// Load two images and allocate other structures

		CvSize cvSize = cvSize(oldImg.width(), oldImg.height());

		IplImage imgB = cvCreateImage(cvSize, newImg.depth(), 1);
		cvCvtColor(newImg, imgB, CV_BGR2GRAY);

		IplImage imgC = cvCreateImage(cvSize, newImg.depth(), 1);
		cvCopy(oldImg, imgC);

		IplImage dst = cvCreateImage(cvGetSize(imgC), imgC.depth(), 1);
		// cvCanny(imgC, dst, 100, 100, 3);

		CvSize img_sz = cvGetSize(oldImg);
		int win_size = 15;

		IplImage eig_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);
		IplImage tmp_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);

		IntPointer corner_count = new IntPointer(1).put(MAX_CORNERS);
		CvPoint2D32f cornersA = new CvPoint2D32f(MAX_CORNERS);

		CvArr mask = null;
		cvGoodFeaturesToTrack(oldImg, eig_image, tmp_image, cornersA, corner_count, 0.05, 5.0, mask, 3, 0, 0.04);

		cvFindCornerSubPix(oldImg, cornersA, corner_count.get(), cvSize(win_size, win_size), cvSize(-1, -1),
				cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.2));

		// Call Lucas Kanade algorithm
		BytePointer features_found = new BytePointer(MAX_CORNERS);
		FloatPointer feature_errors = new FloatPointer(MAX_CORNERS);

		CvSize pyr_sz = cvSize(oldImg.width() + 8, imgB.height() / 3);

		IplImage pyrA = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
		IplImage pyrB = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);

		CvPoint2D32f cornersB = new CvPoint2D32f(MAX_CORNERS);
		cvCalcOpticalFlowPyrLK(oldImg, imgB, pyrA, pyrB, cornersA, cornersB, corner_count.get(),
				cvSize(win_size, win_size), 5, features_found, feature_errors,
				cvTermCriteria(CV_TERMCRIT_NUMBER | CV_TERMCRIT_NUMBER, 120, 0.3), 0);

		for (int i = 0; i < corner_count.get(); i++) {
			cornersA.position(i);
			cornersB.position(i);
			CvPoint p0 = cvPoint(Math.round(cornersA.x()), Math.round(cornersA.y()));
			CvPoint p1 = cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y()));
			cvLine(imgC, p0, p1, CV_RGB(0, 0, 0), 3, CV_AA, 0);

			if (!p0.toString().equals(p1.toString())) {
				Vector v0 = convertToVector(p0.toString());
				Vector v1 = convertToVector(p1.toString());
			}
		}

		return imgC;
	}

	public synchronized IplImage findPolygons(IplImage coloredImage, IplImage filteredImage, int edgeNumber) {
		cvClearMemStorage(storage);
		// coloredImage = balanceWhite(coloredImage);
		CvSeq contour = new CvSeq(null);
		cvFindContours(filteredImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);

		// scale of center box
		int factor = 4;

		// find center points
		xleft = (int) coloredImage.width() / factor;
		xright = (int) (coloredImage.width() / factor) * (factor - 1);
		ytop = 0;
		ybot = coloredImage.height();
		// center of centerpoints y
		yCenterBottom = (coloredImage.height() / 3) * 2;
		yCenterTop = (coloredImage.height() / 3);

		// Find red point
		int posX = 0;
		int posY = 0;
		IplImage detectThrs = getThresholdImage(filteredImage);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = (int) (mom10 / area);
		posY = (int) (mom01 / area);

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == edgeNumber && cvContourArea(points) > 50) {
					// drawLines of Box
					cvDrawContours(coloredImage, points, CvScalar.WHITE, CvScalar.WHITE, -2, 2, CV_AA);
					// Counter for checking points in center box
				}
			}
			contour = contour.h_next();
		}
		return coloredImage;
	}

	public synchronized IplImage findQRFrames(IplImage coloredImage, IplImage filteredImage) {
		float known_distance = 100;
		float known_width = 27;
		float focalLength = (167 * known_distance) / known_width;

		cvClearMemStorage(storage);
		CvSeq contour = new CvSeq(null);
		cvFindContours(filteredImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_NONE);
		
		CvBox2D[] markers = new CvBox2D[3];
		markers[0] = new CvBox2D();
		markers[1] = new CvBox2D();
		markers[2] = new CvBox2D();
		int codeIndex = 0;
		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == 4 && cvContourArea(points) > 50) {
					markers[codeIndex] = cvMinAreaRect2(contour, storage);
					IplImage img1 = IplImage.create(coloredImage.width(), coloredImage.height(), coloredImage.depth(), 1);
					cvCvtColor(coloredImage, img1, CV_RGB2GRAY);
					cvCanny(img1, img1, 100, 200);
					
					IplImage mask = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U, coloredImage.nChannels());
					cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					IplImage crop = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U, coloredImage.nChannels());
					cvSetZero(crop);
					cvCopy(coloredImage, crop, mask);
					return crop;
				}
			}
			contour = contour.h_next();
		}
		return coloredImage;
	}

	private int checkPositionInCenter(int posx, int posy) {
		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		if (upperCenterCondition) {
			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition) {
			return 2;
		}

		if (bottomCenterCondition) {
			return 3;
		}

		return 0;

	}

	private boolean checkBoxForCenter(int posx, int posy) {

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition) {
			return true;
		} else {
			// System.out.println("not centered");
			return false;
		}

	}

	private boolean checkForCenter(int posx, int posy, int redx, int redy) {
		boolean redverticalCondition = redy > ytop && redy < ybot;
		boolean redhorizontalCondition = redx > xleft && redx < xright;

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition && redverticalCondition && redhorizontalCondition) {
			return true;
		} else {
			// System.out.println("not centered");
			return false;
		}

	}

	public Vector convertToVector(String point) {

		int firstIndex = point.toString().lastIndexOf(',');
		int xcord = Integer.parseInt(point.toString().substring(0, firstIndex).replaceAll("[^0-9]", ""));
		int ycord = Integer
				.parseInt(point.toString().substring(firstIndex, point.toString().length()).replaceAll("[^0-9]", ""));

		return new Vector(xcord, ycord);
	}

	private IplImage getThresholdImage(IplImage orgImg) {
		IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
		rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
		rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);

		//// System.out.println("RGBMIN R " + rgba_min.red() + "G " +
		//// rgba_min.green() + " B " + rgba_min.blue()
		// + "Smoothing: " + smoother);
		// System.out.println("RGBMAX R " + rgba_max.red() + "G " +
		//// rgba_max.green() + " B " + rgba_max.blue()
		// + "Smoothing: " + smoother);
		cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red
		cvSmooth(imgThreshold, imgThreshold, 2, smoother, 0, 0, 0);
		// cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}

	private IplImage getThresholdWhiteImage(IplImage orgImg) {
		IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
		rgba_min = CvScalar.GRAY;
		rgba_max = CvScalar.WHITE;

		//// System.out.println("RGBMIN R " + rgba_min.red() + "G " +
		//// rgba_min.green() + " B " + rgba_min.blue()
		// + "Smoothing: " + smoother);
		// System.out.println("RGBMAX R " + rgba_max.red() + "G " +
		//// rgba_max.green() + " B " + rgba_max.blue()
		// + "Smoothing: " + smoother);
		cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red
		cvSmooth(imgThreshold, imgThreshold, 2, smoother, 0, 0, 0);
		// cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}

	private IplImage balanceWhite(IplImage cvtImg) {
		// IplImage cvtImg = IplImage.create(newImg.width(), newImg.height(),
		// newImg.depth(), newImg.nChannels());
		// cvCvtColor(newImg, cvtImg,CV_BGR2HSV);

		IplImage channel1 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel2 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel3 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		cvSplit(cvtImg, channel1, channel2, channel3, null);
		cvEqualizeHist(channel1, channel1);
		cvEqualizeHist(channel2, channel2);
		cvEqualizeHist(channel3, channel3);
		cvMerge(channel1, channel2, channel3, null, cvtImg);

		// cvCvtColor(cvtImg, newImg, CV_HSV2BGR);

		return cvtImg;
	}
}
