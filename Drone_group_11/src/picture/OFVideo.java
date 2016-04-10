package picture;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class OFVideo implements Runnable {

	Java2DFrameConverter converter1;
	CanvasFrame canvas;
	private boolean startValuesControl;
	BufferedImage arg0;

	OpenCVFrameConverter.ToIplImage converter;
	OpticalFlowCalculatorFUCKINGLORT OFC;
	JFrame frame1 = new JFrame("Change Thresh");

	public OFVideo(BufferedImage arg0) {
		this.arg0 = arg0;
		converter = new OpenCVFrameConverter.ToIplImage();
		converter1 = new Java2DFrameConverter();
		canvas = new CanvasFrame("OFVideo");
		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setupFrame();
		
	}

	

	public void setArg0(BufferedImage arg0) {
		this.arg0 = arg0;
	}

	@Override
	public void run() {
		try {
			IplImage newImg = null;
			while (true) {
					newImg = converter.convert(converter1.convert(arg0));
						if (OFC == null) {
							OFC = new OpticalFlowCalculatorFUCKINGLORT(newImg);
							new Thread(OFC).start();
						}

					IplImage poly = OFC.findPolygons(newImg);
//					IplImage kat = OFC.findQRFrames(newImg);
					// IplImage opt = OFC.drawAndCalc(oldImg, poly);
					canvas.showImage(converter.convert(poly));
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void setupFrame() {
		// TODO Auto-generated method stub
		//SLiders
				JSlider sliderMinBlue = new JSlider();
				sliderMinBlue.setValue(OFC.minBlue);
				sliderMinBlue.setName("minBlue");
				sliderMinBlue.setMaximum(255);
				sliderMinBlue.setMinimum(0);
				sliderMinBlue.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) {
						// TODO Auto-generated method stub
						OFC.minBlue = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JSlider sliderMaxBlue = new JSlider();
				sliderMaxBlue.setValue(OFC.maxBlue);
				sliderMaxBlue.setName("MaxBlue");
				sliderMaxBlue.setMaximum(255);
				sliderMaxBlue.setMinimum(0);
				sliderMaxBlue.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) {
						// TODO Auto-generated method stub
						OFC.maxBlue = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JSlider sliderMinRed = new JSlider();
				sliderMinRed.setValue(OFC.minRed);
				sliderMinRed.setName("minRed");
				sliderMinRed.setMaximum(255);
				sliderMinRed.setMinimum(0);
				sliderMinRed.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) {
						// TODO Auto-generated method stub
						OFC.minRed = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JSlider sliderMaxRed = new JSlider();
				sliderMaxRed.setValue(OFC.maxRed);
				sliderMaxRed.setName("maxRed");
				sliderMaxRed.setMaximum(255);
				sliderMaxRed.setMinimum(0);
				sliderMaxRed.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) {
						// TODO Auto-generated method stub
						OFC.maxRed = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JSlider sliderMinGreen = new JSlider();
				sliderMinGreen.setValue(OFC.minGreen);
				sliderMinGreen.setName("minGreen");
				sliderMinGreen.setMaximum(255);
				sliderMinGreen.setMinimum(0);
				sliderMinGreen.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) 
					{
						// TODO Auto-generated method stub
						OFC.minGreen = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JSlider sliderMaxGreen = new JSlider();
				sliderMaxGreen.setValue(OFC.maxGreen);
				sliderMaxGreen.setName("maxGreen");
				sliderMaxGreen.setMaximum(255);
				sliderMaxGreen.setMinimum(0);
				sliderMaxGreen.addChangeListener(new ChangeListener(){

					@Override
					public void stateChanged(ChangeEvent arg0) {
						// TODO Auto-generated method stub
						OFC.maxGreen = ((JSlider) arg0.getSource()).getValue();
					}
					
				});
				JButton buttonRed = new JButton();
				buttonRed.setText("RED");
				buttonRed.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						// TODO Auto-generated method stub
						OFC.maxRed = 242;
						OFC.maxGreen = 99;
						OFC.maxBlue = 255;
						OFC.minRed = 0;
						OFC.minGreen = 0;
						OFC.minBlue = 134;
					}
					
				});
				JButton buttonGreen = new JButton();
				buttonGreen.setText("GREEN");
				buttonGreen.addActionListener(new ActionListener(){

					@Override
					public void actionPerformed(ActionEvent arg0) {
						// TODO Auto-generated method stub
						OFC.maxRed = 56;
						OFC.maxGreen = 255;
						OFC.maxBlue = 40;
						OFC.minRed = 21;
						OFC.minGreen = 43;
						OFC.minBlue = 4;
					}
					
				});
				Panel p = new Panel();
//				p.add(new JTextField("minBlue"));
				p.add(sliderMinBlue);
//				p.add(new JTextField("maxBlue"));
				p.add(sliderMaxBlue);
//				p.add(new JTextField("minRed"));
				p.add(sliderMinRed);
//				p.add(new JTextField("maxRed"));
				p.add(sliderMaxRed);
//				p.add(new JTextField("minGreen"));
				p.add(sliderMinGreen);
//				p.add(new JTextField("maxGreen"));
				p.add(sliderMaxGreen);
				p.add(buttonGreen);
				p.add(buttonRed);
				frame1.add(p);
				frame1.setSize(400, 200);
				frame1.setVisible(true);
	}
}