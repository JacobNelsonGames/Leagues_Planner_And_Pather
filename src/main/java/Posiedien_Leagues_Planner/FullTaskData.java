package Posiedien_Leagues_Planner;

import net.runelite.api.Skill;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.CombatLevelRequirement;
import net.runelite.client.plugins.achievementdiary.SkillRequirement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class FullTaskData
{

    ArrayList<SortedTask> SortedLeaguesTaskList = new ArrayList<SortedTask>();

    HashMap<UUID, TaskData> LeaguesTaskList = new HashMap<UUID, TaskData>();
    HashMap<String, UUID> StringToTask = new HashMap<String, UUID>();

    void AddNewTaskToDataBase(TaskData newTask)
    {
        if (newTask.GUID == null)
        {
            newTask.GUID = UUID.randomUUID();
        }
        LeaguesTaskList.put(newTask.GUID, newTask);
        StringToTask.put(newTask.TaskName, newTask.GUID);
    }

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
                    while (posiedienLeaguesPlannerPlugin.getPathfinder() == null ||
                            !posiedienLeaguesPlannerPlugin.getPathfinder().isDone() ||
                            posiedienLeaguesPlannerPlugin.bQueuedPathfinderTask)
                    {
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        }
                    }
                    WorldPoint LastLocation = posiedienLeaguesPlannerPlugin.getPathfinder().getPath().get(posiedienLeaguesPlannerPlugin.getPathfinder().getPath().size() - 1);

                    if (IsOverworldLocation(LastLocation) && !entry.getValue().OverworldLocations.contains(LastLocation))
                    {
                        entry.getValue().OverworldLocations.add(LastLocation);
                    }
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
    public String GetNextIgnoreBlank(Scanner sc)
    {
        while (true)
        {
            if (!sc.hasNext())
            {
                return null;
            }

            String NextString = sc.next();
            if (!Objects.equals(NextString, "")
                    && !Objects.equals(NextString, "\r\n"))
            {
                NextString = NextString.replace("\r\n", "");
                return NextString;
            }
        }

    }

    public void importFromConverted(File file)
    {
        if (file.exists())
        {
            try (Scanner sc = new Scanner(file))
            {
                // CSV file, so comma delimiter
                sc.useDelimiter(",");

                GetNextIgnoreBlank(sc); // "Task Count:"
                GetNextIgnoreBlank(sc); // #

                // Go through every task in the csv
                while(sc.hasNext())
                {
                    String TaskName = GetNextIgnoreBlank(sc);
                    if (TaskName == null)
                    {
                        break;
                    }

                    // Task already exists, so lets work off that one
                    TaskData newTask = null;
                    boolean bIsNewTask = false;
                    if (StringToTask.containsKey(TaskName))
                    {
                        newTask = LeaguesTaskList.get(StringToTask.get(TaskName));
                    }
                    else
                    {
                        bIsNewTask = true;
                        newTask = new TaskData();
                    }

                    String DifficultyName = GetNextIgnoreBlank(sc);
                    newTask.TaskName = TaskName;
                    newTask.Difficulty = TaskDifficulty.valueOf(DifficultyName);

                    newTask.TaskDescription = GetNextIgnoreBlank(sc);

                    if (!bIsNewTask)
                    {
                        GetNextIgnoreBlank(sc); // Throw out guid
                    }
                    else
                    {
                        newTask.GUID = UUID.fromString(GetNextIgnoreBlank(sc));
                    }

                    newTask.bIsCustomTask = Boolean.valueOf(GetNextIgnoreBlank(sc));
                    newTask.CustomIcon = GetNextIgnoreBlank(sc);
                    if (newTask.CustomIcon.contains("null"))
                    {
                        newTask.CustomIcon = null;
                    }

                    GetNextIgnoreBlank(sc); // "Regions Count: "

                    int RegionCount = Integer.parseInt(GetNextIgnoreBlank(sc));
                    for (int i = 0; i < RegionCount; ++i)
                    {
                        newTask.Regions.add(RegionType.valueOf(GetNextIgnoreBlank(sc)));
                    }

                    GetNextIgnoreBlank(sc); // "Overworld Location Count (Auto-generated): ,"
                    int OverworldPointCount = Integer.parseInt(GetNextIgnoreBlank(sc));
                    for (int i = 0; i < OverworldPointCount; ++i)
                    {
                        WorldPoint newWorldPoint = new WorldPoint(
                                Integer.parseInt(GetNextIgnoreBlank(sc)),
                                Integer.parseInt(GetNextIgnoreBlank(sc)),
                                Integer.parseInt(GetNextIgnoreBlank(sc)));

                        if (!newTask.OverworldLocations.contains(newWorldPoint))
                        {
                            newTask.OverworldLocations.add(newWorldPoint);
                        }
                    }

                    GetNextIgnoreBlank(sc); // "POSITIONS_START"
                    GetNextIgnoreBlank(sc); // "NO_POSITIONS"
                    GetNextIgnoreBlank(sc); // "X"
                    GetNextIgnoreBlank(sc); // "Y"
                    GetNextIgnoreBlank(sc); // "Z"

                    int IterationNum = 0;
                    int CurrentX = 0;
                    int CurrentY = 0;
                    int CurrentZ = 0;

                    String NextString = GetNextIgnoreBlank(sc);
                    while (!NextString.contains("POSITIONS_END"))
                    {
                        switch (IterationNum)
                        {
                            case 0: // X
                                CurrentX = Integer.parseInt(NextString);
                                break;
                            case 1: // Y
                                CurrentY = Integer.parseInt(NextString);
                                break;
                            case 2: // Z
                                CurrentZ = Integer.parseInt(NextString);
                                break;
                        }

                        NextString = GetNextIgnoreBlank(sc);

                        ++IterationNum;
                        // Add a new coord
                        if (IterationNum > 2)
                        {
                            IterationNum = 0;

                            WorldPoint newWorldPoint = new WorldPoint(
                                    CurrentX,
                                    CurrentY,
                                    CurrentZ
                            );

                            if (!newTask.Locations.contains(newWorldPoint))
                            {
                                newTask.Locations.add(newWorldPoint);
                            }
                        }
                    }

                    GetNextIgnoreBlank(sc); // REQUIREMENTS_START
                    GetNextIgnoreBlank(sc); // SKILL NAME
                    GetNextIgnoreBlank(sc); // LEVEL REQ
                    NextString = GetNextIgnoreBlank(sc);
                    IterationNum = 0;
                    String CurrentSkill = null;
                    int SkillReqLevel = 0;

                    while (!NextString.contains("REQUIREMENTS_END"))
                    {
                        switch (IterationNum)
                        {
                            case 0:
                                CurrentSkill = NextString;
                                break;
                            case 1:
                                SkillReqLevel = Integer.parseInt(NextString);
                                break;
                        }

                        NextString = GetNextIgnoreBlank(sc);

                        ++IterationNum;

                        // Add a new requirement
                        if (IterationNum > 1)
                        {
                            IterationNum = 0;

                            if (CurrentSkill.contains("COMBAT"))
                            {
                                newTask.Requirements.add(new CombatLevelRequirement(SkillReqLevel));
                            }
                            else
                            {
                                Skill AddedSkill = Skill.valueOf(CurrentSkill);
                                newTask.Requirements.add(new SkillRequirement(AddedSkill, SkillReqLevel));
                            }
                        }
                    }

                    if (bIsNewTask)
                    {
                        AddNewTaskToDataBase(newTask);
                    }
                }

            }
            catch (IOException ignored)
            {
            }
        }

        // Construct String to UUID lookup
        StringToTask.clear();
        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            StringToTask.put(entry.getValue().TaskName, entry.getKey());
        }
    }

    public String ExportData(PosiedienLeaguesPlannerPlugin plugin)
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append("Task Count: ,");
        Converted.append(LeaguesTaskList.size());
        Converted.append(",");
        Converted.append("\r\n");

        SortedLeaguesTaskList.clear();
        plugin.panel.AddTaskListToSortedLeaguesTasks(null, LeaguesTaskList);

        SortedLeaguesTaskList.sort(new Comparator<SortedTask>() {
            @Override
            public int compare(SortedTask o1, SortedTask o2)
            {
                return (o1.SortPriority.compareTo(o2.SortPriority));
            }
        });


        for (SortedTask SortedTaskIter : SortedLeaguesTaskList)
        {
            TaskData data = plugin.GetTaskData(SortedTaskIter.TaskGUID, SortedTaskIter.bIsCustomTask);

            Converted.append(data.ExportData());
            Converted.append(",");
            Converted.append("\r\n");
        }

        return Converted.toString();
    }

    public void exportToConverted(File file, PosiedienLeaguesPlannerPlugin plugin) throws IOException
    {
        try (FileWriter fw = new FileWriter(file))
        {
            fw.write(ExportData(plugin));
        }
    }

    public void importFromRaw()
    {
        // All the tasks found in our raw
        HashSet<UUID> TaskGUIDSet = new HashSet<>();

        // Go through each region
        for (RegionType CurrentRegion : RegionType.values())
        {
            // We open a file for each of our regions
            File file = new File("RawWikiTrailblazer2TaskData/" + CurrentRegion.toString() + ".txt");
            if (file.exists())
            {
                try (Scanner sc = new Scanner(file))
                {
                    TaskDifficulty CurrentDifficulty = null;

                    // New line delimiter
                    sc.useDelimiter("\n");

                    while(sc.hasNext())
                    {
                        // Find next difficulty
                        while (CurrentDifficulty == null)
                        {
                            if (!sc.hasNext())
                            {
                                break;
                            }

                            String NextString = sc.next();

                            if (NextString.contains("==="))
                            {
                                NextString = NextString.replaceAll("===", "");
                                NextString = NextString.replaceAll("\r", "");
                                NextString = NextString.toUpperCase();
                                CurrentDifficulty = TaskDifficulty.valueOf(NextString);
                            }
                        }

                        // Difficulty found, lets skip until we reach our first task "LeagueTaskRow"
                        TaskData newTask = null;
                        while (CurrentDifficulty != null && newTask == null)
                        {
                            if (!sc.hasNext())
                            {
                                break;
                            }
                            String NextString = sc.next();
                            if (NextString.contains("LeagueTaskRow"))
                            {
                                // Remove the league task row start string
                                NextString = NextString.replaceAll("\\{\\{LeagueTaskRow\\|", "");
                                NextString = NextString.replaceAll("\\{\\{sic}}", "");

                                // Remove wiki links
                                while (NextString.contains("[["))
                                {
                                    int StartSubstring = NextString.indexOf("[[");
                                    int EndSubstring = NextString.indexOf("]]");

                                    String OriginalSubString = NextString.substring(StartSubstring, EndSubstring + 2);

                                    // If there is a pipe, that means we have multiple options, just choose the first
                                    if (OriginalSubString.contains("|"))
                                    {
                                        EndSubstring = OriginalSubString.indexOf("|");
                                    }
                                    else
                                    {
                                        EndSubstring = OriginalSubString.length();
                                    }

                                    String ModifiedSubString = OriginalSubString.substring(2, EndSubstring - 2);
                                    NextString = NextString.replace(OriginalSubString, ModifiedSubString);
                                }

                                String TaskName = NextString.substring(0, NextString.indexOf("|"));
                                NextString = NextString.substring(NextString.indexOf("|") + 1);

                                // No commas aloud
                                TaskName = TaskName.replace(",", "");

                                // Task already exists, keep the cached one instead and skip
                                boolean bIsNewTask = false;
                                if (StringToTask.containsKey(TaskName))
                                {
                                    newTask = LeaguesTaskList.get(StringToTask.get(TaskName));
                                }
                                else
                                {
                                    bIsNewTask = true;
                                    newTask = new TaskData();
                                }

                                newTask.Difficulty = CurrentDifficulty;
                                if (bIsNewTask)
                                {
                                    newTask.TaskName = TaskName;

                                    newTask.TaskDescription = NextString.substring(0, NextString.indexOf("|"));
                                    newTask.TaskDescription = newTask.TaskDescription.replace(",", "");
                                }
                                NextString = NextString.substring(NextString.indexOf("|") + 1);

                                // Skill requirements are next
                                String SkillRequirementString = "";
                                if (NextString.contains("|"))
                                {
                                    SkillRequirementString = NextString.substring(0, NextString.indexOf("|"));
                                    NextString = NextString.substring(NextString.indexOf("|") + 1);
                                }

                                while (SkillRequirementString.contains("{{"))
                                {
                                    // Probably a quest or other requirement
                                    if (!NextString.contains("|"))
                                    {
                                        break;
                                    }
                                    SkillRequirementString =  NextString.substring(0, NextString.indexOf("|"));
                                    NextString = NextString.substring(NextString.indexOf("|") + 1);

                                    if (!NextString.contains("|"))
                                    {
                                        break;
                                    }
                                    String SkillName = SkillRequirementString.toUpperCase();

                                    // Isn't a skill
                                    try
                                    {
                                        Skill.valueOf(SkillName);
                                    } catch (IllegalArgumentException ignored)
                                    {
                                        break;
                                    }

                                    SkillRequirementString = NextString;
                                    if (SkillRequirementString.contains(" "))
                                    {
                                        SkillRequirementString = SkillRequirementString.substring(0, SkillRequirementString.indexOf(" "));
                                    }
                                    if (SkillRequirementString.contains("|"))
                                    {
                                        SkillRequirementString =  SkillRequirementString.substring(0, SkillRequirementString.indexOf("|"));
                                    }

                                    NextString = NextString.substring(NextString.indexOf("|") + 1);

                                    int SkillRequirementLevel = Integer.parseInt(SkillRequirementString.replaceAll("[\\D]", ""));
                                    if (SkillName.equals("COMBAT"))
                                    {
                                        CombatLevelRequirement newCombatReq = new CombatLevelRequirement(SkillRequirementLevel);
                                        newTask.Requirements.add(newCombatReq);
                                    }
                                    else
                                    {
                                        Skill ConvertedSkill = Skill.valueOf(SkillName);
                                        SkillRequirement newSkillReq = new SkillRequirement(ConvertedSkill, SkillRequirementLevel);
                                        newTask.Requirements.add(newSkillReq);
                                    }


                                    if (!NextString.contains("|"))
                                    {
                                        break;
                                    }
                                    SkillRequirementString =  NextString.substring(0, NextString.indexOf("|"));
                                    if (SkillRequirementString.contains("link"))
                                    {
                                        NextString = NextString.substring(NextString.indexOf("|") + 1);
                                    }

                                    if (!NextString.contains("|"))
                                    {
                                        break;
                                    }
                                    SkillRequirementString =  NextString.substring(0, NextString.indexOf("|"));
                                    if (SkillRequirementString.contains("sort"))
                                    {
                                        NextString = NextString.substring(NextString.indexOf("|") + 1);
                                    }
                                }

                                // Other requirements are next, but just don't bother with other reqs right now

                                newTask.Regions.clear();
                                if (CurrentRegion != RegionType.GENERAL)
                                {
                                    newTask.Regions.add(CurrentRegion);
                                }

                                if (bIsNewTask)
                                {
                                    AddNewTaskToDataBase(newTask);
                                }
                                TaskGUIDSet.add(newTask.GUID);
                            }
                            // Finished all the tasks of this difficulty
                            else if (NextString.contains("LeagueTaskBottom"))
                            {
                                CurrentDifficulty = null;
                            }
                        }
                    }

                } catch (IOException ignored)
                {
                }

            }
        }

        {
            // Figure out what tasks to remove (wasn't present in the raw data)
            for (Map.Entry<UUID, TaskData> SearchingTask : LeaguesTaskList.entrySet())
            {
                if (!TaskGUIDSet.contains(SearchingTask.getKey()))
                {
                    SearchingTask.getValue().bIsRemoved = true;
                }
            }
        }

        // Re-construct String to UUID lookup
        StringToTask.clear();
        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            StringToTask.put(entry.getValue().TaskName, entry.getKey());
        }
    }
}
