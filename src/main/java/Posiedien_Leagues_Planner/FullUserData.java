package Posiedien_Leagues_Planner;

import ch.qos.logback.core.BasicStatusManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class FullUserData
{
    HashMap<UUID, TaskData>  CustomTasks = new HashMap<>();
    HashMap<UUID, TaskSortData> PlannedTasks = new HashMap<UUID, TaskSortData>();

    HashSet<UUID> HiddenTasks = new HashSet<UUID>();

    HashSet<UUID> CompletedTasks = new HashSet<>();

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


    public void exportTo(File targ)
    {
        try (FileWriter fw = new FileWriter(targ))
        {
            StringBuilder Converted = new StringBuilder();

            Converted.append(CustomTasks.size());
            Converted.append(",");
            for (HashMap.Entry<UUID, TaskData> entry : CustomTasks.entrySet())
            {
                Converted.append(entry.getKey());
                Converted.append(",");

                Converted.append(entry.getValue().ExportUserDataFormat());
            }

            Converted.append(PlannedTasks.size());
            Converted.append(",");
            for (HashMap.Entry<UUID, TaskSortData> entry : PlannedTasks.entrySet())
            {
                Converted.append(entry.getKey());
                Converted.append(",");

                Converted.append(entry.getValue().ExportData());
            }


            Converted.append(HiddenTasks.size());
            Converted.append(",");
            for (UUID entry : HiddenTasks)
            {
                Converted.append(entry);
                Converted.append(",");
            }

            Converted.append(CompletedTasks.size());
            Converted.append(",");
            for (UUID entry : CompletedTasks)
            {
                Converted.append(entry);
                Converted.append(",");
            }

            fw.write(Converted.toString());
        }
        catch (IOException e)
        {
        }
    }
}
