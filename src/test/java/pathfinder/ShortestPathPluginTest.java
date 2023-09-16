package pathfinder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import Posiedien_Leagues_Planner.PosiedienLeaguesPlannerPlugin;

public class ShortestPathPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(PosiedienLeaguesPlannerPlugin.class);
        RuneLite.main(args);
    }
}
