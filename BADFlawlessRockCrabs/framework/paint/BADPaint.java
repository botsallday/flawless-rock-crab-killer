package scripts.BADFlawlessRockCrabs.framework.paint;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import org.tribot.api.Timing;
import scripts.BADFlawlessRockCrabs.api.skills.BADSkillTracker;

public class BADPaint {
    Font font = new Font("Verdana", Font.BOLD, 14);
    private static final long startTime = System.currentTimeMillis();
    private BADSkillTracker XP = new BADSkillTracker();
    
	public void paint(Graphics g, String state) {
			
	       // set variables for display
	       long run_time = System.currentTimeMillis() - startTime;
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
