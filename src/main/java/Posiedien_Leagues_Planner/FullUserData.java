package Posiedien_Leagues_Planner;

import ch.qos.logback.core.BasicStatusManager;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;

public class FullUserData
{
    HashMap<UUID, TaskData>  CustomTasks = new HashMap<>();
    HashMap<UUID, TaskSortData> PlannedTasks = new HashMap<UUID, TaskSortData>();

    HashSet<UUID> HiddenTasks = new HashSet<UUID>();

    ArrayList<SortedTask> SortedPlannedTasks = new ArrayList<>();

    void CacheSortedPlannedTasks()
    {
        SortedPlannedTasks.clear();

        for (HashMap.Entry<UUID, TaskSortData> mapElement : PlannedTasks.entrySet())
        {
            SortedPlannedTasks.add(new SortedTask(mapElement.getKey(), mapElement.getValue().SortPriority, mapElement.getValue().bIsCustomTask));
        }

        SortedPlannedTasks.sort(new Comparator<SortedTask>() {
            @Override
            public int compare(SortedTask o1, SortedTask o2)
            {
                return (o1.SortPriority.compareTo(o2.SortPriority));
            }
        });

        for (int i = 0; i < SortedPlannedTasks.size(); ++i)
        {
            PlannedTasks.get(SortedPlannedTasks.get(i).TaskGUID).LastSortOrder = i + 1;
        }


    }

    public void importFrom(File targ)
    {
    }
}
