package Posiedien_Leagues_Planner;

import java.util.UUID;

public class SortedTask
{
    UUID TaskGUID;
    Integer SortPriority;

    public SortedTask(UUID key, Integer value)
    {
        TaskGUID = key;
        SortPriority = value;
    }
}
