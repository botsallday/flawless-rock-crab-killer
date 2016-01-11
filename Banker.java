package scripts;

import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.types.generic.Condition;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Walking;
import org.tribot.api2007.WebWalking;
import org.tribot.api2007.ext.Filters;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSItemDefinition;
import org.tribot.api2007.types.RSTile;
import org.tribot.api2007.Banking;

public class Banker {
	
	final Transportation transport;
	private AntiBan ab;
	
	Banker() {
		transport = new Transportation();
		ab = new AntiBan();
	}
	
	public boolean depositAll() {
		if (!Banking.isBankScreenOpen() && Banking.isInBank()) {
			Banking.openBank();
		}
	    if (Inventory.isFull()) {
	        if (Banking.depositAll() > 0) {
	            // condition for waiting until items are deposited
	            Timing.waitCondition(new Condition() {
	                @Override
	                public boolean active() {
	                    // control cpu usage
	                    General.sleep(300, 600);
	                    // ensure we have deposited items
	                    return !Inventory.isFull();
	                }
	            }, General.random(1000, 2500));
	        }
	    }
	    
	    return !Inventory.isFull();

	}
	
	public boolean depositAllBut(String item_name) {
		RSItem[] item = Inventory.find(item_name);
		
		if (item.length > 0) {
			RSItemDefinition definition = item[0].getDefinition();
			
			if (definition != null) {
				
				if (Banking.depositAllExcept(definition.getID()) > 0) {
					return true;
				}
				
			}
			
		} else {
			return depositAll();
		}
		
		return false;
	}
	
    public boolean closeBankScreen() {
        if (Banking.isBankScreenOpen()) {
            return Banking.close();
        }
        
        return false;
    }
    
    public boolean withdrawItem(String item_name, int amount) {
    	if (Banking.isInBank()) {
    		if (!Banking.isBankScreenOpen()) {
    			Banking.openBank();
    		}
    		
    		RSItem[] item = Banking.find(Filters.Items.nameContains(item_name));
    		
    		if (item.length > 0) {
    			if (Banking.withdraw(amount, item[0].name)) {
    	            Timing.waitCondition(new Condition() {
    	                @Override
    	                public boolean active() {
    	                    // control cpu usage
    	                    General.sleep(300, 600);
    	                    // ensure we have deposited items
    	                    return Inventory.find(Filters.Items.nameContains(item_name)).length > 0;
    	                }
    	            }, General.random(1000, 2500));
    	            
    	            ab.handleItemInteractionDelay();
    	            
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    public boolean hasItemInBank(String item_name) {
    	
    	if (Banking.find(Filters.Items.nameContains(item_name)).length > 0) {
    		return true;
    	}
    	
    	return false;
    }
    
    
    public boolean handleBanking(RSArea area, boolean check_run) {
         if (Banking.isInBank() && !Banking.isBankScreenOpen()) {
        	 return Banking.openBank();
         } else {
         	return Walking.walkTo(transport.getTile(area, check_run));
         }
     }
    
    public int distanceToBank(RSTile bank, RSTile player) {
    	return bank.distanceTo(player);
    }
}