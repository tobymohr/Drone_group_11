package app;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.command.CommandManager;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.manager.AbstractManager;
import de.yadrone.base.navdata.Altitude;
import de.yadrone.base.navdata.AltitudeListener;
import de.yadrone.base.navdata.DroneState;
import de.yadrone.base.navdata.MagnetoData;
import de.yadrone.base.navdata.MagnetoListener;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.utils.ARDroneUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;


public class NavDataTracker {

	private ArrayList<Float> derp = new ArrayList<>();
	int i = 0;
	public void initCompass(IARDrone drone, Label lbl)
	{
		NavDataManager manager = drone.getNavDataManager();
		manager.addAltitudeListener(new AltitudeListener() {
			
			@Override
			public void receivedExtendedAltitude(Altitude arg0) {
//				System.out.println(arg0);
				
				
			}
			
			@Override
			public void receivedAltitude(int arg0) {
				System.out.println(arg0);
				
			}
		});
//
//		manager.addMagnetoListener(new MagnetoListener() {
//			
//			@Override
//			public void received(MagnetoData arg0) {
//				
//				System.out.println(arg0);
//				
//				
//				Platform.runLater(new Runnable() {
//					@Override
//					public void run() {
//						lbl.setText(arg0.getHeadingUnwrapped() + "");
//					}
//				});
//				
//			}
//		});
		
	}

}
