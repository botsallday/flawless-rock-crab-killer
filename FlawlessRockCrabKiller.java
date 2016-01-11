package scripts;

import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

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
import org.tribot.api2007.types.RSArea;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSNPCDefinition;
import org.tribot.api2007.types.RSObject;
import org.tribot.api2007.types.RSPlayer;
import org.tribot.api2007.types.RSTile;


@ScriptManifest(authors = {"botsallday"}, category = "Combat", name = "FlawlessRockCrabKiller")

// bank area
// crabs area
// tree safe area



public class FlawlessRockCrabKiller extends Script implements Painting {
	private static final long startTime = System.currentTimeMillis();
    Font font = new Font("Verdana", Font.BOLD, 14);
	
	private Banker banker = new Banker();
	private Transportation transport = new Transportation();
	private AntiBan ab = new AntiBan();
	private Clicking clicker = new Clicking();
	private SkillTracker XP = new SkillTracker();
	
	private RSArea bank_area = new RSArea(new RSTile(2724, 3490, 0), new RSTile(2727, 3493, 0));
	private RSArea refresh_crabs_area = new RSArea(new RSTile(2706, 3654, 0), new RSTile(2712, 3658, 0));
	private RSArea rock_crabs_area = new RSArea(new RSTile(2697, 3711, 0), new RSTile(2719, 3727, 0));
	private RSArea camelot_teleport_area = new RSArea(new RSTile(2754, 3473, 0), new RSTile(2763, 3481, 0));
	private RSArea lumbridge_spawn_area = new RSArea(new RSTile(3217, 3210, 0), new RSTile(3224, 3224, 0));
	private RSArea tunnel_area = new RSArea(new RSTile(2730, 3714, 0), new RSTile(2730, 3710, 0));
	private RSTile tunnel_entrance = new RSTile(2773, 10162);
	private RSArea checkpoint_one = new RSArea(new RSTile(2682, 3545, 0), new RSTile(2688, 3541, 0));
	private RSArea checkpoint_two = new RSArea(new RSTile(2663, 3624, 0), new RSTile(2669, 3625, 0));
	
	private boolean use_potions;
	private boolean refreshing;
	private boolean execute;
	private State state;
	private long refresh_time = Timing.currentTimeMillis() + refreshRandom();
	private int failed_wakes = 0;
	private String food;
	private String tab = "Camelot teleport";
	private String potion = "Combat potion";
	private CrabGUI gui = new CrabGUI();
	
	private Condition in_teleport_area = new Condition() {
        @Override
        public boolean active() {
            // control cpu usage
            General.sleep(100);
            // ensure we have deposited items
            return camelot_teleport_area.contains(Player.getPosition());
        }
	};
	
	private Condition attackable_crab = new Condition() {
        @Override
        public boolean active() {
        	RSNPC[] crabs = crabs();
            // control cpu usage
            General.sleep(100);
            
            if (crabs.length > 0) {
            	if ((!crabs[0].isInCombat() || crabs[0].isInteractingWithMe()) && (crabs[0].isMoving() || Combat.isUnderAttack())) {
            		return true;
            	}
            }
            
            return false;
        }
	};
	
	private Condition bad_items_deposited = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Inventory.find(Filters.Items.nameNotEquals(food, tab)).length < 2;
        }
	};
	
	private Condition tabs_withdrawn = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Inventory.getCount(tab) > 1;
        }
	};
	
	private Condition food_withdrawn = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Inventory.isFull();
        }
	};
	
	private Condition inside_tunnel = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Player.getPosition().distanceTo(tunnel_entrance.getPosition()) <= 1;
        }
	};
	
	private Condition outside_tunnel = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return tunnel_area.contains(Player.getPosition());
        }
	};
	
	private Condition fighting = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Combat.isUnderAttack() || hasTarget();
        }
	};
	
	private Condition is_logged_in = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Login.getLoginState() != Login.STATE.LOGINSCREEN;
        }
	};
	
	private Condition potion_drank = new Condition() {
        @Override
        public boolean active() {
        	General.sleep(100);
        	return Skills.getActualLevel(Skills.SKILLS.ATTACK) < Skills.getCurrentLevel(Skills.SKILLS.ATTACK);
        }
	};
	
	@Override
	public void run() {
		
		execute = true;
		gui.setVisible(true);
		while (gui.isVisible()) {
			sleep(100);
			println("Waiting for GUI");
		}
		setGuiVars();
		ab.setHoverSkill(Skills.SKILLS.HITPOINTS);
		// there are null objects that may cause a ban if examined in these locations
		ab.ignore_examine = true;
		
		while (true) {
			sleep(50);
			state = state();
			
			switch (state) {
				case LOGIN:
					Timing.waitCondition(is_logged_in, 10000);
					break;
				case TELEPORT:
					if (teleport()) {
						ab.handleItemInteractionDelay();
	                    Timing.waitCondition(in_teleport_area, General.random(8000, 12000));
					}
					break;
				case WALK_TO_BANK:
					if (!banker.handleBanking(bank_area, true)) {
						transport.walkCustomNavPath(bank_area.getRandomTile());
					}
					break;
				case WALK_TO_ROCKS:
					walkToRocks();
					break;
				case WAITING:
					ab.handleWait();
					break;
				case WALKING:
					ab.handleWait();
					break;
				case WITHDRAW_TELEPORT:
					if (!banker.withdrawItem(tab, 1)) {
						if (!banker.hasItemInBank(tab)) {
							execute = false;
						}
					};
					ab.handleItemInteractionDelay();
                    Timing.waitCondition(tabs_withdrawn, General.random(3000, 5000));
					break;
				case WITHDRAW_FOOD:
					if (!banker.withdrawItem(food, 28)) {
						if (!banker.hasItemInBank(food)) {
							execute = false;
						}
					};
					ab.handleItemInteractionDelay();
                    Timing.waitCondition(food_withdrawn, General.random(3000, 5000));
					break;
				case WITHDRAW_POTIONS:
					if (!banker.withdrawItem(potion, 1)) {
						use_potions = false;
					};
					ab.handleItemInteractionDelay();
					break;
				case REFRESH_CRABS:
					transport.checkRun();
					println("Refreshing crabs");
					// refresh using the tunnel or forest
					if (refreshTunnel()) {
						println("Refreshing random");
						resetRefreshTime();
					} else if (refreshForest()) {
						resetRefreshTime();
					}
					break;
				case WAKE_CRAB:
					transport.checkRun();
					ab.handleNewObjectCombatDelay();
					attemptToWakeCrab();
					break;
				case COMBAT:
					eat();
					if (use_potions) {
						checkPotion();
					}
					break;
				case SOMETHING_WENT_WRONG:
					execute = false;
					break;
				case DEAD:
					println("You died!");
					break;
				case DEPOSIT_ITEMS:
					if (depositBadItems()) {
	                    Timing.waitCondition(bad_items_deposited, General.random(8000, 12000));
					};
					break;
				case CANNON_DETECTED:
					println("Trying to hop worlds because of cannon");
					hop();
					break;
				case TOO_MANY_PLAYERS:
					hop();
					break;
				case NAVIGATE_PATH:
					if (navigatePath()) {
						println("Navigated path");
					}
					break;
				case TURN_ON_AUTO:
					turnAutoRetaliateOn();
					break;
			}
		}
	}
	
  private State state() {

	  // handle login state
	  if (Login.getLoginState() == Login.STATE.LOGINSCREEN) {
		  return State.LOGIN;
	  }
	  // handle death
	  if (lumbridge_spawn_area.contains(Player.getPosition())) {
		  execute = false;
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
		  if (detectCannon() && rock_crabs_area.contains(Player.getPosition())) {
			  return State.CANNON_DETECTED;
		  }
		  // when out of food we must try to bank
		  if (outOfFood() && !Banking.isInBank()) {
			  // only teleport if needed
			  if (banker.distanceToBank(bank_area.getRandomTile(), Player.getPosition()) > 50) {
				  return State.TELEPORT;
			  }
			  return State.WALK_TO_BANK;
		  }
		  // walk to bank if we have teleported
		  if (camelot_teleport_area.contains(Player.getPosition())) {
			  return State.WALK_TO_BANK;
		  }
		  // handle getting supplies and leaving bank
		  if (Banking.isInBank()) {
			  // deposit items that we don't need to take with us
			  if (inventoryContainsBadItems()) {
				  println("Has bad items");
				  return State.DEPOSIT_ITEMS;
			  }
			  // see if we need to withdraw supplies
			  if (needsAnySupplies()) {
				  if (needsTeleport()) {
					  return State.WITHDRAW_TELEPORT;
				  }
				  if (use_potions && needsPotions()) {
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
		  if (!atCrabs() && !refreshing && !Banking.isInBank()) {
			  return State.WALK_TO_ROCKS;
		  }
		  // handle refreshing crabs
		  if ((Timing.currentTimeMillis() >= refresh_time) || refreshing || failed_wakes > 8) {
			  refreshing = true;
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
	  
   enum State {
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
   
   private boolean navigatePath() {
	   // first attempt to use our refresh methods to get there
		if (refreshForest()) {
			return true;
		}
		// if for some reason they fail, use custom checkpoints
		if (Walking.walkTo(checkpoint_one.getRandomTile())) {
			if (Walking.walkTo(checkpoint_two.getRandomTile())) {
				return true;
			}
		}
		
		return false;
   }
   
   private boolean refreshForest() {
	   // use the forest to refresh crabs
		if (WebWalking.walkTo(refresh_crabs_area.getRandomTile())) {
			println("Web walked to forest");
			refreshing = false;
			return true;
		} else {
			println("Custom pathing to forest");
			// walk with a custom path
			if (Walking.walkPath(transport.generateRandomStraightPath(refresh_crabs_area.getRandomTile(), 1, 1))) {
				refreshing = false;
				return true;
			};
		}
		
		return false;
   }
   
   private boolean refreshTunnel() {
	   // attempt to refresh using the tunnel
		if (walkToTunnel()) {
			println("Walked to tunnel");
            Timing.waitCondition(outside_tunnel, General.random(8000, 12000));
            // enter tunnel
			if (enterTunnel()) {
				println("Entered tunnel");
				Timing.waitCondition(inside_tunnel, General.random(8000, 12000));
				// exit tunnel
				if (exitTunnel()) {
					println("Exited tunnel");
					refreshing = false;
					Timing.waitCondition(outside_tunnel, General.random(8000, 12000));
					return true;
				}
			}
		}
		
		return false;
   }
   
   private void walkToRocks() {
	   // attempt to walk to the crab area with a randomized path to avoid bans
		if (Walking.walkPath(transport.generateRandomStraightPath(rock_crabs_area.getRandomTile(), 3, 1))) {
			println("Walked custom randomized path");
		} else {
			println("Failed to walk custom randomized path");
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
						   return ab.handleUseClosest(crabs[i], crabs[i+1]);
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
		   transport.checkRun();
		   // cache the crab so we can see if it woke for us
		   RSNPC crab = findAttackableCrab(crabs);
		   // switch between screen and minimap clicks
		   switch (General.random(1, 2)) {
		   	   case 1:
		   		   println("Attempting to minimap click to wake crab");
		   		   // use a function for a chance at getting a random adjacent tile to the crab
				   if (transport.normalWalk(getAdjacent(crab.getPosition()))) {
					   // wait for the attack
		               Timing.waitCondition(attackable_crab, General.random(10000, 12000));
		               println("Attempting to attack crab after minimap wake");
				   };
				   break;
		   	   case 2:
		   		   println("Attempting to screen click to wake crab");
		   		   // use a function for a chance at getting a random adjacent tile to the crab
				   if (Walking.clickTileMS(getAdjacent(crab.getPosition()), "Walk here")) {
		               Timing.waitCondition(attackable_crab, General.random(6000, 8000));
			   		   println("Attempting to awake crab after screen click wake");
				   };
		   
		   }
		   // ensure we are waking crabs up, if not we will refresh
		   if (clicker.actionsContainOption(crab.getActions(), "Attack")) {
			   println("Woke crab successfully!");
			   // we woke the crab!
			   failed_wakes = 0;
		   } else {
			   println("Failed to wake crab");
			   // we must have failed to wake the crab
			   failed_wakes++;
		   }
	   }
   }
   
   private boolean attackCrab(RSNPC crab) {
	   if (!crab.isInCombat() || crab.isInteractingWithMe()) {
		   // handle wait between switching attack targets
		   ab.handleSwitchObjectCombatDelay();
		   return crab.click("Attack");
	   }
	   
	   return false;
   }
   
   private RSNPC[] crabs() {
	   return NPCs.findNearest(Filters.NPCs.nameContains("Rock").combine(Filters.NPCs.inArea(rock_crabs_area), true).combine(Filters.NPCs.nameNotContains("olem"), true));
   }
   
   private boolean atCrabs() {
	   return rock_crabs_area.contains(Player.getPosition());
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
	   return Inventory.find(Filters.Items.nameNotEquals(food, tab).combine(Filters.Items.nameNotContains(potion), true));
   }
   
   private boolean needsTeleport() {
	   println(Inventory.getCount(tab) < 1);
	   return Inventory.getCount(tab) < 1;
   }
   
   private boolean outOfFood() {
	   return Inventory.find(Filters.Items.actionsContains("Eat")).length < 1;
   }
   
   private boolean needsPotions() {
	   return Inventory.find(Filters.Items.nameContains(potion)).length < 1;
   }
   
   private boolean teleport() {
	   // find tabs
	   RSItem[] teleport = Inventory.find(Filters.Items.nameEquals(tab));
	   // null check
	   if (teleport.length > 0) {
		   // hover tab
		   if (teleport[0].hover()) {
			   // click tab
			   if (teleport[0].click("Teleport")) {
				   return true;
			   }
		   }
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
				if (rock_crabs_area.contains(cannon[i].getPosition())) {
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
		food = gui.getFoodName();
		use_potions = gui.usePotions();
	}
	
	@SuppressWarnings("deprecation")
	public boolean eat() {
		// eat based on abc utils
		if (healthPercent() < ab.abc.INT_TRACKER.NEXT_EAT_AT.next()) {
			if (eatFood()) {
				println("Ate food");
				ab.abc.INT_TRACKER.NEXT_EAT_AT.reset();
				return true;
			};
		}
		
		return false;
	}
	
	public boolean eatFood() {
	   println("Eating food");
	   RSItem[] foods = Inventory.find(food);
	   // null check
	   if (foods.length > 0) {
			// hover the food
			if (foods[0].hover()) {
				// click the food
				if (foods[0].click("Eat")) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean walkToTunnel() {
		return Walking.walkPath(transport.generateRandomStraightPath(tunnel_area.getRandomTile(), 1, 1));
	}
	
	public boolean exitTunnel() {
		// find tunnel
		RSObject[] tunnel = Objects.find(10, Filters.Objects.actionsContains("Enter").combine(Filters.Objects.nameContains("Tunnel"), true));
		// null check
		if (tunnel.length > 0) {
			// try to enter the tunnel (from the inside, so exit)
			if (DynamicClicking.clickRSObject(tunnel[0], "Enter")) {
				// wait until we are outside
				Timing.waitCondition(outside_tunnel, General.random(10000, 12000));
				return true;
			}
		}
		
		return false;
	}
	
   public boolean actionsContainOption(String[] actions, String key) {
	   for (int i = 0; i < actions.length; i++) {
		   println(actions[i]);
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
				Timing.waitCondition(inside_tunnel, General.random(10000, 12000));
				return true;
			}
		}
		
		return false;
	}
	
	public boolean skillIsAtBase(Skills.SKILLS skill) {
		// compare skill levels to actual level to see if we are boosted
		if (Skills.getCurrentLevel(skill) == Skills.getActualLevel(skill)) {
			return true;
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
		RSItem[] pot = Inventory.find(Filters.Items.nameContains(potion));
		// null check
		if (pot.length > 0) {
			// try to drink the potion
			if (pot[0].click("Drink")) {
				// wait condition
				Timing.waitCondition(potion_drank, 3000);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean shouldUsePotion() {
		// determine if we should drink a potion
		if (skillIsAtBase(Skills.SKILLS.ATTACK) || skillIsAtBase(Skills.SKILLS.STRENGTH)) {
			return true;
		}
		
		return false;
	}
	
	public boolean detectPlayers() {
		// return a bool to determine if we should hop based on the number of players
		if (countPlayers() > 6) {
			return true;
		}
		
		return false;
	}
	
	public int countPlayers() {
		return players().length;
	}
	
	public RSPlayer[] players() {
		return Players.getAll(Filters.Players.inArea(rock_crabs_area));
	}
	
	private boolean hop() {
		// use the lovely FC game hopper
		if (FCInGameHopper.hop(WorldHopper.getRandomWorld(true, false))) {;
			println("Hopped successfully!");
			return true;
		}
		
		return false;
	}
	
	private void resetRefreshTime() {
		refresh_time = Timing.currentTimeMillis() + refreshRandom();
	}
	
	@Override
	public void onPaint(Graphics g) {
		
       // set variables for display
       long run_time = System.currentTimeMillis() - startTime;
//       long xp_hr = 
       long time_by_hour = 3600000/run_time;
       long xp_by_hour = XP.getTotalGainedXp()*time_by_hour;
       g.setFont(font);
       g.setColor(new Color(0, 0, 0));

       g.drawString("Run Time: " + Timing.msToString(run_time), 40, 360);
       g.drawString("Script State: " + state, 220, 360);
       g.drawString("Experience gained: "+XP.getTotalGainedXp()+" ("+xp_by_hour+")", 40, 340);
       g.drawString("Crabs Killed: "+(XP.getTotalGainedXp()/250)+" ("+(xp_by_hour/250)+")", 310, 340);
	}
	
}
