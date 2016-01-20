package scripts.BADFlawlessRockCrabs.framework.flawlessrockcrabkiller;

import scripts.BADFlawlessRockCrabs.api.antiban.BADAntiBan;
import scripts.BADFlawlessRockCrabs.api.areas.BADAreas;
import scripts.BADFlawlessRockCrabs.api.banking.BADBanking;
import scripts.BADFlawlessRockCrabs.api.clicking.BADClicking;
import scripts.BADFlawlessRockCrabs.api.conditions.BADConditions;
import scripts.BADFlawlessRockCrabs.api.fcworldhopper.FCInGameHopper;
import scripts.BADFlawlessRockCrabs.api.skills.BADSkillTracker;
import scripts.BADFlawlessRockCrabs.api.transportation.BADTransportation;
import scripts.BADFlawlessRockCrabs.framework.gui.CrabGUI;
import org.tribot.api.DynamicClicking;
import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.types.generic.Condition;
import org.tribot.api2007.Banking;
import org.tribot.api2007.Camera;
import org.tribot.api2007.Combat;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.Login;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Objects;
import org.tribot.api2007.Player;
import org.tribot.api2007.Players;
import org.tribot.api2007.Skills;
import org.tribot.api2007.Walking;
import org.tribot.api2007.WebWalking;
import org.tribot.api2007.WorldHopper;
import org.tribot.api2007.ext.Filters;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSNPCDefinition;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSPlayer;
import org.tribot.api2007.types.RSTile;

public class FlawlessRockCrabKillerCore {
	
	private BADBanking BANKER = new BADBanking();
	private BADTransportation TRANSPORT = new BADTransportation();
	private BADAntiBan AB = new BADAntiBan();
	
	private boolean USE_POTIONS;
	private boolean REFRESHING;
	private boolean EXECUTE = true;
	private long REFRESH_TIME = Timing.currentTimeMillis() + refreshRandom();
	private int FAILED_WAKES = 0;
	private String FOOD;
	private String TAB = "Camelot teleport";
	private String POTION = "Combat POTION";
	private CrabGUI GUI = new CrabGUI();
	private BADSkillTracker TRACKER = new BADSkillTracker();
	
	private Condition bad_items_deposited = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Inventory.find(Filters.Items.nameNotEquals(FOOD, TAB)).length < 2;
        }
	};

	public boolean executing() {
		return EXECUTE;
	}
	
	public void run() {
		GUI.setVisible(true);
		while (GUI.isVisible()) {
			General.sleep(100);
			General.println("Waiting for GUI");
		}
		setGuiVars();
		AB.setHoverSkill(Skills.SKILLS.HITPOINTS);
		
		while (EXECUTE) {
			General.sleep(50);
			switch (state()) {
				case LOGIN:
					Timing.waitCondition(BADConditions.isLoggedIn(), 10000);
					break;
				case TELEPORT:
					if (teleport()) {
						AB.handleItemInteractionDelay();
	                    Timing.waitCondition(BADConditions.inArea(BADAreas.CAMELOT_TELEPORT_AREA), General.random(8000, 12000));
					}
					break;
				case WALK_TO_BANK:
					if (!BANKER.walkToBank(BADAreas.CAMELOT_BANK_AREA, true)) {
						TRANSPORT.walkCustomNavPath(BADAreas.CAMELOT_BANK_AREA.getRandomTile());
					}
					break;
				case WALK_TO_ROCKS:
					walkToRocks();
					break;
				case WAITING:
					AB.handleWait();
					break;
				case WALKING:
					AB.handleWait();
					break;
				case WITHDRAW_TELEPORT:
					if (!BANKER.withdraw(TAB, 1)) {
						if (!BANKER.hasItem(TAB)) {
							EXECUTE = false;
						}
					};
					AB.handleItemInteractionDelay();
                    Timing.waitCondition(BADConditions.hasItem(TAB), General.random(3000, 5000));
					break;
				case WITHDRAW_FOOD:
					if (!BANKER.withdraw(FOOD, 28)) {
						if (!BANKER.hasItem(FOOD)) {
							EXECUTE = false;
						}
					} else {
						AB.handleItemInteractionDelay();
	                    Timing.waitCondition(BADConditions.hasItem(FOOD), General.random(3000, 5000));
					}
                    break;
				case WITHDRAW_POTIONS:
					if (!BANKER.withdraw(POTION, 1)) {
						USE_POTIONS = false;
					};
					AB.handleItemInteractionDelay();
					break;
				case REFRESH_CRABS:
					TRANSPORT.checkRun();
					General.println("Refreshing crabs");
					
					if (refreshForest()) {
						resetRefreshTime();
					}
					break;
				case WAKE_CRAB:
					TRANSPORT.checkRun();
					AB.handleNewObjectCombatDelay();
					attemptToWakeCrab();
					break;
				case COMBAT:
					eat();
					if (USE_POTIONS) {
						checkPotion();
					}
					break;
				case SOMETHING_WENT_WRONG:
					EXECUTE = false;
					break;
				case DEAD:
					General.println("You died!");
					EXECUTE = false;
					break;
				case DEPOSIT_ITEMS:
					if (depositBadItems()) {
	                    Timing.waitCondition(bad_items_deposited, General.random(8000, 12000));
					};
					break;
				case CANNON_DETECTED:
					General.println("Trying to hop worlds because of cannon");
					hop();
					break;
				case TOO_MANY_PLAYERS:
					hop();
					break;
				case NAVIGATE_PATH:
					if (navigatePath()) {
						General.println("Navigated path");
					}
					break;
				case TURN_ON_AUTO:
					turnAutoRetaliateOn();
					break;
			}
		}
	}
	
  public State state() {

	  // handle login state
	  if (Login.getLoginState() == Login.STATE.LOGINSCREEN) {
		  return State.LOGIN;
	  }
	  // handle death
	  if (BADAreas.LUMBRIDGE_SPAWN_AREA.contains(Player.getPosition())) {
		  EXECUTE = false;
		  return State.DEAD;
	  }
	  // turn on auto retaliate if it isn't on
	  if (!autoRetaliateOn()) {
		  return State.TURN_ON_AUTO;
	  }

	  
	  if (!busy()) {
		  // determine if we should hop based on the number of players in our area
		  if (detectPlayers()) {
			  return State.TOO_MANY_PLAYERS;
		  }
		  // determine if we should hop based on a cannon in our area
		  if (detectCannon() && BADAreas.EAST_ROCK_CRABS_AREA.contains(Player.getPosition())) {
			  return State.CANNON_DETECTED;
		  }
		  // when out of food we must try to bank
		  if (outOfFood() && !Banking.isInBank()) {
			  // only teleport if needed
			  if (BANKER.distanceToBank(BADAreas.CAMELOT_BANK_AREA.getRandomTile(), Player.getPosition()) > 50) {
				  return State.TELEPORT;
			  }
			  return State.WALK_TO_BANK;
		  }
		  // walk to bank if we have teleported
		  if (BADAreas.CAMELOT_TELEPORT_AREA.contains(Player.getPosition())) {
			  return State.WALK_TO_BANK;
		  }
		  // handle getting supplies and leaving bank
		  if (Banking.isInBank()) {
			  // deposit items that we don't need to take with us
			  if (inventoryContainsBadItems()) {
				  General.println("Has bad items");
				  return State.DEPOSIT_ITEMS;
			  }
			  // see if we need to withdraw supplies
			  if (needsAnySupplies()) {
				  if (needsTeleport()) {
					  return State.WITHDRAW_TELEPORT;
				  }
				  if (USE_POTIONS && needsPotions()) {
					  return State.WITHDRAW_POTIONS;
				  }
				  if (outOfFood() || !Inventory.isFull()) {
					  return State.WITHDRAW_FOOD;
				  }
			  } else {
				  // we are ready to go to the crabs
				  return State.NAVIGATE_PATH;
			  }
		  }
		  // handle walking to crabs
		  if (!atCrabs() && !REFRESHING && !Banking.isInBank()) {
			  return State.WALK_TO_ROCKS;
		  }
		  // handle REFRESHING crabs
		  if ((Timing.currentTimeMillis() >= REFRESH_TIME) || REFRESHING || FAILED_WAKES > 9) {
			  REFRESHING = true;
			  return State.REFRESH_CRABS;
		  }
		  // handle waking a crab
		  if (atCrabs()) {
			  return State.WAKE_CRAB;
		  }
	  } else {
		  // if we are in combat or chasing a crab
		  if (Combat.isUnderAttack() || hasTarget()) {
			  return State.COMBAT;
		  }
		  // if we are running away from something, probably
		  if (Player.isMoving()) {
			  return State.WALKING;
		  }
		  // we are doing something non combat related
		  if (Player.getAnimation() > -1) {
			  return State.WAITING;
		  }
	  }
	  return State.SOMETHING_WENT_WRONG;
  }
	  
   public enum State {
        WALK_TO_BANK,
        WALK_TO_ROCKS,
        DEPOSIT_ITEMS,
        SOMETHING_WENT_WRONG,
        WALKING,
        WITHDRAW_TELEPORT,
        WITHDRAW_FOOD,
        WITHDRAW_POTIONS,
        WAITING,
        COMBAT,
        REFRESH_CRABS,
        WAKE_CRAB,
        TELEPORT,
        CANNON_DETECTED,
        DEAD,
        LOGIN,
        TOO_MANY_PLAYERS,
        NAVIGATE_PATH,
        TURN_ON_AUTO
    }
   
   private long refreshRandom() {
	   // return a random number between 7-9.8 minutes
	   return General.random(420000, 580000);
   }
   
   private RSTile[] generateNavigationPath() {
	   RSTile[] path = {
			   BADAreas.CRABS_WALK_CHECKPOINT_ONE.getRandomTile(), 
			   BADAreas.CRAB_WALK_CHECKPOINT_TWO.getRandomTile(),
			   BADAreas.CRAB_WALK_CHECKPOINT_THREE.getRandomTile(),
			   BADAreas.CRAB_WALK_CHECKPOINT_FOUR.getRandomTile(),
			   BADAreas.CRAB_WALK_CHECKPOINT_FIVE.getRandomTile(),
			   BADAreas.CRAB_WALK_CHECKPOINT_SIX.getRandomTile(),
	   };
   
	   return path;
   }
   
   private boolean navigatePath() {
	   General.println("Navigating path");
	   
	   RSTile[] path = generateNavigationPath();
	   boolean success = true;
	   General.println("Starting navigation");
	   
	   for (int i=0; i<path.length; i++) {
		   if (Walking.blindWalkTo(path[i])) {
			   General.println("Walking path"+" "+i);
		   } else {
			   General.println("fail");
			   success = false;
			   break;
		   }
	   }
	   
		return success;
   }
   
   private boolean refreshForest() {
	   // use the forest to refresh crabs
		if (TRANSPORT.walkPath(TRANSPORT.nav().findPath(BADAreas.REFRESH_CRABS_AREA.getRandomTile()))) {
			General.println("Web walked to forest");
			REFRESHING = false;
			return true;
		} else {
			General.println("Custom pathing to forest");
			// walk with a custom path
			if (Walking.walkPath(TRANSPORT.generateRandomStraightPath(BADAreas.REFRESH_CRABS_AREA.getRandomTile(), 1, 1))) {
				REFRESHING = false;
				return true;
			};
		}
		
		return false;
   }
   
   private boolean refreshTunnel() {
	   // attempt to refresh using the tunnel
		if (walkToTunnel()) {
			General.println("Walked to tunnel");
            Timing.waitCondition(BADConditions.inArea(BADAreas.EAST_CRABS_TUNNEL_AREA), General.random(8000, 12000));
            // enter tunnel
			if (enterTunnel()) {
				General.println("Entered tunnel");
				Timing.waitCondition(BADConditions.playerPositionIs(BADAreas.CRABS_TUNNEL_ENTERANCE_TILE), General.random(8000, 12000));
				// exit tunnel
				if (exitTunnel()) {
					General.println("Exited tunnel");
					REFRESHING = false;
					Timing.waitCondition(BADConditions.inArea(BADAreas.EAST_CRABS_TUNNEL_AREA), General.random(8000, 12000));
					return true;
				}
			}
		}
		
		return false;
   }
   
   private void walkToRocks() {
	   // attempt to walk to the crab area with a randomized path to avoid bans
		if (Walking.walkPath(TRANSPORT.generateRandomStraightPath(BADAreas.EAST_ROCK_CRABS_AREA.getRandomTile(), 3, 1))) {
			General.println("Walked custom randomized path");
		} else {
			General.println("Failed to walk custom randomized path");
		}
   }
   
   private RSNPC findAttackableCrab(RSNPC[] crabs) {
	   // iterate crabs
	   for (int i = 0; i < crabs.length; i++) {
		   // make sure noone else is fighting the crab
		   if (!crabs[i].isInCombat() || crabs[i].isInteractingWithMe()){
			   // ensure crab is on screen
			   if (!crabs[i].isOnScreen()) {
				   Camera.turnToTile(crabs[i]);
			   }
			   // get the definition 
			   RSNPCDefinition crab_definition = crabs[i].getDefinition();
			   // null check
			   if (crab_definition != null) {
				   // comply with antibans use closest policy
				   if (!actionsContainOption(crab_definition.getActions(), "Attack")){
					   // check to see if we should use the closest, or next closest
					   if (i+1 < crabs.length) {
						   return AB.handleUseClosest(crabs[i], crabs[i+1]);
					   }
					   
					   return crabs[i];
				   }
			   }
		   }
	   }
	   // if we must steal it then we shall
	   return crabs[0];
   }
   
   private RSTile getAdjacent(RSTile tile) {
	   // randomly determine distance for tile
	   switch (General.random(1, 2)) {
	   		case 1:
	   		   return tile.translate(General.random(0, 1), 0);
	   		case 2:
	   		   return tile.translate(0, General.random(0, 1));
	   }
	   
	   return tile.translate(General.random(0, 1), General.random(0, 1));

   }
   
   private void attemptToWakeCrab() {
	   // get crabs
	   RSNPC[] crabs = crabs();
	   
	   if (crabs.length > 0) {
		   TRANSPORT.checkRun();
		   // cache the crab so we can see if it woke for us
		   RSNPC crab = findAttackableCrab(crabs);
		   // switch between screen and minimap clicks
		   switch (General.random(1, 2)) {
		   	   case 1:
		   		   General.println("Attempting to minimap click to wake crab");
		   		   TRANSPORT.checkRun();
		   		   // use a function for a chance at getting a random adjacent tile to the crab
				   if (TRANSPORT.webWalking(getAdjacent(crab.getPosition()))) {
					   // wait for the attack
		               Timing.waitCondition(BADConditions.attackableEnemy(crab), General.random(10000, 12000));
		               General.println("Attempting to attack crab after minimap wake");
				   };
				   break;
		   	   case 2:
		   		   General.println("Attempting to screen click to wake crab");
		   		   // use a function for a chance at getting a random adjacent tile to the crab
				   if (Walking.clickTileMS(getAdjacent(crab.getPosition()), "Walk here")) {
		               Timing.waitCondition(BADConditions.attackableEnemy(crab), General.random(6000, 8000));
			   		   General.println("Attempting to awake crab after screen click wake");
				   };
		   
		   }
		   
		   // wait to see if we can enter combat
		   if (Timing.waitCondition(BADConditions.isFighting(), General.random(3000, 5000))) {
			   // ensure we are waking crabs up, if not we will refresh
			   FAILED_WAKES = 0;
			   General.println("Woke crab successfully");

		   } else {
			   General.println("Failed to wake crab");
			   FAILED_WAKES++;
		   }
	   }
   }
   
   private RSNPC[] crabs() {
	   return NPCs.findNearest(Filters.NPCs.nameContains("Rock").combine(Filters.NPCs.inArea(BADAreas.EAST_ROCK_CRABS_AREA), true).combine(Filters.NPCs.nameNotContains("olem"), true));
   }
   
   private boolean atCrabs() {
	   return BADAreas.EAST_ROCK_CRABS_AREA.contains(Player.getPosition());
   }
   
   private boolean busy() {
	   // if we are doing anything, we shouldn't try doing something else
	   return Player.isMoving() || Player.getAnimation() > -1 || Combat.isUnderAttack() || hasTarget();
   }
   
   private boolean hasTarget() {
	   return Combat.getTargetEntity() != null;
   }
   
   private boolean needsAnySupplies() {
	   // see if we need supplies
	   if (Banking.isInBank() && !Inventory.isFull()) {
		   return true;
	   }
	   
	   return needsTeleport() || outOfFood();
   }
   
   private boolean inventoryContainsBadItems() {
	   // get inventory contents
	   RSItem[] inventory = inventory();
	   
	   if (inventory.length > 0) {
		   // we have items that we don't need
		   return true;
	   }
	   
	   return false;
   }
   
   private boolean depositBadItems() {
	   // make sure the bank is open
	   if (!Banking.isBankScreenOpen()) {
		   Banking.openBank();
	   }
	   
	   if (Banking.depositAll() > 0) {
		   Timing.waitCondition(bad_items_deposited, 4000);
		   return true;
	   }
	   
	   return false;
   }
   
   private RSItem[] inventory() {
	   return Inventory.find(Filters.Items.nameNotEquals(FOOD, TAB).combine(Filters.Items.nameNotContains(POTION), true));
   }
   
   private boolean needsTeleport() {
	   return Inventory.getCount(TAB) < 1;
   }
   
   private boolean outOfFood() {
	   return Inventory.find(Filters.Items.actionsContains("Eat")).length < 1;
   }
   
   private boolean needsPotions() {
	   return Inventory.find(Filters.Items.nameContains(POTION)).length < 1;
   }
   
   private boolean teleport() {
	   // find TABs
	   RSItem[] teleport = Inventory.find(Filters.Items.nameEquals(TAB));
	   // null check
	   if (teleport.length > 0 && teleport[0].hover() && teleport[0].click("Teleport")) {
		   return true;
	   }
	   
	   return false;
   }
	
	public int maxhp() {
		return Combat.getMaxHP();
	}
	
	public int hp() {
		return Combat.getHP();
	}
	
	public float healthPercent() {
		// get health percent
	    float perc = (float)hp()/(float)maxhp();
		return perc*100;
	}
	
	private boolean detectCannon() {
		// get cannons
		RSObject[] cannon = Objects.find(40, Filters.Objects.nameContains("Dwarf multi"));
		// null check
		if (cannon.length > 0) {
			// iterate cannons
			for (int i = 0; i < cannon.length; i++) {
				// ensure they are in the same area we are killing crabs in
				if (BADAreas.EAST_ROCK_CRABS_AREA.contains(cannon[i].getPosition())) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean autoRetaliateOn() {
		return Combat.isAutoRetaliateOn();
	}
	
	private boolean turnAutoRetaliateOn() {
		return Combat.setAutoRetaliate(true);
	}
	
	private void setGuiVars() {
		FOOD = GUI.getFoodName();
		USE_POTIONS = GUI.usePotions();
	}
	
	@SuppressWarnings("deprecation")
	public boolean eat() {
		// eat based on abc utils
		if (healthPercent() < AB.abc.INT_TRACKER.NEXT_EAT_AT.next()) {
			if (eatFood()) {
				General.println("Ate food");
				AB.abc.INT_TRACKER.NEXT_EAT_AT.reset();
				return true;
			};
		}
		
		return false;
	}
	
	public boolean eatFood() {
	   General.println("Eating food");
	   RSItem[] foods = Inventory.find(FOOD);
	   // null check
	   if (foods.length > 0 && foods[0].hover() && foods[0].click("Eat")) {
			return true;
		}
		
		return false;
	}
	
	public boolean walkToTunnel() {
		return Walking.walkPath(TRANSPORT.generateRandomStraightPath(BADAreas.EAST_CRABS_TUNNEL_AREA.getRandomTile(), 1, 1));
	}
	
	public boolean exitTunnel() {
		// find tunnel
		RSObject[] tunnel = Objects.find(10, Filters.Objects.actionsContains("Enter").combine(Filters.Objects.nameContains("Tunnel"), true));
		// null check
		if (tunnel.length > 0) {
			// try to enter the tunnel (from the inside, so exit)
			if (DynamicClicking.clickRSObject(tunnel[0], "Enter")) {
				// wait until we are outside
				Timing.waitCondition(BADConditions.inArea(BADAreas.EAST_CRABS_TUNNEL_AREA), General.random(10000, 12000));
				return true;
			}
		}
		
		return false;
	}
	
   public boolean actionsContainOption(String[] actions, String key) {
	   for (int i = 0; i < actions.length; i++) {
		   General.println(actions[i]);
		   if (actions[i].contains(key)) {
			   return true;
		   }
	   }
	   
	   return false;
   }
	
	public boolean enterTunnel() {
		// find the tunnel
		RSObject[] tunnel = Objects.find(10, Filters.Objects.actionsContains("Enter").combine(Filters.Objects.nameContains("Tunnel"), true));
		// null check
		if (tunnel.length > 0) {
			// try to enter the tunnel
			if (DynamicClicking.clickRSObject(tunnel[0], "Enter")) {
				// wait until we're inside
				Timing.waitCondition(BADConditions.playerPositionIs(BADAreas.CRABS_TUNNEL_ENTERANCE_TILE), General.random(10000, 12000));
				return true;
			}
		}
		
		return false;
	}
	
	public boolean checkPotion() {
		// see if we should use the potion
		if (shouldUsePotion()) {
			usePotion();
			return true;
		}
		
		return false;
	}
	
	public boolean usePotion() {
		// get potions from inventory
		RSItem[] pot = Inventory.find(Filters.Items.nameContains(POTION));
		// null check
		if (pot.length > 0) {
			// try to drink the potion
			if (pot[0].click("Drink")) {
				// wait condition
				Timing.waitCondition(BADConditions.combatPotionDrank(), 3000);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean shouldUsePotion() {
		// determine if we should drink a potion
		if (TRACKER.skillIsAtBase(Skills.SKILLS.ATTACK) || TRACKER.skillIsAtBase(Skills.SKILLS.STRENGTH)) {
			return true;
		}
		
		return false;
	}
	
	public boolean detectPlayers() {
		// return a bool to determine if we should hop based on the number of players
		if (countPlayers() > 5) {
			return true;
		}
		
		return false;
	}
	
	public int countPlayers() {
		return players().length;
	}
	
	public RSPlayer[] players() {
		return Players.getAll(Filters.Players.inArea(BADAreas.EAST_ROCK_CRABS_AREA));
	}
	
	private boolean hop() {
		// use the lovely FC game hopper
		if (FCInGameHopper.hop(WorldHopper.getRandomWorld(true, false))) {;
			General.println("Hopped successfully!");
			return true;
		}
		
		return false;
	}
	
	private void resetRefreshTime() {
		REFRESH_TIME = Timing.currentTimeMillis() + refreshRandom();
	}
}
