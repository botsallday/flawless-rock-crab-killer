package scripts.BADFlawlessRockCrabs.flawlessrockcrabkiller;

import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.api.General;
import org.tribot.script.interfaces.Painting;
import org.tribot.script.interfaces.Starting;

import scripts.BADFlawlessRockCrabs.framework.flawlessrockcrabkiller.FlawlessRockCrabKillerCore;
import scripts.BADFlawlessRockCrabs.framework.paint.BADPaint;

import java.awt.Graphics; 

@ScriptManifest(authors = {"botsallday"}, category = "Combat", name = "FlawlessRockCrabKiller")

public class FlawlessRockCrabKiller extends Script implements Painting, Starting {
    
	private BADPaint painter = new BADPaint();
	private FlawlessRockCrabKillerCore core = new FlawlessRockCrabKillerCore();
    
    public void run() {
		// execute the script
		core.run();
    }
   
    public void onPaint(Graphics g) {
    	painter.paint(g, core.state().toString());
    }

	@Override
	public void onStart() {
    	General.useAntiBanCompliance(true);
	}
}