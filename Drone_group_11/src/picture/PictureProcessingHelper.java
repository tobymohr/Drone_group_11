package picture;

import static org.bytedeco.javacpp.helper.opencv_core.*;

import picture.PictureController;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.helper.opencv_core.*;

import picture.PictureController;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc.CvMoments;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import static org.bytedeco.javacpp.opencv_core.*;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageProducer;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import helper.Circle;
import helper.CustomPoint;
import helper.Vector;

public class PictureProcessingHelper {

	private static final int MAX_CORNERS = 200;
	OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	private float distance;
	Java2DFrameConverter converter1 = new Java2DFrameConverter();
	private CvMemStorage storage = CvMemStorage.create();
	IplImage dest = null;
	IplImage warp = null;
	static int maxRed = 242;
	static int maxGreen = 99;
	static int maxBlue = 255;
	static int minRed = 0;
	static int minGreen = 0;
	static int minBlue = 134;
	static int smoother = 11;
	public String code = "";
	private int i = 0;
	CvPoint2D32f c1 = new CvPoint2D32f(4);
	CvPoint2D32f c2 = new CvPoint2D32f(4);

	private CvScalar rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
	private CvScalar rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom, dist;
	QRCodeReader reader = new QRCodeReader();
	private Result qrCodeResult;
	LuminanceSource source;
	BinaryBitmap bitmap;
	List<CvPoint> corners = new ArrayList<CvPoint>();
	IplImage mask;
	IplImage crop;
	IplImage imgWarped;
	IplImage imgSharpened;

	CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);
	// CanvasFrame canvas = new CanvasFrame("Warped Image");
	private int j = 0;
	private Point2f vertices;

	public PictureProcessingHelper() {
		// canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	public void transformForDistance() {
	}

	private int closestPoint(List<CvSeq> pointsList, CvSeq markerMiddle) {
		double qrMarkerSize = cvContourArea(markerMiddle);
		double distance = Math.abs(cvContourArea(pointsList.get(0)) - qrMarkerSize);
		int index = 0;
		for (int i = 1; i < pointsList.size(); i++) {
			double newDistance = Math.abs(cvContourArea(pointsList.get(i)) - qrMarkerSize);
			if (newDistance < distance) {
				index = i;
				distance = newDistance;
			}
		}
		return index;
	}

	public Mat findContoursBlackMat(Mat img) {
		MatVector contour1 = new MatVector();
		// cvtColor(img, grayImageMat, CV_BGR2GRAY);
		Mat matHSV = new Mat(img.arraySize(), img.arrayDepth(), img.arrayChannels());
		cvtColor(img, matHSV, CV_RGB2HSV);
		Mat scalar1 = new Mat(new Scalar(0, 0, 0, 0));
		Mat scalar2 = new Mat(new Scalar(180, 255, 38, 0));

		inRange(matHSV, scalar1, scalar2, matHSV);
		findContours(matHSV, contour1, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());

		for (int i = 0; i < contour1.size(); i++) {
			drawContours(matHSV, contour1, i, new Scalar(0, 0, 0, 0), 3, CV_FILLED, null, 1, new opencv_core.Point());
		}
		return matHSV;
	}

	public Mat findContoursRedMat(Mat img) {

		MatVector matContour = new MatVector();

		Mat mathsv3 = new Mat(img.arraySize(), CV_8U, 3);// cvCreateImage(cvGetSize(img),
															// 8, 3);
		Mat mathueLower = new Mat(img.arraySize(), CV_8U, 1);
		Mat mathueUpper = new Mat(img.arraySize(), CV_8U, 1);
		Mat imgbin3 = new Mat(img.arraySize(), CV_8U, 3);

		cvtColor(img, mathsv3, CV_BGR2HSV);

		Mat scalar1 = new Mat(new Scalar(0, 100, 100, 0));
		Mat scalar2 = new Mat(new Scalar(10, 255, 255, 0));
		Mat scalar3 = new Mat(new Scalar(160, 100, 100, 0));
		Mat scalar4 = new Mat(new Scalar(179, 255, 255, 0));
		// Two ranges to get full color spectrum
		inRange(mathsv3, scalar1, scalar2, mathueLower);
		inRange(mathsv3, scalar3, scalar4, mathueUpper);
		addWeighted(mathueLower, 1.0, mathueUpper, 1.0, 0.0, imgbin3);
		findContours(imgbin3, matContour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());
		for (int i = 0; i < matContour.size(); i++) {
			drawContours(imgbin3, matContour, i, new Scalar(0, 0, 0, 0), 3, CV_FILLED, null, 1,
					new opencv_core.Point());
		}
		return imgbin3;
	}

	public Mat findContoursGreenMat(Mat img) {

		MatVector matContour = new MatVector();

		Mat imghsv = new Mat(img.arraySize(), 8, 3);
		Mat imgbin = new Mat(img.arraySize(), 8, 1);
		cvtColor(img, imghsv, CV_BGR2HSV);

		Mat scalar1 = new Mat(new Scalar(35, 75, 6, 0));
		Mat scalar2 = new Mat(new Scalar(75, 220, 220, 0));
		// Two ranges to get full color spectrum
		inRange(imghsv, scalar1, scalar2, imgbin);

		findContours(imgbin, matContour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());

		for (int i = 0; i < matContour.size(); i++) {
			drawContours(imgbin, matContour, i, new Scalar(0, 0, 0, 0), 3, CV_FILLED, null, 1, new opencv_core.Point());
		}
		return imgbin;
	}
	
	


	public Mat warpImage(Mat crop, RotatedRect rect) {
		vertices = new Point2f(4);
		CvMemStorage storage = CvMemStorage.create();
		rect.points(vertices);
		int angle = Math.abs((int) rect.angle());
		float w = crop.cols();
		float x = distance;
		float y = 28;
		float z = vertices.position(1).x() - vertices.position(0).x();
		z = Math.abs(z);
//		System.out.println("Width in pix " + z);
		double sum = (w*y)/(2*x*z);
		double AOV = Math.atan(sum)*2;
		AOV = Math.toDegrees(AOV);
//		System.out.println("AOV " + AOV);
		
		Point tl = null;
		Point tr = null;
		Point br = null;
		Point bl = null;
		int height;
		int width;
		if (angle >= 0 && angle < 10) {
			tl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			tr = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			br = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			bl = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			height = (int) rect.size().height();
			width = (int) rect.size().width();
		} else {
			tl = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			tr = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			br = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			bl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			height = (int) rect.size().width();
			width = (int) rect.size().height();
		}

		float[] sourcePoints = { tl.x(), tl.y(), tr.x(), tr.y(), bl.x(), bl.y(), br.x(), br.y() };
		float[] destinationPoints = { 0.0f, 0.0f, width, 0.0f, 0.0f, height, width, height };
		CvMat homography = cvCreateMat(3, 3, CV_32FC1);
		cvGetPerspectiveTransform(sourcePoints, destinationPoints, homography);
		Mat copy = crop.clone();
		IplImage dest = new IplImage(copy);
		cvWarpPerspective(dest, dest, homography, CV_INTER_LINEAR, CvScalar.ZERO);
		dest = cropImage(dest, 0, 0, width, height);
		Mat m = cvarrToMat(dest.clone());
		cvRelease(dest);
		cvClearMemStorage(storage);
		
		return m;

	}

	public IplImage cleanImageSmoothingForOCR(IplImage srcImage) {
		IplImage destImage = cvCreateImage(cvGetSize(srcImage), IPL_DEPTH_8U, 1);
		cvCvtColor(srcImage, destImage, CV_BGR2GRAY);
		cvThreshold(destImage, destImage, 0, 255, CV_THRESH_OTSU);
		return destImage;
	}

	private IplImage cropImage(IplImage dest, int fromX, int fromY, int toWidth, int toHeight) {
		cvSetImageROI(dest, cvRect(fromX, fromY, toWidth, toHeight));
		IplImage dest2 = cvCloneImage(dest);
		cvCopy(dest, dest2);

		return dest2;
	}

	@SuppressWarnings("resource")
	public Mat extractQRImage(Mat img0) {
		
		Mat img1 = new Mat(img0.arraySize(), CV_8UC1, 1);
		cvtColor(img0, img1, CV_RGB2GRAY);
		Canny(img1, img1, 75, 200);
		MatVector matContour = new MatVector();
		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);
		
		Mat crop = new Mat(img0.rows(), img0.cols(), CV_8UC3, Scalar.BLACK);
		Mat mask = new Mat(img1.rows(), img1.cols(), CV_8UC1, Scalar.BLACK);
		for (int i = 0; i < matContour.size(); i++) {
			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > 1000) {
				drawContours(mask, matContour, i, Scalar.WHITE, CV_FILLED, 8, null, 1, null);
				img0.copyTo(crop, mask);
				RotatedRect rect = minAreaRect(matContour.get(i));
				crop = warpImage(crop, rect);
				scanQrCode(crop);
				distance = calcDistance(rect);
				checkAngles(rect);
				drawContours(img0, matContour, i, Scalar.WHITE, 2, 8, null, 1, null);
				putText(img0, "" + (int) distance, new Point((int)rect.center().x() - 25, (int)rect.center().y() + 60), 1, 2, Scalar.RED, 2, 8, false);
				putText(img0, code, new Point((int)rect.center().x() - 25, (int)rect.center().y() + 80), 1, 2, Scalar.GREEN, 2, 8, false);
				crop = new Mat(img0.rows(), img0.cols(), CV_8UC3, Scalar.BLACK);
				mask = new Mat(img1.rows(), img1.cols(), CV_8UC1, Scalar.BLACK);
			}
		}
		return img0;
	}
	
	public void checkAngles(RotatedRect rect) {
		Point2f vertices = new Point2f(4);
		rect.points(vertices);
		int angle = Math.abs((int) rect.angle());
		Point tl = null;
		Point tr = null;
		Point br = null;
		Point bl = null;
		if (angle >= 0 && angle < 10) {
			tl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
			tr = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			br = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			bl = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
		} else {
			tl = new Point((int) vertices.position(2).x(), (int) vertices.position(2).y());
			tr = new Point((int) vertices.position(3).x(), (int) vertices.position(3).y());
			br = new Point((int) vertices.position(0).x(), (int) vertices.position(0).y());
			bl = new Point((int) vertices.position(1).x(), (int) vertices.position(1).y());
		}
		System.out.println("----------");
		System.out.println(calculateAngle(tl, tr, bl));
		System.out.println(calculateAngle(tr, tl, br));
		System.out.println(calculateAngle(bl, tl, br));
		System.out.println(calculateAngle(br, bl, tr));
		System.out.println("----------");
	}
	
	public boolean scanQrCode(Mat srcImage){
		BufferedImage qrCode = converter1.convert(converter.convert(srcImage));
		source = new BufferedImageLuminanceSource(qrCode);
		bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			Result detectionResult = reader.decode(bitmap);
			code = detectionResult.getText();
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	public float calcDistance(RotatedRect rect){
		float knownDistance = 214;
		float height = 0;
		float focalLength = 103 * knownDistance;
		int angle = Math.abs((int) rect.angle());
		if (angle >= 0 && angle < 10) {
			height =  rect.size().height();
		} else {
			height =  rect.size().width();
		}
		return focalLength/height;
	}
	
	

	
	public Mat center(Mat img, Mat filter) {
		
		MatVector matContour = new MatVector();

		
		Mat img1 = new Mat(img.arraySize(), CV_8UC1, 1);
		
		cvtColor(img, img1, CV_RGB2GRAY);
		Canny(img1, img1, 75, 200);
		
		findContours(img1, matContour, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);


		int factor = 5;

		// find center points
		xleft = (int) img.arrayWidth() / factor;
		xright = (int) (img.arrayWidth() / factor) * (factor - 1);
		ytop = 0;
		ybot = img.arrayHeight();
		// center of centerpoints y
		yCenterBottom = (img.arrayHeight() / 3) * 2;
		yCenterTop = (img.arrayHeight() / 3);

		// System.out.println(img.arrayHeight() + "h");
		// System.out.println(img.arrayWidth() + "w");
		// Make center points
		Point pointTopLeft = new Point(xleft, ytop);
		Point pointBottomLeft = new Point(xleft, ybot);
		Point pointTopRight = new Point(xright, ytop);
		Point pointRightBottom = new Point(xright, ybot);

		// Make upper line points in center
		Point pointCenterUpperLeft = new Point(xleft, yCenterTop);
		Point pointCenterUpperRight = new Point(xright, yCenterTop);
		// Make bottom line points in center
		Point pointCenterBottomLeft = new Point(xleft, yCenterBottom);
		Point pointCenterBottomRight = new Point(xright, yCenterBottom);

		// Find red point
		int posX = 0;
		int posY = 0;

		Moments moments = moments(filter);
		double mom10 = moments.m10();
		double mom01 = moments.m01();
		double mom00 = moments.m00();
		posX = (int) (mom10 / mom00);
		posY = (int) (mom01 / mom00);
		
		// Draw Center
	
		int counter = 0;
		for (int i = 0; i < matContour.size(); i++) {
			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			if (matContour.get(i).total()== 4 && contourArea(matContour.get(i))>1000 && contourArea(matContour.get(i)) <10000) {
				Point2f centerPoint = minAreaRect(matContour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(img, p, p, Scalar.BLACK, 16, CV_AA, 0);

				drawContours(img1, matContour, i, Scalar.WHITE, CV_FILLED, 8, null, 1, null);
				
				

				for (int j = 0; j < matContour.get(i).total(); j++) {
					Point2f centerPointTemp = minAreaRect(matContour.get(i)).center();
					opencv_core.Point ptemp = new opencv_core.Point((int) centerPointTemp.x(),
							(int) centerPointTemp.y());
					line(img, ptemp, ptemp, Scalar.BLACK, 16, CV_AA, 0);
					if (checkBoxForCenter(ptemp.x(), ptemp.y(),dist)) {
						counter++;
					}
				}

				if (counter == matContour.get(i).total()) {
					// check in which part of center box is.
					switch (checkPositionInCenter(p.x(), p.y(),dist)) {
					case 1:
						line(img, p, p, Scalar.BLUE, 16, CV_AA, 0);
						break;
					case 2:
						line(img, p, p, Scalar.RED, 16, CV_AA, 0);
						dist = 0;
						break;
					case 3:
						line(img, p, p, Scalar.GREEN, 16, CV_AA, 0);
						break;
					default:
						break;

					}

				}
			}
		}
		line(img, pointTopLeft, pointTopRight, new Scalar(255, 0, 255, 0));
		line(img, pointTopRight, pointRightBottom, new Scalar(255, 0, 255, 0));
		line(img, pointRightBottom, pointBottomLeft, new Scalar(255, 0, 255, 0));
		line(img, pointBottomLeft, pointTopLeft, new Scalar(255, 0, 255, 0));
		// Draw upper line
		line(img, pointCenterUpperLeft, pointCenterUpperRight, new Scalar(0, 0, 255, 0));
		line(img, pointCenterBottomLeft, pointCenterBottomRight, new Scalar(0, 255, 0, 0));
		
		

		return img;
	}


	private int closestPoint(List<Mat> pointsList, Mat markerMiddle) {
		double qrMarkerSize = contourArea(markerMiddle);
		double distance = Math.abs(contourArea(pointsList.get(0)) - qrMarkerSize);
		int index = 0;
		for (int i = 1; i < pointsList.size(); i++) {
			double newDistance = Math.abs(contourArea(pointsList.get(i)) - qrMarkerSize);
			if (newDistance < distance) {
				index = i;
				distance = newDistance;
			}
		}
		return index;
	}

	public IplImage findContoursGreen(IplImage img) {

		IplImage imghsv, imgbin;

		// Green
		CvScalar minc = cvScalar(35, 75, 6, 0), maxc = cvScalar(75, 255, 255, 0);
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
			cvDrawContours(imgbin, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0, CV_FILLED, 8, cvPoint(0, 0));
			contour1 = contour1.h_next();

		}

		// while (contour2 != null && !contour2.isNull()) {
		// areaC = cvContourArea(contour2, CV_WHOLE_SEQ, 1);
		// if (areaC < areaMax) {
		// cvDrawContours(imgbin, contour2, CV_RGB(0, 0, 0), CV_RGB(0, 0, 0), 0,
		// CV_FILLED, 8, cvPoint(0, 0));
		// }
		//
		// contour2 = contour2.h_next();
		// }

		return imgbin;

	}

	public IplImage convertMatToIplImage(Mat mat) {
		return converter.convert(converter.convert(mat));
	}

	public IplImage opticalFlowOnDrones(IplImage imgA, IplImage newFrame) {
		// // Load two images and allocate other structures
		// CvSize cvSize = cvSize(imgA.width(), imgA.height());
		//
		// imgB = cvCreateImage(cvSize, newFrame.depth(), 1);
		// cvCvtColor(newFrame, imgB, CV_BGR2GRAY);
		//
		// imgC = cvCreateImage(cvSize, newFrame.depth(), 1);
		// cvCopy(imgA, imgC);
		//
		// cvThreshold(imgC, imgC, 100, 255, CV_THRESH_TOZERO);
		//
		// CvSize img_sz = cvGetSize(imgA);
		// int win_size = 15;
		//
		// eig_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);
		// tmp_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);
		//
		// IntPointer corner_count = new IntPointer(1).put(MAX_CORNERS);
		// CvPoint2D32f cornersA = new CvPoint2D32f(MAX_CORNERS);
		//
		// CvArr mask = null;
		// cvGoodFeaturesToTrack(imgA, eig_image, tmp_image, cornersA,
		// corner_count, 0.05, 5.0, mask, 3, 0, 0.04);
		//
		// cvFindCornerSubPix(imgA, cornersA, corner_count.get(),
		// cvSize(win_size, win_size), cvSize(-1, -1),
		// cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
		//
		// // Call Lucas Kanade algorithm
		// BytePointer features_found = new BytePointer(MAX_CORNERS);
		// FloatPointer feature_errors = new FloatPointer(MAX_CORNERS);
		//
		// CvSize pyr_sz = cvSize(imgA.width() + 8, imgB.height() / 3);
		//
		// pyrA = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
		// pyrB = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
		//
		// CvPoint2D32f cornersB = new CvPoint2D32f(MAX_CORNERS);
		//
		// cvCalcOpticalFlowPyrLK(imgA, imgB, pyrA, pyrB, cornersA, cornersB,
		// corner_count.get(),
		// cvSize(win_size, win_size), 5, features_found, feature_errors,
		// cvTermCriteria(CV_TERMCRIT_NUMBER | CV_TERMCRIT_NUMBER, 20, 0.3), 0);
		//
		// // Put lines on the screen along with dots
		// for (int i = 0; i < corner_count.get(); i++) {
		// if (features_found.get(i) == 0 || feature_errors.get(i) > 550) {
		// continue;
		// }
		// cornersA.position(i);
		// cornersB.position(i);
		// CvPoint p0 = cvPoint(Math.round(cornersA.x()),
		// Math.round(cornersA.y()));
		// CvPoint p1 = cvPoint(Math.round(cornersB.x()),
		// Math.round(cornersB.y()));
		// cvLine(imgC, p0, p1, CV_RGB(255, 255, 255), 3, CV_AA, 0);
		//
		// if (!p0.toString().equals(p1.toString())) {
		// Vector v0 = convertToVector(p0.toString());
		// Vector v1 = convertToVector(p1.toString());
		// Vector newVector = v0.subtract(v1);
		//
		// if(newVector.y < -10){
		// System.out.println("Moving Down");
		// }
		//
		// if(newVector.y > 10){
		// System.out.println("Moving Up");
		// }
		//
		// if(newVector.x > 10){
		// System.out.println("Moving Left");
		// }
		//
		// if(newVector.x < -10){
		// System.out.println("Moving Right");
		// }
		// }
		// }
		return null;
	}

	public synchronized IplImage findPolygons(IplImage coloredImage, IplImage filteredImage, int edgeNumber) {
		cvClearMemStorage(storage);
		double angle = 0;
		int polygonCount = 0;
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

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if ((points.total() > 2 && points.total() < 6) && cvContourArea(points) > 1200
						&& cvContourArea(points) < 50000) {

					int posX = 0;
					int posY = 0;
					CvMoments moments = new CvMoments();
					cvMoments(filteredImage, moments, 1);
					double mom10 = cvGetSpatialMoment(moments, 1, 0);
					double mom01 = cvGetSpatialMoment(moments, 0, 1);
					double area = cvGetCentralMoment(moments, 0, 0);

					posX = (int) (mom10 / area);
					posY = (int) (mom01 / area);

					CvPoint p0 = cvPoint(posX, posY);
					cvLine(coloredImage, p0, p0, CV_RGB(255, 0, 0), 16, CV_AA, 0);
					// System.out.println(posX);
					// System.out.println(posY);
					cvDrawContours(coloredImage, points, CvScalar.WHITE, CvScalar.WHITE, -2, 2, CV_AA);

				}
			}
			contour = contour.h_next();
		}
		// System.out.println(polygonCount);
		return coloredImage;
	}

	public synchronized Mat findPolygonsMat(Mat coloredImage, Mat filteredImage, int edgeNumber) {

		MatVector contour = new MatVector();
		findContours(filteredImage, contour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());

		// System.out.println(contour.size() + "");
		// find center points
		for (int i = 0; i < contour.size(); i++) {
			approxPolyDP(contour.get(i), contour.get(i), 0.02 * arcLength(contour.get(i), true), true);
			if (contour.get(i).total() == 4 && contourArea(contour.get(i)) > 150) {
				Point2f centerPoint = minAreaRect(contour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(coloredImage, p, p, new Scalar(255, 0, 0, 0), 16, CV_AA, 0);
				drawContours(coloredImage, contour, i, new Scalar(0, 0, 0, 0), 3, CV_AA, null, 1,
						new opencv_core.Point());

			}

			// if (contourArea(contour.get(i)) > 150 &&
			// contourArea(contour.get(i)) < 10000) {
			// drawLines of Box
			// drawContours(coloredImage, contour, i, new Scalar(0,0,0,3))
			//
			//
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
				CV_CHAIN_APPROX_SIMPLE);

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
					IplImage img1 = IplImage.create(coloredImage.width(), coloredImage.height(), coloredImage.depth(),
							1);
					cvCvtColor(coloredImage, img1, CV_RGB2GRAY);
					cvCanny(img1, img1, 100, 200);
					IplImage mask = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U,
							coloredImage.nChannels());
					cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					IplImage crop = IplImage.create(coloredImage.width(), coloredImage.height(), IPL_DEPTH_8U,
							coloredImage.nChannels());

					//
					cvCopy(coloredImage, crop, mask);
					return crop;
				}
			}
			contour = contour.h_next();
		}
		return null;
	}

	@SuppressWarnings("resource")
	public synchronized IplImage fRFrames(IplImage image) {
		float known_distance = 100;
		float known_width = 27;
		float focalLength = (167 * known_distance) / known_width;

		cvClearMemStorage(storage);
		// image = balanceWhite(image);
		IplImage grayImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCvtColor(image, grayImage, CV_BGR2GRAY);
		IplImage orgImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCopy(image, orgImage);
		// grayImage = getThresholdBlackImage(grayImage);

		CvSeq contour = new CvSeq(null);
		cvFindContours(grayImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);

		// center dots
		int factor = 3;

		// find center points
		xleft = (int) image.width() / factor;
		xright = (int) (image.width() / factor) * (factor - 1);
		ytop = (int) image.height() / factor;
		ybot = (int) (image.height() / factor) * (factor - 1);

		// Make center points
		CvPoint pointTopLeft = cvPoint(xleft, ytop);
		CvPoint pointBottomLeft = cvPoint(xleft, ybot);
		CvPoint pointTopRight = cvPoint(xright, ytop);
		CvPoint pointRightBottom = cvPoint(xright, ybot);

		// Find red point
		int posX = 0;
		int posY = 0;
		IplImage detectThrs = getThresholdImage(grayImage);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = (int) (mom10 / area);
		posY = (int) (mom01 / area);
		CvBox2D[] markers = new CvBox2D[3];
		markers[0] = new CvBox2D();
		markers[1] = new CvBox2D();
		markers[2] = new CvBox2D();
		IplImage crop = IplImage.create(orgImage.width(), orgImage.height(), IPL_DEPTH_8U, orgImage.nChannels());
		cvSetZero(crop);
		int codeIndex = 0;

		while (contour != null && !contour.isNull()) {

			// Draw red point
			cvLine(image, pointTopLeft, pointTopRight, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointTopRight, pointRightBottom, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointRightBottom, pointBottomLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointBottomLeft, pointTopLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);

			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (cvContourArea(points) > 100) {
					for (int i = 0; i < points.total(); i++) {
						//// cvLine(image, p0, p0, CV_RGB(255, 0, 0), 3, CV_AA,
						//// 0);
						CvPoint v = new CvPoint(cvGetSeqElem(points, i));
						cvDrawContours(image, points, CvScalar.RED, CvScalar.RED, -2, 2, CV_AA);
						CvPoint p0 = cvPoint(posX, posY);
						// Draw red point

						markers[codeIndex] = cvMinAreaRect2(contour, storage);
						IplImage img1 = IplImage.create(orgImage.width(), orgImage.height(), orgImage.depth(), 1);
						cvCvtColor(orgImage, img1, CV_RGB2GRAY);
						cvCanny(img1, img1, 100, 200);

						IplImage mask = IplImage.create(orgImage.width(), orgImage.height(), IPL_DEPTH_8U,
								orgImage.nChannels());
						cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);

						cvCopy(orgImage, crop, mask);
						return mask;
					}
				}
			}

			contour = contour.h_next();
		}
		return image;
	}

	public String getQrCode() {
		return code;
	}

	public double getDistance() {
		return distance;
	}

	private int checkPositionInCenter(int posx, int posy, int dist) {

		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		boolean leftCenterCondition = posx < xleft;
		boolean rightCenterCondition = posx > xright;
		
		if (upperCenterCondition) {
			dist = 0;
			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition) {
			dist = 0;
			return 2;
		}

		if (bottomCenterCondition) {
			dist = 0;
			return 3;
		}
		if(leftCenterCondition){
			dist = 0;
			return 4;
		}
		
		if(rightCenterCondition){
			dist = 0;
			return 5;
		}
		return 0;

	}

	private boolean checkBoxForCenter(int posx, int posy, int dist) {
		
		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition) {
			dist = 50;
			return true;
		} else {
			return false;
		}

	}
	
	private boolean checkCenterDistance(int posx, int posy, int dist){
		
		dist = 50; 
		
		checkBoxForCenter(posx, posy, dist);
		
		checkPositionInCenter(posx, posy, dist);
		
		
		
		return false;
		
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

	public Mat erodeAndDilate(Mat thresh) {

		Mat erodeElement = getStructuringElement(MORPH_RECT, new Size(3, 3));
		Mat dilateElement = getStructuringElement(MORPH_RECT, new Size(8, 8));
		erode(thresh, thresh, erodeElement);
		erode(thresh, thresh, erodeElement);
		dilate(thresh, thresh, dilateElement);
		dilate(thresh, thresh, dilateElement);
		return thresh;

	}
	
	
	public double calcAngles(IplImage coloredImage, CvSeq points) {

		double angle = 0;
		ArrayList<CvPoint> listen = new ArrayList<CvPoint>();

		for (int i = 0; i < 5; i++) {
			listen.add(new CvPoint(cvGetSeqElem(points, i)));
		}

		// find the maximum cosine of the angle between joint edges

		for (int j = 0; j < listen.size() - 1; j++) {

			angle = Math.atan2(listen.get(j + 1).y() - listen.get(j).y(), listen.get(j + 1).x() - listen.get(j).x())
					* 180.0 / CV_PI;
			System.out.println(angle);
			break;
		}

		return angle;
	}
	
	public double calculateAngle(Point A, Point B, Point C) {
		double a = Math.sqrt(Math.pow(C.x()-B.x(), 2) + Math.pow(C.y()-B.y(), 2));
		double b = Math.sqrt(Math.pow(A.x()-C.x(), 2) + Math.pow(A.y()-C.y(), 2));
		double c = Math.sqrt(Math.pow(B.x()-A.x(), 2) + Math.pow(B.y()-A.y(), 2));
		
		double cosA = (Math.pow(b, 2) + Math.pow(c, 2) - Math.pow(a, 2)) / (2 * b * c);
		return Math.acos(cosA);
	}
}