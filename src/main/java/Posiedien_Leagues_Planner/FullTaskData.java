package Posiedien_Leagues_Planner;

import net.runelite.api.coords.WorldPoint;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;

public class FullTaskData
{

    ArrayList<SortedTask> SortedLeaguesTaskList = new ArrayList<SortedTask>();

    HashMap<UUID, TaskData> LeaguesTaskList = new HashMap<UUID, TaskData>();

    public void CalculateAndCacheOverworldLocations(PosiedienLeaguesPlannerPlugin posiedienLeaguesPlannerPlugin)
    {
        // Go through all of our points on our tasks
        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            for (WorldPoint NextLocation : entry.getValue().Locations)
            {
                if (!IsOverworldLocation(NextLocation))
                {
                    posiedienLeaguesPlannerPlugin.restartPathfinding(NextLocation, new WorldPoint(3000,3000,0), true);
                    while (posiedienLeaguesPlannerPlugin.getPathfinder() == null || !posiedienLeaguesPlannerPlugin.getPathfinder().isDone())
                    {
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        }
                    }
                    entry.getValue().OverworldLocations.add(posiedienLeaguesPlannerPlugin.getPathfinder().getPath().get(posiedienLeaguesPlannerPlugin.getPathfinder().getPath().size() - 1));
                }
            }
        }
    }

    public boolean IsOverworldLocation(WorldPoint testPoint)
    {
        // Just hard code the overworld bounds
        if (testPoint.getPlane() == 0)
        {
            if (testPoint.getX() > 1022 && testPoint.getX() < 3968)
            {
                return testPoint.getY() > 2494 && testPoint.getY() < 4160;
            }
        }

        return false;
    }

    public void importFromConverted(File targ)
    {
    }

    public void importFromRaw()
    {
        File targ = new File("TaskData.csv");
    }
}
