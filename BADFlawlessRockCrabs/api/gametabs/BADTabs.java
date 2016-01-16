package scripts.BADFlawlessRockCrabs.api.gametabs;

import org.tribot.api2007.GameTab;

public class BADTabs {
	
	// Ensure we don't try to open the tab if it is already open
	public boolean openTab(GameTab.TABS tab) {
		if (GameTab.getOpen() != tab && GameTab.open(tab)) {
				return true;
		}
		
		return false;
	}
	
}
