package scripts.BADFlawlessRockCrabs.api.banking;

import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api2007.Banking;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSTile;

import scripts.BADFlawlessRockCrabs.api.conditions.BADConditions;
import scripts.BADFlawlessRockCrabs.api.transportation.BADTransportation;

public class BADBanking {
	private BADTransportation transport = new BADTransportation();
	// deposit all items
	public int depositAll() {
		if (canBank()) {
			return Banking.depositAll();
		}
		return 0;
	}
	
	public boolean withdraw(String item_name, int count) {
		if (canBank()) {
			return Banking.withdraw(count, item_name);
		}
		
		return false;
	}
	
	public int distanceToBank(RSTile tile1, RSTile tile2) {
		return tile1.distanceTo(tile2);
	}
	
	public boolean hasItem(String item_name) {
		if (canBank()) {
			return Banking.find(item_name).length > 0;
		}
		
		return false;
	}
	
	private boolean canBank() {
		// ensure we can bank, and wait for the bank to open if needed
		if (ensureAbleToBank()) {
			Timing.waitCondition(BADConditions.BANK_OPEN, 3000);
			return true;
		}
		
		return false;
	}
	
	// Ensures we are in the bank, and opens bank if needed. Will not move into the bank if you arent already inside.
	private boolean ensureAbleToBank() {
		if (!Banking.isInBank()) {
			return false;
		}
		
		if (!Banking.isBankScreenOpen()) {
			if (Banking.openBank()) {
				if (Banking.isBankScreenOpen()) {
					Timing.waitCondition(BADConditions.BANK_OPEN, 3000);
					return true;
				}
			};
		}
		
		return true;
	}
	
	public boolean walkToBank(RSArea bank_area, boolean check_run) {
		if (check_run) {
			transport.checkRun();
		}
		if (!Banking.isInBank()) {
			if (transport.webWalking(bank_area.getRandomTile()) && Timing.waitCondition(BADConditions.IN_BANK, General.random(3000, 6000))) {
				return true;
			}
		}
		
		return false;
	}
}
