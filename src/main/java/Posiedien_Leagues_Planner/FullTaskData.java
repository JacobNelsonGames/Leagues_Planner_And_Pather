package Posiedien_Leagues_Planner;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FullTaskData
{

    HashMap<UUID, TaskData> LeaguesTaskList = new HashMap<UUID, TaskData>();

    public void CalculateAndCacheOverworldLocations()
    {
        // Go through all of our points on our tasks
        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            for (WorldPoint NextLocation : entry.getValue().Locations)
            {
                if (!IsOverworldLocation(NextLocation))
                {

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
                if (testPoint.getY() > 2494 && testPoint.getY() < 4160)

                {
                    return true;
                }
            }
        }

        return false;
    }
}
