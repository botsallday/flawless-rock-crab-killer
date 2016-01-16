package scripts.BADFlawlessRockCrabs.api.antiban;

import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.types.generic.Condition;
import org.tribot.api.util.ABCUtil;
import org.tribot.api2007.Camera;
import org.tribot.api2007.GameTab;
import org.tribot.api2007.Player;
import org.tribot.api2007.Skills;
import org.tribot.api2007.types.RSNPC;

public class BADAntiBan {
	
	public String antiban_status;
	public long antiban_performed;
	public ABCUtil abc;
	private long last_anti_ban_action;
	private Skills.SKILLS hover_skill;
	public boolean ignore_examine;
	
    public BADAntiBan() {
    	General.useAntiBanCompliance(true);
    	log("Starting antiban");
    	abc = new ABCUtil();
    	antiban_performed = 0;
    	antiban_status = "Waiting";
    	last_anti_ban_action = 0;
    	this.ignore_examine = false;
    }
    
    public void setHoverSkill(Skills.SKILLS skill) {
    	this.hover_skill = skill;
    }
    
    private boolean openGameTab(GameTab.TABS tab) {
		if (GameTab.open(tab)) {
            Timing.waitCondition(new Condition() {
                @Override
                public boolean active() {
                    // control cpu usage
                    General.sleep(150, 250);
                    // ensure we have opened the tab
                    return GameTab.getOpen() == tab;
                }
            }, General.random(1000, 2000));
            
            return true;
		}
		
		return false;
    }
	
    private boolean performTabAntiBan(long next, GameTab.TABS tab) {
    	
		if (System.currentTimeMillis() >= next && GameTab.getOpen() != tab) {
			log("Performing check tab anti ban");
			if (openGameTab(tab)) {
				antiban_status = "Performing antiban action";
				antiban_performed ++;
				log("Successfully performed check tab "+"("+tab+") antiban");
				return true;
			};
			
			return true;
		}
			
		return false;
		
    }
    
    public boolean handleHoverNextNPC(RSNPC npc) {
    	if (abc.BOOL_TRACKER.HOVER_NEXT.next()) {
    		if (!npc.isOnScreen()) {
    			Camera.turnToTile(npc);
    		}
    		
    		npc.hover();
    		abc.BOOL_TRACKER.HOVER_NEXT.reset();
    		return true;
    	}
    	
    	return false;
    }
    
    public RSNPC handleUseClosest(RSNPC a, RSNPC b) {
    	
		if (abc.BOOL_TRACKER.USE_CLOSEST.next()) {
			if (b.getPosition().distanceTo(a.getPosition()) < 4) {
				abc.BOOL_TRACKER.USE_CLOSEST.reset();
				return b;
			}
		}
		
		return a;
    	
    }
    
    public boolean handleItemInteractionDelay() {
    	// handle delay between interacting with multiple items
    	General.sleep(abc.DELAY_TRACKER.ITEM_INTERACTION.next());
    	abc.DELAY_TRACKER.ITEM_INTERACTION.reset();
    	return true;
    }
    
    public boolean handleSwitchObjectCombatDelay() {
    	// handle waiting between fighting npcs
    	General.sleep(abc.DELAY_TRACKER.SWITCH_OBJECT_COMBAT.next());
    	abc.DELAY_TRACKER.SWITCH_OBJECT_COMBAT.reset();
    	return true;
    }
    
    public boolean handleNewObjectCombatDelay() {
    	// handle waiting for a new object
    	General.sleep(abc.DELAY_TRACKER.NEW_OBJECT_COMBAT.next());
    	abc.DELAY_TRACKER.NEW_OBJECT_COMBAT.reset();
    	return false;
    }
    
    private void log(String string) {
    	System.out.println(string);
	}

	public void handleWait() {
    	antiban_status = "Checking";
		abc.performTimedActions(this.hover_skill);
    	antiban_status = "Waiting";
    }
	
	public void resetTimer() {
		last_anti_ban_action = Timing.currentTimeMillis();
	}
	
	public boolean hoverSkill(Skills.SKILLS skill) {
		if (openGameTab(GameTab.TABS.STATS)) {
			if (skill.hover()) {
	            Timing.waitCondition(new Condition() {
	                @Override
	                public boolean active() {
	                    // control cpu usage
	                    General.sleep(150, 250);
	                    // ensure we have opened the tab
	                    return GameTab.getOpen() == GameTab.TABS.STATS && skill.hover();
	                }
	            }, General.random(2000, 6000));
	            
				return true;
			}
		}
		
		return false;
	}
}
