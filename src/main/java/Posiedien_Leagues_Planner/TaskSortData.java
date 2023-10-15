package Posiedien_Leagues_Planner;

public class TaskSortData
{
    Integer SortPriority;
    Integer LastSortOrder = 0;
    Boolean bIsCustomTask = false;

    public TaskSortData(int i, Boolean bCustomTask)
    {
        SortPriority = i;
        bIsCustomTask = bCustomTask;
    }

    public String ExportData()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append(SortPriority);
        Converted.append(",");

        Converted.append(LastSortOrder);
        Converted.append(",");

        Converted.append(bIsCustomTask);
        Converted.append(",");

        return Converted.toString();
    }
}
