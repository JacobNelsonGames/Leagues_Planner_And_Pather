package Posiedien_Leagues_Planner;

import java.lang.reflect.Array;
import java.util.*;

public class FullUserData
{
    HashMap<UUID, Integer> PlannedTasks = new HashMap<UUID, Integer>();

    ArrayList<SortedTask> SortedPlannedTasks = new ArrayList<>();

    void CacheSortedPlannedTasks()
    {
        SortedPlannedTasks.clear();

        for (HashMap.Entry<UUID, Integer> mapElement : PlannedTasks.entrySet())
        {
            SortedPlannedTasks.add(new SortedTask(mapElement.getKey(), mapElement.getValue()));
        }

        SortedPlannedTasks.sort(new Comparator<SortedTask>() {
            @Override
            public int compare(SortedTask o1, SortedTask o2)
            {
                return (o1.SortPriority.compareTo(o2.SortPriority));
            }
        });
    }
}
