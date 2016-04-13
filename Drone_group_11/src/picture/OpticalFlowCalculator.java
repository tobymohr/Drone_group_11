package picture;

import static org.bytedeco.javacpp.helper.opencv_core.*;


import picture.PictureController;
import static org.bytedeco.javacpp.helper.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import org.bytedeco.javacv.*;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.qrcode.QRCodeReader;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import helper.Vector;

public class OpticalFlowCalculator {

	private static final int MAX_CORNERS = 5;
	OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	Java2DFrameConverter converter1 = new Java2DFrameConverter();
	private CvMemStorage storage = CvMemStorage.create();
	static int maxRed = 242;
	static int maxGreen = 99;
	static int maxBlue = 255;
	static int minRed = 0;
	static int minGreen = 0;
	static int minBlue = 134;
	static int smoother = 11;
	private int minThresh = 30;
	private int i = 0;
	CvPoint2D32f c1 = new CvPoint2D32f(4);
	CvPoint2D32f c2 = new CvPoint2D32f(4);


	private CvScalar rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
	private CvScalar rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom;
	QRCodeReader reader = new QRCodeReader();
	LuminanceSource source;
	BinaryBitmap bitmap;
	List<CvPoint> corners = new ArrayList<CvPoint>();

	CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);

	public OpticalFlowCalculator() {
	}


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

	public IplImage warpImage(IplImage crop, CvSeq points) {
		for (int i = 0; i < 4; i++) {
			CvPoint p = new CvPoint(cvGetSeqElem(points, i));
			corners.add(p);
		}
		//
		// Initialize Table Corners as Image Coordinates

		float[] aImg = { corners.get(0).x(), corners.get(0).y(), // BR
																	// X:
																	// 3234
																	// Y:
																	// 1858
																	// TL
				corners.get(1).x(), corners.get(1).y(), // BL X: 0
														// Y: 1801
														// BL
				corners.get(2).x(), corners.get(2).y(), // TR X:
														// 2722 Y:
														// 1069 BR
				corners.get(3).x(), corners.get(3).y() // TL X: 523
														// Y: 1030
														// TR
		};

		//
		cvLine(crop, corners.get(2), corners.get(2), CvScalar.RED, 3, CV_AA, 0);

		int height = corners.get(1).y() - corners.get(0).y();
		int width = corners.get(3).x() - corners.get(0).x();
		if (height <= 0 || width <= 0 || height == width) {
			return crop;
		}
		float aspect = height / width;
		
		float[] aWorld = { 
				0.0f, 0.0f,
				0.0f, crop.height() * aspect,
				crop.width(), crop.height() * aspect,
				crop.width(),0.0f 
				};

		CvMat homography = cvCreateMat(3, 3, opencv_core.CV_32FC1);
		opencv_imgproc.cvGetPerspectiveTransform(aImg, aWorld, homography);

		IplImage imgWarped = cvCreateImage(new CvSize(crop.width(), (int) (crop.height() * aspect)), 8, 3);
		opencv_imgproc.cvWarpPerspective(crop, imgWarped, homography, opencv_imgproc.CV_INTER_LINEAR, CvScalar.ZERO);

		return imgWarped;
	}

	public IplImage extractQRImage(IplImage img0) {
		cvClearMemStorage(storage);
		float known_distance = 100;
		float known_width = 28;
		float focalLength = (152 * known_distance) / known_width;

		IplImage img1 = cvCreateImage(cvGetSize(img0), IPL_DEPTH_8U, 1);
		cvCvtColor(img0, img1, CV_RGB2GRAY);

		cvCanny(img1, img1, 100, 200);
		CvSeq contour = new CvSeq(null);
		cvFindContours(img1, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);

		CvBox2D[] markers = new CvBox2D[3];
		markers[0] = new CvBox2D();
		markers[1] = new CvBox2D();
		markers[2] = new CvBox2D();
		List<String> codes = new ArrayList<String>();

		IplImage mask2 = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img1.nChannels());
		IplImage crop2 = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());
		IplImage imgWarped = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());;
		cvSetZero(crop2);
		cvSetZero(mask2);
		boolean found = false;
		BufferedImage qrCode;

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == 4 && cvContourArea(points) > 150 && cvContourArea(points) < 75000) {


					IplImage mask = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img1.nChannels());
					IplImage crop = cvCreateImage(cvGetSize(img1), IPL_DEPTH_8U, img0.nChannels());
					cvSetZero(crop);
					cvSetZero(mask);
					cvDrawContours(mask, points, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					cvDrawContours(mask2, points, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
					cvCopy(img0, crop, mask);
					cvCopy(img0, crop2, mask2);

					markers[0] = cvMinAreaRect2(points, storage);
					
					
//					qrCode = converter1.convert(converter.convert(imgWarped));
//					source = new BufferedImageLuminanceSource(qrCode);
//					bitmap = new BinaryBitmap(new HybridBinarizer(source));
//					try {
//						Result detectionResult = reader.decode(bitmap);
//						codes.add(detectionResult.getText());
//						found = true;
//					} catch (NotFoundException e) {
//					} catch (ChecksumException e) {
//					} catch (FormatException e) {
//					}
////					canvas.showImage(converter.convert(imgWarped));

				
					imgWarped = warpImage(crop, points);
					
					return imgWarped;
				}
			}

			contour = contour.h_next();
		}
		if (found) {
			System.out.println("-------------");
			for (String e : codes) {
				System.out.println(e);
			}
			System.out.println("-------------");

		}
		return img0;
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
		
		erodeAndDilate(imgbin);

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

		cvThreshold(grayImage, grayImage, 100, 255, CV_THRESH_BINARY);

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
		// cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return grayImage;
	}

	public IplImage findContoursRed(IplImage img) {

		IplImage hueLower = null;// for red
		IplImage hueUpper = null;
		IplImage imghsv, imgbin;

		// img = balanceWhite(img);
		CvSeq contour1 = new CvSeq(), contour2;
		CvMemStorage storage = CvMemStorage.create();
		double areaMax = 1000, areaC = 0;

		imghsv = cvCreateImage(cvGetSize(img), 8, 3);
		imgbin = cvCreateImage(cvGetSize(img), 8, 1);
		hueLower = cvCreateImage(cvGetSize(img), 8, 1);
		hueUpper = cvCreateImage(cvGetSize(img), 8, 1);

		cvCvtColor(img, imghsv, CV_BGR2HSV);
		
		
		// Two ranges to get full color spectrum
		cvInRangeS(imghsv, cvScalar(0, 100, 100,0), cvScalar(10, 255, 255, 0), hueLower);
		cvInRangeS(imghsv, cvScalar(160, 100, 100, 0), cvScalar(179, 255, 255, 0), hueUpper);
		cvAddWeighted(hueLower, 1.0, hueUpper, 1.0, 0.0, imgbin);
		
		cvReleaseImage(hueLower);
		cvReleaseImage(hueUpper);
		cvReleaseImage(imghsv);

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
		// cvSmooth(imgbin, imgbin, 3, smoother, 0, 0, 0);
		return imgbin;
	}

	public IplImage findContoursGreen(IplImage img) {
		
		IplImage imghsv, imgbin;
		
		
		// Green
		CvScalar minc = cvScalar(35, 70, 7, 0), maxc = cvScalar(75, 255, 255, 0);
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
				if (points.total() == edgeNumber && cvContourArea(points) > 50 && cvContourArea(points) < 50000) {
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
	
	public IplImage erodeAndDilate(IplImage thresh)
	{
		// Removes static
		  Mat matImg = cvarrToMat(thresh);
		  Mat eroded = new Mat(MORPH_RECT,3,3);				  
		  erode(matImg, eroded, eroded);
		  
		  // Dilate image, by default 3x3 element is used
		  Mat dilated = new Mat(MORPH_RECT,8,8);
		  dilate(eroded, eroded, dilated);
		  
		  thresh = new IplImage(eroded);
		
		return thresh;
		
	}
}
