package picture;

import java.util.ArrayList;

import app.CommandController;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.MagnetoData;
import de.yadrone.base.navdata.MagnetoListener;
import de.yadrone.base.navdata.NavDataManager;
import de.yadrone.base.utils.ARDroneUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;


public class NavDataTracker {

	private CommandController cC;
	private ArrayList<MagnetoListener> magnetoListener = new ArrayList<MagnetoListener>();
	
	
	public void initCompass(IARDrone drone, Label lbl)
	{
		//cC = new CommandController(drone);
		NavDataManager manager = drone.getNavDataManager();
		manager.addMagnetoListener(new MagnetoListener() {
			
			@Override
			public void received(MagnetoData arg0) {
				//System.out.println(arg0.getHeadingFusionUnwrapped() + " fusion");
				//System.out.println(arg0.getHeadingGyroUnwrapped() + " gyro");
				System.out.println(arg0.getHeadingUnwrapped());
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						lbl.setText(arg0.getHeadingUnwrapped() + "");
					}
				});
				
			}
		});
		
	}

}
