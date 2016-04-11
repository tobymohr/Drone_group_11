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
import static org.bytedeco.javacpp.opencv_core.cvCloneImage;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateSeq;
import static org.bytedeco.javacpp.opencv_core.cvCvtSeqToArray;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvInRangeS;
import static org.bytedeco.javacpp.opencv_core.cvMerge;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvSeqPush;
import static org.bytedeco.javacpp.opencv_core.cvSetImageCOI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvSplit;
import static org.bytedeco.javacpp.opencv_core.cvTermCriteria;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
import static org.bytedeco.javacpp.opencv_core.cvSetZero;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HSV2BGR;
import static org.bytedeco.javacpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_TOZERO;
import static org.bytedeco.javacpp.opencv_imgproc.cvApproxPoly;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvCheckContourConvexity;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourPerimeter;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDilate;
import static org.bytedeco.javacpp.opencv_imgproc.cvEqualizeHist;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_imgproc.cvFindCornerSubPix;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetCentralMoment;
import static org.bytedeco.javacpp.opencv_imgproc.cvGetSpatialMoment;
import static org.bytedeco.javacpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;
import static org.bytedeco.javacpp.opencv_imgproc.cvMinAreaRect2;
import static org.bytedeco.javacpp.opencv_imgproc.cvMoments;
import static org.bytedeco.javacpp.opencv_imgproc.cvPolyLine;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrDown;
import static org.bytedeco.javacpp.opencv_imgproc.cvPyrUp;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;
import static org.bytedeco.javacpp.opencv_video.cvCalcOpticalFlowPyrLK;

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
import org.bytedeco.javacpp.opencv_core.CvSlice;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvMoments;
import org.bytedeco.javacpp.helper.opencv_core.CvArr;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.qrcode.QRCodeReader;

import helper.Vector;

public class OpticalFlowCalculatorFUCKINGLORT implements Runnable {

	private static final int MAX_CORNERS = 5;
	private static final int MAX_Y_COORDINATE = 6; //TODO FIGURE OUT THE REAL MAX Y COORDINATE
	private static final int MAX_X_COORDINATE = 5; //TODO FIGURE OUT THE REAL MAX x COORDINATE

	OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	Java2DFrameConverter converter1 = new Java2DFrameConverter();
	private CvMemStorage storage = CvMemStorage.create();
	static int maxRed = 242;
	static int maxGreen = 99;
	static int maxBlue = 255;
	static int minRed = 0;
	static int minGreen = 0;
	static int minBlue = 134;
	static int smoother = 9;
	private int minThresh = 9;
	
	private int maxThresh = 5;
	private CvScalar rgba_min = cvScalar(minRed, minGreen, minBlue, 0);
	private CvScalar rgba_max = cvScalar(maxRed, maxGreen, maxBlue, 0);
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom;
	QRCodeReader reader = new QRCodeReader();
	LuminanceSource source;
	BinaryBitmap bitmap;

	CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);

	public OpticalFlowCalculatorFUCKINGLORT(IplImage frame) {
	}
	
	public int getMinThresh() {
		return minThresh;
	}

	public void setMinThresh(int minThresh) {
		this.minThresh = minThresh;
	}

	public int getMaxThresh() {
		return maxThresh;
	}

	public void setMaxThresh(int maxThresh) {
		this.maxThresh = maxThresh;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
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
	
	

	public synchronized IplImage findPolygons(IplImage image) {
		cvClearMemStorage(storage);
		image = balanceWhite(image);
		IplImage grayImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, 1);
		cvCvtColor(image, grayImage, CV_BGR2GRAY);

		/**
		 * Can use 
		 * cvCanny(image, grayImage, 75, 90, 3);
		 * instead of 
		 * cvThreshold(getThresholdImage(image), grayImage, 70, 70, CV_THRESH_TOZERO);
		 */
		
//		cvCanny(image, grayImage, 75, 90, 3);
		cvThreshold(getThresholdImage(image), grayImage, 70, 70, CV_THRESH_TOZERO);
		CvSeq contour = new CvSeq(null);

		cvFindContours(grayImage, storage, contour, Loader.sizeof(CvContour.class), CV_RETR_LIST,
				CV_CHAIN_APPROX_SIMPLE);
		
		
		// scale of center box
		int factor = 4;

		// find center points
		xleft = (int) image.width() / factor;
		xright = (int) (image.width() / factor) * (factor - 1);
		ytop = 0;
		ybot = image.height();
        //center of centerpoints y
		yCenterBottom = (image.height() / 3) *2;
		yCenterTop = (image.height() / 3) ;
		
		// Make center points
		CvPoint pointTopLeft = cvPoint(xleft, ytop);
		CvPoint pointBottomLeft = cvPoint(xleft, ybot);
		CvPoint pointTopRight = cvPoint(xright, ytop);
		CvPoint pointRightBottom = cvPoint(xright, ybot);

		// Make upper line points in center
		CvPoint pointCenterUpperLeft = cvPoint(xleft, yCenterTop);
		CvPoint pointCenterUpperRight = cvPoint(xright, yCenterTop);

		// Make bottom line points in center
		CvPoint pointCenterBottomLeft = cvPoint(xleft, yCenterBottom);
		CvPoint pointCenterBottomRight = cvPoint(xright, yCenterBottom);

		// Find red point
		int posX = 0;
		int posY = 0;
		IplImage detectThrs = getThresholdImage(image);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		posX = (int) (mom10 / area);
		posY = (int) (mom01 / area);

		// Draw Center
		cvLine(image, pointTopLeft, pointTopRight, CV_RGB(255, 0, 255), 2, CV_AA, 0);
		cvLine(image, pointTopRight, pointRightBottom, CV_RGB(255, 0, 255), 2, CV_AA, 0);
		cvLine(image, pointRightBottom, pointBottomLeft, CV_RGB(255, 0, 255), 2, CV_AA, 0);
		cvLine(image, pointBottomLeft, pointTopLeft, CV_RGB(255, 0, 255), 2, CV_AA, 0);

		// Draw upperline in center
		cvLine(image, pointCenterUpperLeft, pointCenterUpperRight, CV_RGB(0, 0, 255), 2, CV_AA, 0);
		cvLine(image, pointCenterBottomLeft, pointCenterBottomRight, CV_RGB(0, 255, 0), 2, CV_AA, 0);

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() < 6 && cvContourArea(points) > 50) {
					// drawLines of Box
					cvDrawContours(image, points, CvScalar.WHITE, CvScalar.WHITE, -2, 2, CV_AA);
					// Counter for checking points in center box
					int counter = 0;
					CvPoint v = null;

					for (int i = 0; i < points.total(); i++) {
						v = new CvPoint(cvGetSeqElem(points, i));
						// draw corners of box found
						cvLine(image, v, v, CV_RGB(255, 0, 0), 3, CV_AA, 0);
						// Count if box corners are in purple center box
						if (checkBoxForCenter(v.x(), v.y())) {
							counter++;

						}

					}
					// if counter is the same as points in array points then all
					// points must be centered
					CvPoint p0 = cvPoint(posX, posY);
					cvLine(image, p0, p0, CV_RGB(0, 0, 0), 16, CV_AA, 0);
					if (counter == points.total()) {
						//check in which part of center box is. 
						switch(checkPositionInCenter(v.x(), v.y())){
						case 1:
							cvLine(image, p0, p0,  CvScalar.BLUE, 16, CV_AA, 0);
							break;
						case 2:
							cvLine(image, p0, p0,  CvScalar.RED, 16, CV_AA, 0);
							break;
						case 3:
							cvLine(image, p0, p0,  CvScalar.GREEN, 16, CV_AA, 0);
							break;
						default: 
							break;
							
						}
					} 
				}
			}
			contour = contour.h_next();
		}
		return image;
	}
	
	public synchronized IplImage oldfindQRFrames(IplImage image) {
		float known_distance = 100;
		float known_width = 27;
		
		
		float focalLength = (167 * known_distance) / known_width;
		cvClearMemStorage(storage);
//		image = balanceWhite(image);
		IplImage grayImage = cvCloneImage(image);
//		cvCopy(image, grayImage);
//		cvCanny(getThresholdBlackImage(grayImage),grayImage, getMinThresh(), getMaxThresh());
		cvThreshold(grayImage, grayImage, getMinThresh(), getMaxThresh(), CV_THRESH_TOZERO);
//		System.out.println("Min " + getMinThresh() + "max " + getMaxThresh());
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
		CvBox2D marker = new CvBox2D();
		while (contour != null && !contour.isNull()) {

			// Draw red point
			cvLine(image, pointTopLeft, pointTopRight, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointTopRight, pointRightBottom, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointRightBottom, pointBottomLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);
			cvLine(image, pointBottomLeft, pointTopLeft, CV_RGB(255, 0, 255), 3, CV_AA, 0);

			if (contour.elem_size() > 0) {
				CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
						cvContourPerimeter(contour) * 0.02, 0);
				if (points.total() == 4 && cvContourArea(points) > 100) {
					for (int i = 0; i < points.total(); i++) {
						CvPoint v = new CvPoint(cvGetSeqElem(points, i));
						if (checkForCenter(v.x(), v.y(), posX, posY)) {
							CvPoint p0 = cvPoint(posX, posY);
							// Draw red point
							cvLine(image, p0, p0, CV_RGB(255, 0, 0), 3, CV_AA, 0);
							cvDrawContours(image, points, CvScalar.RED, CvScalar.RED, -2, 2, CV_AA);
							if (marker.get(2) < cvMinAreaRect2(contour, storage).get(2)) {
								
								marker = cvMinAreaRect2(contour, storage);
							}
						}
					}
				}
			}

			contour = contour.h_next();
		}
		
//		System.out.println((known_width * focalLength) / marker.get(2));
		return image;

	}
	
	public synchronized IplImage findQRFrames(IplImage image) {
		float known_distance = 100;
		float known_width = 27;
		float focalLength = (167 * known_distance) / known_width;
		
		cvClearMemStorage(storage);
//		image = balanceWhite(image);
		IplImage grayImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCvtColor(image, grayImage, CV_BGR2GRAY);
		IplImage orgImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, image.nChannels());
		cvCopy(image, orgImage);
		grayImage = getThresholdBlackImage(grayImage);
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
				if (points.total() == 4 && cvContourArea(points) > 100) {
					for (int i = 0; i < points.total(); i++) {
////						cvLine(image, p0, p0, CV_RGB(255, 0, 0), 3, CV_AA, 0);
						CvPoint v = new CvPoint(cvGetSeqElem(points, i));
						cvDrawContours(image, points, CvScalar.RED, CvScalar.RED, -2, 2, CV_AA);
						CvPoint p0 = cvPoint(posX, posY);
						// Draw red point
					
						markers[codeIndex] = cvMinAreaRect2(contour, storage);
						IplImage img1 = IplImage.create(orgImage.width(), orgImage.height(), orgImage.depth(), 1);
						cvCvtColor(orgImage, img1, CV_RGB2GRAY);
						cvCanny(img1, img1, 100, 200);
						
						IplImage mask = IplImage.create(orgImage.width(), orgImage.height(), IPL_DEPTH_8U, orgImage.nChannels());
						cvDrawContours(mask, contour, CvScalar.WHITE, CV_RGB(248, 18, 18), 1, -1, 8);
						
						cvCopy(orgImage, crop, mask);
//						BufferedImage qrCode = converter1.convert(converter.convert(crop));
//						source = new BufferedImageLuminanceSource(qrCode);
//		 				bitmap = new BinaryBitmap(new HybridBinarizer(source));
//		 				try {
//		 					Result detectionResult = reader.decode(bitmap);
////		 					codes[codeIndex] = detectionResult.getText();
//							codeIndex++;
//		 				} catch (NotFoundException e) 
//		 				{
//		 				} catch (ChecksumException e) {
//		 					// TODO Auto-generated catch block
//		 					e.printStackTrace();
//		 				} catch (FormatException e) {
//		 					// TODO Auto-generated catch block
//		 					e.printStackTrace();
//		 				}								
					}
				}
			}

			contour = contour.h_next();
		}
		return crop;
	}
	
	public IplImage drawSquares(IplImage img, CvSeq squares) {

        //      Java translation: Here the code is somewhat different from the C version.
        //      I was unable to get straight forward CvPoint[] arrays
        //      working with "reader" and the "CV_READ_SEQ_ELEM".

//        CvSeqReader reader = new CvSeqReader();
		cvClearMemStorage(storage);
        IplImage cpy = cvCloneImage(img);
        int i = 0;

        // Used by attempt 3
        // Create a "super"-slice, consisting of the entire sequence of squares
        CvSlice slice = new CvSlice(squares);

        // initialize reader of the sequence
//        cvStartReadSeq(squares, reader, 0);

         // read 4 sequence elements at a time (all vertices of a square)
         for(i = 0; i < squares.total(); i += 4) {

             CvPoint rect = new CvPoint(4);
             IntPointer count = new IntPointer(1).put(4);
             // get the 4 corner slice from the "super"-slice
             cvCvtSeqToArray(squares, rect, slice.start_index(i).end_index(i + 4));

             cvPolyLine(cpy, rect.position(0), count, 1, 1, CV_RGB(0,255,0), 3, CV_AA, 0);
         }

//        canvas.showImage(converter.convert(cpy));
        return cpy;
    }
	
	public IplImage HSVTOBGR(IplImage img){
		IplImage bgrImg = IplImage.create(img.width(), img.height(), img.depth(), img.nChannels());
		cvCvtColor(img,bgrImg ,CV_HSV2BGR);
		return bgrImg;
	}
	
	public CvSeq findSquares(IplImage img) {
        // Java translation: moved into loop
        // CvSeq contours = new CvSeq();
//		cvClearMemStorage(storage);
		int thresh = 50;
        int i, c, l, N = 11;
        CvSize sz = cvSize(img.width() & -2, img.height() & -2);
        IplImage timg = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, img.nChannels()); 
        cvCopy(img,timg);// make a copy of input image
        IplImage gray = cvCreateImage(sz, 8, 1);
        IplImage pyr = cvCreateImage(cvSize(sz.width()/2, sz.height()/2), 8, img.nChannels());
        IplImage tgray = null;
        // Java translation: moved into loop
        // CvSeq result = null;
        // double s = 0.0, t = 0.0;

        // create empty sequence that will contain points -
        // 4 points per square (the square's vertices)
        CvSeq squares = cvCreateSeq(0, Loader.sizeof(CvSeq.class), Loader.sizeof(CvPoint.class), storage);

        // select the maximum ROI in the image
        // with the width and height divisible by 2
        cvSetImageROI(timg, cvRect(0, 0, sz.width(), sz.height()));

        // down-scale and upscale the image to filter out the noise
        cvPyrDown(timg, pyr, 7);
        cvPyrUp(pyr, timg, 7);
        tgray = cvCreateImage(sz, 8, 1);

        // find squares in every color plane of the image
        for (c = 0; c < 3; c++) {
            // extract the c-th color plane
            cvSetImageCOI(timg, c+1);
            cvCopy(timg, tgray);

            // try several threshold levels
            for (l = 0; l < N; l++) {
                // hack: use Canny instead of zero threshold level.
                // Canny helps to catch squares with gradient shading
                if (l == 0) {
                    // apply Canny. Take the upper threshold from slider
                    // and set the lower to 0 (which forces edges merging)
                    cvCanny(tgray, gray, 0, thresh, 5);
                    // dilate canny output to remove potential
                    // holes between edge segments
                    cvDilate(gray, gray, null, 1);
                } else {
                    // apply threshold if l!=0:
                    //     tgray(x,y) = gray(x,y) < (l+1)*255/N ? 255 : 0
                    cvThreshold(tgray, gray, (l+1)*255/N, 255, CV_THRESH_BINARY);
                }

                // find contours and store them all as a list
                // Java translation: moved into the loop
                CvSeq contours = new CvSeq();
                cvFindContours(gray, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE, cvPoint(0,0));

                // test each contour
                while (contours != null && !contours.isNull()) {
                    // approximate contour with accuracy proportional
                    // to the contour perimeter
                    // Java translation: moved into the loop
                    CvSeq result = cvApproxPoly(contours, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours)*0.02, 0);
                    // square contours should have 4 vertices after approximation
                    // relatively large area (to filter out noisy contours)
                    // and be convex.
                    // Note: absolute value of an area is used because
                    // area may be positive or negative - in accordance with the
                    // contour orientation
                    if(result.total() == 4 && Math.abs(cvContourArea(result, CV_WHOLE_SEQ, 0)) > 1000 && cvCheckContourConvexity(result) != 0) {

                        // Java translation: moved into loop
                        double s = 0.0, t = 0.0;

                        for( i = 0; i < 5; i++ ) {
                            // find minimum angle between joint
                            // edges (maximum of cosine)
                            if( i >= 2 ) {
                                //      Java translation:
                                //          Comment from the HoughLines.java sample code:
                                //          "    Based on JavaCPP, the equivalent of the C code:
                                //                  CvPoint* line = (CvPoint*)cvGetSeqElem(lines,i);
                                //                  CvPoint first=line[0];
                                //                  CvPoint second=line[1];
                                //          is:
                                //                  Pointer line = cvGetSeqElem(lines, i);
                                //                  CvPoint first = new CvPoint(line).position(0);
                                //                  CvPoint second = new CvPoint(line).position(1);
                                //          "
                                //          ... so after some trial and error this seem to work
//                                t = fabs(angle(
//                                        (CvPoint*)cvGetSeqElem( result, i ),
//                                        (CvPoint*)cvGetSeqElem( result, i-2 ),
//                                        (CvPoint*)cvGetSeqElem( result, i-1 )));
                                t = Math.abs(angle(new CvPoint(cvGetSeqElem(result, i)),
                                        new CvPoint(cvGetSeqElem(result, i-2)),
                                        new CvPoint(cvGetSeqElem(result, i-1))));
                                s = s > t ? s : t;
                            }
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence
                        if (s < 0.3)
                            for( i = 0; i < 4; i++ ) {
                                cvSeqPush(squares, cvGetSeqElem(result, i));
                            }
                    }

                    // take the next contour
                    contours = contours.h_next();
                }
            }
        }

     

        return squares;
    }
	
	public void kaare(){
		String[] codes = new String[3];
		boolean found = true;
		double[] distances = new double[3];
		double[] xCoordinates = new double[3];
		double[] yCoordinates = new double[3];
		codes[0] = "W02.02";
		codes[1] = "W02.01";
		codes[2] = "W02.00";
		for (int i = 0; i < 3; i++) {
			if (codes[i] == null) {
				found = false;
			} else {
				double[] parse = parseCoordinate(codes[i]);
				xCoordinates[i] = parse[0];
				yCoordinates[i] = parse[1];
			}
		}
		if (found) {
			double[] distancesTest = new double[3];
			distances[0] = 3;
			distances[1] = 2;
			distances[2] = 3;
			double distanceBetweenCoordinates = 150;
//			for (int i = 0; i < 3; i++) {
//				distances[i] = (known_width * focalLength) / markers[i].get(2);
//			}
			double angleA = Math.atan(distanceBetweenCoordinates/distances[0]);
			double angleB = Math.atan(distanceBetweenCoordinates/distances[1]);

			double radiusOne = 0.5 * (distanceBetweenCoordinates / Math.sin(angleA));
			double centerOneX = 0.5 * ((yCoordinates[1] - yCoordinates[0]) / 
					(Math.sqrt(Math.pow(Math.abs(-1 * yCoordinates[1] + yCoordinates[0]), 2)) + Math.sqrt(Math.pow(Math.abs(-1 * xCoordinates[1] + xCoordinates[0]), 2)))) *
					Math.sqrt((Math.pow(distanceBetweenCoordinates, 2)/Math.pow(Math.sin(angleA), 2)) - Math.pow(distanceBetweenCoordinates, 2)) +
					0.5 * xCoordinates[0] + 0.5 * xCoordinates[1];
			double centerOneY = 0.5 * ((xCoordinates[1] - xCoordinates[0]) / 
					(Math.sqrt(Math.pow(Math.abs(-1 * yCoordinates[1] + yCoordinates[0]), 2)) + Math.sqrt(Math.pow(Math.abs(-1 * xCoordinates[1] + xCoordinates[0]), 2)))) *
					Math.sqrt((Math.pow(distanceBetweenCoordinates, 2)/Math.pow(Math.sin(angleA), 2)) - Math.pow(distanceBetweenCoordinates, 2)) +
					0.5 * yCoordinates[0] + 0.5 * yCoordinates[1];
			
			double radiusTwo = 0.5 * (distanceBetweenCoordinates / Math.sin(angleB));
			double centerTwoX = 0.5 * ((yCoordinates[2] - yCoordinates[1]) / 
					(Math.sqrt(Math.pow(Math.abs(-1 * yCoordinates[2] + yCoordinates[1]), 2)) + Math.sqrt(Math.pow(Math.abs(-1 * xCoordinates[2] + xCoordinates[1]), 2)))) *
					Math.sqrt((Math.pow(distanceBetweenCoordinates, 2)/Math.pow(Math.sin(angleB), 2)) - Math.pow(distanceBetweenCoordinates, 2)) +
					0.5 * xCoordinates[1] + 0.5 * xCoordinates[2];
			double centerTwoY = 0.5 * ((xCoordinates[2] - xCoordinates[1]) / 
					(Math.sqrt(Math.pow(Math.abs(-1 * yCoordinates[2] + yCoordinates[1]), 2)) + Math.sqrt(Math.pow(Math.abs(-1 * xCoordinates[2] + xCoordinates[1]), 2)))) *
					Math.sqrt((Math.pow(distanceBetweenCoordinates, 2)/Math.pow(Math.sin(angleB), 2)) - Math.pow(distanceBetweenCoordinates, 2)) +
					0.5 * yCoordinates[1] + 0.5 * yCoordinates[2];
			
			double d, a, h, x2, y2, xu, yu, p2X, p2Y;
			d = Math.sqrt(Math.pow((centerOneX - centerTwoX), 2) + Math.pow((centerOneY - centerTwoY), 2));
			a = (Math.pow(radiusOne, 2) - Math.pow(radiusTwo, 2) + Math.pow(d, 2)) / 2 * d;
			h = Math.sqrt(Math.pow(radiusOne, 2) - Math.pow(a, 2));
			p2X = centerOneX + a * (centerOneX - centerTwoX) / d;
			p2Y = centerOneY + a * (centerOneY - centerTwoY) / d;
			x2 = p2X + h * (centerTwoY - centerOneY) / d;
			y2 = p2Y - h * (centerTwoX - centerOneX) / d;
			xu = p2X - h * (centerTwoY - centerOneY) / d;
			yu = p2Y + h * (centerTwoX - centerOneX) / d;

			System.out.println("----------");
			System.out.println(x2);
			System.out.println(y2);
			System.out.println(xu);
			System.out.println(yu);
			System.out.println("----------");
			System.out.println();

			
//			System.out.println("----------");
//			for (int i = 0; i < 3; i++) {
//				System.out.println(codes[i]);
//			}
//			System.out.println("----------");
		}
	}
	
	private double[] parseCoordinate(String text) {
		double[] result = new double[2];
		
		text = text.substring(1);
		if (text.startsWith("00")) {
			result[0] = Double.parseDouble(text.substring(4));
			result[1] = MAX_Y_COORDINATE;			
		} else if (text.startsWith("01")) {
			result[0] = MAX_X_COORDINATE;
			result[1] = Double.parseDouble(text.substring(4));			
		} else if (text.startsWith("02")) {
			result[0] = Double.parseDouble(text.substring(4));
			result[1] = 0;
		} else if (text.startsWith("03")) {
			result[0] = 0;
			result[1] = Double.parseDouble(text.substring(4));
		} else {
			return null;
		}
		
		return result;
	}

	private int checkPositionInCenter(int posx, int posy) {
		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		if (upperCenterCondition) {
			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition ) {
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

		System.out.println("RGBMIN R " + rgba_min.red() + "G " + rgba_min.green() + " B " + rgba_min.blue()
				+ "Smoothing: " + smoother);
		System.out.println("RGBMAX R " + rgba_max.red() + "G " + rgba_max.green() + " B " + rgba_max.blue()
				+ "Smoothing: " + smoother);
		cvInRangeS(orgImg, rgba_min, rgba_max, imgThreshold);// red
		cvSmooth(imgThreshold, imgThreshold, 2, smoother, 0, 0, 0);
		// cvSaveImage(++ii + "dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}
	
	public IplImage getThresholdBlackImage(IplImage orgImg) {
		IplImage imgHSV = IplImage.create(orgImg.width(), orgImg.height(), IPL_DEPTH_8U, 3);
		cvCvtColor(orgImg, imgHSV, CV_BGR2HSV);
		IplImage imgThreshold = IplImage.create(orgImg.width(), orgImg.height(), IPL_DEPTH_8U,1);

		// for black
		cvInRangeS(imgHSV, cvScalar(0, 0, 0, 0), cvScalar(190, 255, 30, 0), imgThreshold);
		cvSmooth(imgThreshold, imgThreshold, 1, smoother, 0, 0, 0);
		
		return imgThreshold;
	}
	private IplImage balanceWhite(IplImage cvtImg){
//		IplImage cvtImg = IplImage.create(newImg.width(), newImg.height(), newImg.depth(), newImg.nChannels());
//		cvCvtColor(newImg, cvtImg,CV_BGR2HSV);
		
		IplImage channel1 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel2 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		IplImage channel3 = IplImage.create(cvtImg.width(), cvtImg.height(), cvtImg.depth(), 1);
		cvSplit(cvtImg, channel1, channel2, channel3, null);
		cvEqualizeHist(channel1, channel1);
		cvEqualizeHist(channel2, channel2);
		cvEqualizeHist(channel3, channel3);
		cvMerge(channel1, channel2, channel3, null, cvtImg);
		
//		cvCvtColor(cvtImg, newImg, CV_HSV2BGR);
		
		
		return cvtImg;
	}
}
