package Posiedien_Leagues_Planner;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.CombatLevelRequirement;
import net.runelite.client.plugins.achievementdiary.SkillRequirement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.ArrayList;

public class FullTaskData
{

    ArrayList<SortedTask> SortedLeaguesTaskList = new ArrayList<SortedTask>();

    HashMap<UUID, TaskData> LeaguesTaskList = new HashMap<UUID, TaskData>();
    HashMap<String, UUID> StringToTask = new HashMap<String, UUID>();

    void AddNewTaskToDataBase(TaskData newTask)
    {
        newTask.GUID = UUID.randomUUID();
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
                    while (posiedienLeaguesPlannerPlugin.getPathfinder() == null || !posiedienLeaguesPlannerPlugin.getPathfinder().isDone())
                    {
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        }
                    }
                    entry.getValue().OverworldLocations.add(posiedienLeaguesPlannerPlugin.getPathfinder().getPath().get(posiedienLeaguesPlannerPlugin.getPathfinder().getPath().size() - 1));
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

    public void importFromConverted(File file)
    {
        LeaguesTaskList.clear();

        // Construct String to UUID lookup
        StringToTask.clear();
        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            StringToTask.put(entry.getValue().TaskName, entry.getKey());
        }
    }

    public String ExportData()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append("Task Count: ,");
        Converted.append(LeaguesTaskList.size());
        Converted.append(",");
        Converted.append("\n");

        for (HashMap.Entry<UUID, TaskData> entry : LeaguesTaskList.entrySet())
        {
            Converted.append(entry.getValue().ExportData());
            Converted.append("\n");
        }

        return Converted.toString();
    }

    public void exportToConverted(File file) throws IOException
    {
        try (FileWriter fw = new FileWriter(file))
        {
            fw.write(ExportData());
        }
    }

    public void importFromRaw()
    {
        // Go through each region
        for (RegionType CurrentRegion : RegionType.values())
        {
            // We open a file for each of our regions
            File file = new File("RawWikiTaskData/" + CurrentRegion.toString() + ".txt");
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

                                // Task already exists, keep the cached one instead and skip
                                if (StringToTask.containsKey(TaskName))
                                {
                                    continue;
                                }

                                newTask = new TaskData();
                                newTask.Difficulty = CurrentDifficulty;
                                newTask.TaskName = TaskName;

                                newTask.TaskDescription = NextString.substring(0, NextString.indexOf("|"));
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
                                        Skill TestIsSkill = Skill.valueOf(SkillName);
                                    } catch (IllegalArgumentException ignored)
                                    {
                                        break;
                                    }

                                    SkillRequirementString =  NextString.substring(0, NextString.indexOf("|"));
                                    NextString = NextString.substring(NextString.indexOf("|") + 1);

                                    // Could have no link or sort strings
                                    if (SkillRequirementString.contains("}}"))
                                    {
                                        SkillRequirementString = SkillRequirementString.replace("}}", "");
                                    }

                                    if (SkillRequirementString.contains(","))
                                    {
                                        SkillRequirementString = SkillRequirementString.substring(0, SkillRequirementString.indexOf(","));
                                    }

                                    int SkillRequirementLevel = Integer.parseInt(SkillRequirementString);
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

                                if (CurrentRegion != RegionType.GENERAL)
                                {
                                    newTask.Regions.add(CurrentRegion);
                                }
                                AddNewTaskToDataBase(newTask);
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

    }
}
