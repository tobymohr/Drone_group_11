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
import com.sun.javafx.geom.Vec4d;

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
import helper.Move;
import helper.Vector;

public class PictureProcessingHelper {

	private OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	private float distance;
	private Java2DFrameConverter converter1 = new Java2DFrameConverter();
	private CvMemStorage storage = CvMemStorage.create();
	public String code = "";
	private int xleft, xright, ytop, ybot, yCenterTop, yCenterBottom, dist;
	private QRCodeReader reader = new QRCodeReader();
	private LuminanceSource source;
	private BinaryBitmap bitmap;
	private Point2f vertices;

	public PictureProcessingHelper() {
	}

	double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
		double dx1 = pt1.x() - pt0.x();
		double dy1 = pt1.y() - pt0.y();
		double dx2 = pt2.x() - pt0.x();
		double dy2 = pt2.y() - pt0.y();

		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	public Mat findContoursBlackMat(Mat img) {
		MatVector contour1 = new MatVector();
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
		Mat mathsv3 = new Mat(img.arraySize(), CV_8U, 3);
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
		// System.out.println("Width in pix " + z);
		double sum = (w * y) / (2 * x * z);
		double AOV = Math.atan(sum) * 2;
		AOV = Math.toDegrees(AOV);
		// System.out.println("AOV " + AOV);

		Point tl = null;
		Point tr = null;
		Point br = null;
		Point bl = null;
		float height;
		float width;
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
			height = rect.size().width();
			width = rect.size().height();
		}

		float[] sourcePoints = { tl.x(), tl.y(), tr.x(), tr.y(), bl.x(), bl.y(), br.x(), br.y() };
		float[] destinationPoints = { 0.0f, 0.0f, width, 0.0f, 0.0f, height, width, height };
		CvMat homography = cvCreateMat(3, 3, CV_32FC1);
		cvGetPerspectiveTransform(sourcePoints, destinationPoints, homography);
		Mat copy = crop.clone();
		IplImage dest = new IplImage(copy);
		cvWarpPerspective(dest, dest, homography, CV_INTER_LINEAR, CvScalar.ZERO);
		dest = cropImage(dest, 0, 0, (int) width, (int) height);
		Mat m = cvarrToMat(dest.clone());
		cvRelease(dest);
		cvClearMemStorage(storage);

		return m;

	}

	private IplImage cropImage(IplImage dest, int fromX, int fromY, int toWidth, int toHeight) {
		cvSetImageROI(dest, cvRect(fromX, fromY, toWidth, toHeight));
		IplImage dest2 = cvCloneImage(dest);
		cvCopy(dest, dest2);

		return dest2;
	}

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
				if (scanQrCode(crop)) {
					putText(img0, code, new Point((int) rect.center().x() - 25, (int) rect.center().y() + 80), 1, 2,
							Scalar.GREEN, 2, 8, false);
				}
				distance = calcDistance(rect);
				List<Long> points = calcPosition(3, 3.3, 3);
				printMoves(calcMoves(points.get(0), points.get(1)));

				drawContours(img0, matContour, i, Scalar.WHITE, 2, 8, null, 1, null);
				putText(img0, "" + (int) distance,
						new Point((int) rect.center().x() - 25, (int) rect.center().y() + 60), 1, 2, Scalar.BLUE, 2, 8,
						false);
				if (center(rect)) {
					putText(img0, "CENTER", new Point((int) rect.center().x() - 25, (int) rect.center().y() + 20), 1, 2,
							Scalar.RED, 2, 8, false);
				}
				crop = new Mat(img0.rows(), img0.cols(), CV_8UC3, Scalar.BLACK);
				mask = new Mat(img1.rows(), img1.cols(), CV_8UC1, Scalar.BLACK);
			}
		}
		return img0;
	}

	public List<Move> calcMoves(long x, long y) {
		List<Move> moves = new ArrayList<>();
		int minY = 0;
		int maxX = 5;
		int maxY = 5;
		// Calc moves in x-axis
		for (int i = 0; i < 5; i++) {
			if (x < maxX) {
				x++;
				moves.add(new Move(Move.MOVE_RIGHT));
			}
		}
		// Calc moves in y-axis
		for (int i = 0; i < 5; i++) {
			if (y <= maxY && y > minY) {
				y--;
				moves.add(new Move(Move.MOVE_DOWN));
			}
		}
		return moves;
	}

	public void printMoves(List<Move> moves) {
		for (Move move : moves) {
			if (move.getMove() == Move.MOVE_RIGHT) {
				System.out.println("MOVE RIGHT");
			}
			if (move.getMove() == Move.MOVE_DOWN) {
				System.out.println("MOVE DOWN");
			}
			if (move.getMove() == Move.MOVE_LEFT) {
				System.out.println("MOVE LEFT");
			}
			if (move.getMove() == Move.MOVE_FORWARD) {
				System.out.println("MOVE FORWARD");
			}
		}
	}

	public List<Long> calcPosition(double distanceOne, double distanceTwo, double distanceThree) {
		List<Long> calcedPoints = new ArrayList<>();
		double distanceBetweenPointsOne = 1.5;
		double distanceBetweenPointsTwo = 1.5;
		double angleA = CustomPoint.calculateAngle(distanceOne, distanceBetweenPointsOne);
		double angleB = CustomPoint.calculateAngle(distanceThree, distanceBetweenPointsTwo);
		CustomPoint P1 = new CustomPoint(6, 0);
		CustomPoint P2 = new CustomPoint(5, 0);
		CustomPoint P3 = new CustomPoint(4, 0);
		Circle C1 = new Circle(Circle.calculateCenter(P1, P2, distanceBetweenPointsOne, angleA),
				Circle.calculateRadius(distanceBetweenPointsOne, angleA));
		// System.out.println(C1.getCenter().getX() + "|" +
		// C1.getCenter().getY() + "|" + C1.getRadius());
		Circle C2 = new Circle(Circle.calculateCenter(P2, P3, distanceBetweenPointsTwo, angleB),
				Circle.calculateRadius(distanceBetweenPointsTwo, angleB));
		// System.out.println(C2.getCenter().getX() + "|" +
		// C2.getCenter().getY() + "|" + C2.getRadius());
		CustomPoint[] points = Circle.intersection(C1, C2);
		for (CustomPoint p : points) {
			System.out.println(Math.round(p.getX()) + "|" + Math.round(p.getY()));
			calcedPoints.add(Math.round(p.getX()));
			calcedPoints.add(Math.round(p.getY()));
		}

		return calcedPoints;
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
		// System.out.println("----------");
		// System.out.println(Math.toDegrees(calculateAngle(tl, tr, bl)));
		// System.out.println(Math.toDegrees(calculateAngle(tr, tl, br)));
		// System.out.println(Math.toDegrees(calculateAngle(bl, tl, br)));
		// System.out.println(Math.toDegrees(calculateAngle(br, bl, tr)));
		// System.out.println("----------");
	}

	public boolean scanQrCode(Mat srcImage) {
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

	public float calcDistance(RotatedRect rect) {
		float knownDistance = 214;
		float height = 0;
		float focalLength = 103 * knownDistance;
		int angle = Math.abs((int) rect.angle());
		if (angle >= 0 && angle < 10) {
			height = rect.size().height();
		} else {
			height = rect.size().width();
		}
		return focalLength / height;
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
		
//		// center points 
//		int xcenter =  img.arrayWidth()/2;
//		int ycenter = img.arrayHeight()/2;
		
		

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

		int counter = 0;
		for (int i = 0; i < matContour.size(); i++) {

			approxPolyDP(matContour.get(i), matContour.get(i), 0.02 * arcLength(matContour.get(i), true), true);
			if (matContour.get(i).total() == 4 && contourArea(matContour.get(i)) > 1000
					&& contourArea(matContour.get(i)) < 10000) {
				Point2f centerPoint = minAreaRect(matContour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(img, p, p, Scalar.BLACK, 16, CV_AA, 0);
				drawContours(img1, matContour, i, Scalar.WHITE, CV_FILLED, 8, null, 1, null);
				for (int j = 0; j < matContour.get(i).total(); j++) {
					Point2f centerPointTemp = minAreaRect(matContour.get(i)).center();
					opencv_core.Point ptemp = new opencv_core.Point((int) centerPointTemp.x(),
							(int) centerPointTemp.y());
					line(img, ptemp, ptemp, Scalar.BLACK, 16, CV_AA, 0);
					if (checkBoxForCenter(ptemp.x(), ptemp.y())) {
						counter++;
					}
				}

				if (counter == matContour.get(i).total()) {
					// check in which part of center box is.

					switch (checkPositionInCenter(p.x(), p.y())) {
					case 1:
						line(img, p, p, Scalar.BLUE, 16, CV_AA, 0);
						break;
					case 2:
						line(img, p, p, Scalar.RED, 16, CV_AA, 0);
						dist = 0;
						System.out.println("Center" + dist);
						break;
					case 3:	
						line(img, p, p, Scalar.GREEN, 16, CV_AA, 0);
						System.out.println("move up" + dist);
						break;
					case 4: 
						line(img,p,p,Scalar.BLACK,16,CV_AA,0);
						System.out.println("move right" + dist);
					case 5: 
						line(img,p,p,Scalar.BLACK,16,CV_AA,0);
						System.out.println("Move left" + dist);
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
	
	public Mat circle(Mat img){
		
		MatVector matCircles = new MatVector();
		
		Mat img1 = new Mat(img.arraySize(), CV_8UC1, 1);
		
		cvtColor(img, img1, CV_RGB2GRAY);
		
		GaussianBlur(img1,img, new Size(9,9), 2.0);
		
		HoughCircles(img, img,HOUGH_GRADIENT, 1, 100, 100, 100, 15, 500);
		
		
		
		return img;
	}

	public IplImage convertMatToIplImage(Mat mat) {
		return converter.convert(converter.convert(mat));
	}

	public synchronized Mat findPolygonsMat(Mat coloredImage, Mat filteredImage, int edgeNumber) {

		MatVector contour = new MatVector();
		findContours(filteredImage, contour, RETR_LIST, CV_LINK_RUNS, new opencv_core.Point());

		for (int i = 0; i < contour.size(); i++) {
			approxPolyDP(contour.get(i), contour.get(i), 0.02 * arcLength(contour.get(i), true), true);
			if (contour.get(i).total() == 4 && contourArea(contour.get(i)) > 150) {
				Point2f centerPoint = minAreaRect(contour.get(i)).center();
				opencv_core.Point p = new opencv_core.Point((int) centerPoint.x(), (int) centerPoint.y());
				line(coloredImage, p, p, new Scalar(255, 0, 0, 0), 16, CV_AA, 0);
				drawContours(coloredImage, contour, i, new Scalar(0, 0, 0, 0), 3, CV_AA, null, 1,
						new opencv_core.Point());
			}
		}

		return coloredImage;
	}

	public String getQrCode() {
		return code;
	}

	public double getDistance() {
		return distance;
	}

	private int checkPositionInCenter(int posx, int posy) {

		boolean bottomCenterCondition = posy > yCenterBottom;
		boolean upperCenterCondition = posy < yCenterTop;
		boolean leftCenterCondition = posx < xleft;
		boolean rightCenterCondition = posx > xright;

		if (upperCenterCondition) {
			
			return 1;
		}

		if (!bottomCenterCondition && !upperCenterCondition) {
			
			return 2;
		}

		if (bottomCenterCondition) {
			
			return 3;
		}
		if (leftCenterCondition) {
	
			return 4;
		}

		if (rightCenterCondition) {
			return 5;
		}
		return 0;

	}


	private boolean checkBoxForCenter(int posx, int posy) {
		

		boolean verticalCondition = posy > ytop && posy < ybot;
		boolean horizontalCondition = posx > xleft && posx < xright;
		if (horizontalCondition && verticalCondition) {
			
			return true;
		} else {
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

		/**
		 * THIS DOES NOT WORK, FIX OR DELETE IF NEEDED
		 */

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

	public boolean center(RotatedRect rect) {
		float ratio = rect.size().height() / rect.size().width();
		return ratio > 1.40 && ratio < 1.45;
	}
}