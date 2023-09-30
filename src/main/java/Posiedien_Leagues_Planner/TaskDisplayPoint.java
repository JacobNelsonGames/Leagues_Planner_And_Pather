package Posiedien_Leagues_Planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.tuple.Pair;

public class TaskDisplayPoint
{
    UUID DisplayPointGUID;

    // Boolean whether or not the task is a custom task
    HashMap<UUID, Boolean> Tasks = new HashMap<>();
    HashMap<UUID, Boolean> DungeonTasks = new HashMap<>();

    ArrayList<WorldPoint> TaskWorldPoints = new ArrayList<>();

    WorldPoint MapDisplayPoint;

    // An average map display point
    void UpdateMapDisplayPoint()
    {
        int averageX = 0;
        int averageY = 0;
        int averageZ = 0;

        for (WorldPoint TaskPoint : TaskWorldPoints)
        {
            averageX += TaskPoint.getX();
            averageY += TaskPoint.getY();
            averageZ += TaskPoint.getPlane();
        }
        averageX /= TaskWorldPoints.size();
        averageY /= TaskWorldPoints.size();
        averageZ /= TaskWorldPoints.size();

        MapDisplayPoint = new WorldPoint(averageX, averageY, averageZ);
    }
}
