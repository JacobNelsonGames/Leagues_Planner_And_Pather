package Posiedien_Leagues_Planner;

public class TaskSortData
{
    Integer SortPriority;
    int LastSortOrder = 0;
    Boolean bIsCustomTask = false;

    public TaskSortData(int i, Boolean bCustomTask)
    {
        SortPriority = i;
        bIsCustomTask = bCustomTask;
    }
}
