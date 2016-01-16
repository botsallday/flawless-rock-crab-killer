package scripts.BADFlawlessRockCrabs.api.interfaces;

import org.tribot.api2007.Interfaces;
import org.tribot.api2007.types.RSInterfaceChild;

public class BADInterfaces {
	
	public static final int SPINNING_WHEEL_PARENT_ID = 459;
	public static final int SPINNING_WHEEL_CHILD_ID = 90;
	
    public static RSInterfaceChild getChildInterface(int parent, int child) {
		return Interfaces.get(parent, child);
    }
}
