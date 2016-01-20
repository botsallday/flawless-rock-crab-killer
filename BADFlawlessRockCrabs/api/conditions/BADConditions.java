package scripts.BADFlawlessRockCrabs.api.conditions;

import org.tribot.api.General;
import org.tribot.api.types.generic.Condition;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Combat;
import org.tribot.api2007.Game;
import org.tribot.api2007.GameTab;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Login;
import org.tribot.api2007.Player;
import org.tribot.api2007.Skills;
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSTile;

import scripts.BADFlawlessRockCrabs.api.interfaces.BADInterfaces;


public class BADConditions {
	public static final Condition IN_BANK = inBank();
	public static final Condition SPUN_FLAX = spunFlax();
	public static final Condition BANK_OPEN = bankOpen();
	public static final Condition INVENTORY_EMPTY = inventoryEmpty();
	public static final Condition INVENTORY_TAB_OPEN = inventoryOpen();
	public static final Condition WAIT_IDLE = isIdle();
	public static final Condition ON_GROUND_FLOOR = onGroundFloor();
	public static final Condition ON_FIRST_FLOOR = onFirstFloor();

	public static Condition isFighting() {
		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(100);
	        	return Combat.isUnderAttack() || Combat.getTargetEntity() != null;
	        }
		};
	}
	
	public static Condition crosshairChange() {
		int crosshair_state = Game.getCrosshairState();
		
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Game.getCrosshairState() != crosshair_state;
			}
		};
	}
	
	public static Condition playerPositionIs(RSTile tile) {
		return new Condition() {
			@Override
			public boolean active() {
				General.sleep(100);
				return Player.getPosition().distanceTo(tile) < 1;
			}
		};
	}
	public static Condition foodWithdrawn() {

		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(100);
	        	return Inventory.isFull();
	        }
		};

	}

	public static Condition attackableEnemy(RSNPC enemy) {
		return new Condition() {
	        @Override
	        public boolean active() {
	            // control cpu usage
	            General.sleep(100);
	            
            	if ((!enemy.isInCombat() || enemy.isInteractingWithMe()) && (enemy.isMoving() || Combat.isUnderAttack())) {
            		return true;
            	}
	            
	            return false;
	        }
		};
	}

	public static Condition isLoggedIn() {
		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(100);
	        	return Login.getLoginState() != Login.STATE.LOGINSCREEN;
	        }
		};
	}

	public static Condition combatPotionDrank() {
		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(100);
	        	return Skills.getActualLevel(Skills.SKILLS.ATTACK) < Skills.getCurrentLevel(Skills.SKILLS.ATTACK);
	        }
		};
	}
	
	private static Condition inventoryEmpty() {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Inventory.getAll().length == 0;
			}
		};
	}
	
	public static Condition interfaceOpen(int parent, int child) {
		return new Condition() {
			@Override
			public boolean active() {
				General.sleep(100);
				return BADInterfaces.getChildInterface(parent, child) != null;
			}
		};
	}
	
	public static Condition interfaceClosed(int parent, int child) {
		return new Condition() {
			@Override
			public boolean active() {
				General.sleep(100);
				return BADInterfaces.getChildInterface(parent, child) == null;
			}
		};
	}
	
	private static Condition onGroundFloor() {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Player.getPosition().getPlane() == 0;
			}
		};
	}
	
	private static Condition onFirstFloor() {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Player.getPosition().getPlane() == 1;
			}
		};
	}
	
	private static Condition isIdle() {
		return new Condition() {
			@Override
			public boolean active() {
				General.sleep(100);
				return waitIdle(1000);
			}
		};
	}
	
	private static Condition inventoryOpen() {
		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(1000);
	        	return GameTab.getOpen() == GameTab.TABS.INVENTORY;
	        }
		};
	}
	
	public static Condition hasItem(String name) {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Inventory.getCount(name) > 0;
			}
		};
	}
	
	private static Condition inBank() {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return Banking.isInBank();
			}
		};
	}
	
	private static Condition bankOpen() {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(1000);
				return Banking.isBankScreenOpen();
			}
		};
	}
	
	
	public static Condition inArea(RSArea area) {
		return new Condition() {
			@Override
			public boolean active()
			{
				General.sleep(100);
				return area.contains(Player.getPosition());
			}
		};
	}
	
	public static Condition nearTile(RSTile tile, int distance) {
		return new Condition() {
			@Override
			public boolean active() {
				General.sleep(100);
				return Player.getPosition().distanceTo(tile) <= distance;
			}
		};
	}
	
	private static Condition spunFlax() {
		return new Condition() {
	        @Override
	        public boolean active() {
	        	General.sleep(1000);
	        	// see if we have flax left
	        	if (Inventory.getCount("Flax") > 0) {
	        		// see if we are still spinning (handles reaction to leveling up which makes you stop spinning)
	        		if (Player.getAnimation() == -1) {
	        			// since we will be idle for about 1/2 second we must ensure that we are actually done spinning and not in between spins
	        			return waitIdle(2000);
	        		}
	        		return false;
	        	}
				return true;
	        }
		};
	};
	
    public static boolean waitIdle(long amount) {
    	// capture current time
    	long time = System.currentTimeMillis();
    	// we will break if the player animation moves from idle or we wait the requested amount of time
    	while (Player.getAnimation() == -1) {
    		General.sleep(100);
    		if (System.currentTimeMillis() > time + amount) {
    			return true;
    		}
    	}
    	return false;
    }
	

}
