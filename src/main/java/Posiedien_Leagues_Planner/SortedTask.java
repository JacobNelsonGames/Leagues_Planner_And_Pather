package Posiedien_Leagues_Planner;

import java.util.UUID;

public class SortedTask
{
    UUID TaskGUID;
    Integer SortPriority;

    Boolean bIsCustomTask;

    public SortedTask(UUID key, Integer value, Boolean bCustomTask)
    {
        TaskGUID = key;
        SortPriority = value;
        bIsCustomTask = bCustomTask;
    }
}
