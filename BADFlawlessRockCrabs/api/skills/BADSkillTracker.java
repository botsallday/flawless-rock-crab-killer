package scripts.BADFlawlessRockCrabs.api.skills;

import org.tribot.api2007.Skills;

public class BADSkillTracker {
		
		private long attack;
		private long strength;
		private long defence;
		private long hitpoints;
		private long range;
		private Skills.SKILLS[] skills = {Skills.SKILLS.ATTACK, Skills.SKILLS.STRENGTH, Skills.SKILLS.HITPOINTS, Skills.SKILLS.DEFENCE, Skills.SKILLS.RANGED};


		public BADSkillTracker() {
			attack = Skills.getXP(Skills.SKILLS.ATTACK);
			strength = Skills.getXP(Skills.SKILLS.STRENGTH);
			defence = Skills.getXP(Skills.SKILLS.DEFENCE);
			hitpoints = Skills.getXP(Skills.SKILLS.HITPOINTS);
			range = Skills.getXP(Skills.SKILLS.RANGED);
		}
		
		public long attackXP() {
			return attack;
		}
		
		public long strengthXP() {
			return strength;
		}
		
		public long defenceXP() {
			return defence;
		}
		
		public long hitpointsXP() {
			return hitpoints;
		}
		
		public long rangeXP() {
			return range;
		}
		
		public long get(Skills.SKILLS skill) {
			return Skills.getXP(skill);
		}
		
		public long gained(Skills.SKILLS skill, long xp) {
			return get(skill) - xp;
		}
		
		public long level(Skills.SKILLS skill) {
			return Skills.getXPToNextLevel(skill);
		}
		
		public long getTotalGainedXp() {
			long total = 0;
			
			for (int i=0; i < skills.length; i++) {
//				total += gained(skills[i], ));
				total += get(skills[i]);
			}
			
			total = (total-attack-range-hitpoints-strength-defence);
			
			return total;
		}
		
		public boolean skillIsAtBase(Skills.SKILLS skill) {
			// compare skill levels to actual level to see if we are boosted
			if (Skills.getCurrentLevel(skill) == Skills.getActualLevel(skill)) {
				return true;
			}
			
			return false;
		}
		
		
}
