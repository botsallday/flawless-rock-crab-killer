package scripts.BADFlawlessRockCrabs.api.clicking;

import org.tribot.api.DynamicClicking;
import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.types.generic.Condition;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Player;
import org.tribot.api2007.ext.Filters;
import org.tribot.api2007.types.RSGroundItem;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSItemDefinition;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.Camera;
import org.tribot.api2007.Equipment;
import org.tribot.api2007.GroundItems;


public class BADClicking {
	
   public boolean actionsContainOption(String[] actions, String key) {
	   for (int i = 0; i < actions.length; i++) {
		   if (actions[i].contains(key)) {
			   return true;
		   }
	   }
	   
	   return false;
   }
	
    public boolean collectAnimableObject(RSObject obj, String option) {
        if (!Inventory.isFull()) {
            if (obj.isOnScreen() && obj.isClickable()) {
                if (DynamicClicking.clickRSObject(obj, option)) {
                    // wait until we finish picking the obj
                    Timing.waitCondition(new Condition() {
                        @Override
                        public boolean active() {
                            // control cpu usage
                            General.sleep(250, 500);
                            // ensure we have deposited items
                            return Player.getAnimation() == -1;
                        }
                    }, General.random(500, 750));
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public RSObject[] getNearestObjects(String obj) {
    	return Objects.findNearest(30, obj);
    }
    
    public boolean plantSeed(RSObject plot, RSItem seed) {
    	
    	if (!plot.isOnScreen()) {
    		Camera.turnToTile(plot.getPosition());
    	}
    	
    	if (seed.isClickable() && seed.hover()) {
    		if (seed.click("Use")) {
    			if (!plot.isOnScreen()) {
    				Camera.turnToTile(plot.getPosition());
    			}
	    		if (DynamicClicking.clickRSObject(plot, "Use")) {
	    			return true;
	    		}
    		}
    	}
    	
    	return false;
    }
    
    public boolean clickGroundObject(RSObject obj, String option, int min, int max) {
        if (!Inventory.isFull()) {
            if (obj.isOnScreen() && obj.isClickable()) {
                if (DynamicClicking.clickRSObject(obj, option)) {
                    return true;
                }
            } else {
            	Camera.turnToTile(obj.getPosition());
            }
        }
        
        return false;
    }
    
    public boolean farmPlot(String action, RSObject plot, int min, int max) {
		if (clickGroundObject(plot, action, min, max)) {
	       Timing.waitCondition(new Condition() {
	           @Override
	           public boolean active() {
	               // control cpu usage
	               General.sleep(500);
	               // ensure we have deposited items
	               return Player.getAnimation() > 0 || Player.isMoving();
	           }
	       }, General.random(min, max));
	       // wait until we finish picking the obj
	       Timing.waitCondition(new Condition() {
	           @Override
	           public boolean active() {
	               // control cpu usage
	               General.sleep(2500);
	               // ensure we have deposited items
	               return Objects.find(3, Filters.Objects.actionsContains("Clear", "Harvest", "Pick")).length < 1 && Player.getAnimation() < -1 && !Player.isMoving();
	           }
	       }, General.random(min, max));
		       
			General.println("Raked area");
			return true;
		}
		
		return false;
    }
    
	public boolean getGroundItemByName(String item) {
		// get ground items
		RSGroundItem[] groundItems = GroundItems.findNearest(item);
		// get definition
		RSItemDefinition definition = groundItems[0].getDefinition();
		
	    if (definition != null) {
	        String name = definition.getName();
	        if (name != null) {
	        	// take item from stack
	            if (groundItems[0].click("Take " + name)) {
	            	// count inventory items
	            	final int count = Inventory.getAll().length;
	            	
	            	// wait condition
	            	Timing.waitCondition(new Condition() {
	                    @Override
	                    public boolean active() {
	                        return Inventory.getAll().length > count;
	                    }
	                }, General.random(3000, 6000));
	            	
	            	return true;
	            }
	        }
	    }
	    
	    return false;
	}
	
	public RSObject[] nearestObjectByAction(String action, int dist) {
		return Objects.findNearest(dist, Filters.Objects.actionsContains(action));
	}
	
	public boolean useItemOnPlot(RSObject plot, RSItem[] obj) {
		if (obj.length > 0) {
			
			int count = Inventory.getCount(obj[0].getID());
			System.out.println("Found seed in inventory");
			if (plantSeed(plot, obj[0])) {
		       Timing.waitCondition(new Condition() {
		           @Override
		           public boolean active() {
		               // control cpu usage
		               General.sleep(500);
		               // ensure we have deposited items
		                   return !Player.isMoving() || Player.getAnimation() < 0;
		               }
	           		}, General.random(3000, 6000));
		       Timing.waitCondition(new Condition() {
		           @Override
		           public boolean active() {
		               // control cpu usage
		               General.sleep(500);
		               // ensure we have deposited items
		                   return Inventory.getCount(obj[0].getID()) < count;
		               }
		           }, General.random(3000, 6000));
				}
				
				if (obj[0].getStack() < count) {
					return true;
				} else {
					return false;
				}
		}
		
		return false;
	}
	
	public boolean hasItemEquipped(String item) {
		return Equipment.isEquipped(Filters.Items.nameContains(item));
	}
	
	public boolean equipItem(String item_name) {
		
    	RSItem[] item = Inventory.find(Filters.Items.nameContains(item_name));
    	
    	if (item.length > 0) {
    		if (item[0].click("Wield")) {
			       Timing.waitCondition(new Condition() {
			           @Override
			           public boolean active() {
			               // control cpu usage
			               General.sleep(100);
			               // ensure we have deposited items
			               return hasItemEquipped(item_name);
			           }
			       }, General.random(3000, 5000));
			       return true;
    		}
    	}
    	
		
		return false;
	}
}
