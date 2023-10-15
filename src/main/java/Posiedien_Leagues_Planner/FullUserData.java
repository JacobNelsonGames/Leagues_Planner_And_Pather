package Posiedien_Leagues_Planner;

import ch.qos.logback.core.BasicStatusManager;
import net.runelite.api.Skill;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.CombatLevelRequirement;
import net.runelite.client.plugins.achievementdiary.Requirement;
import net.runelite.client.plugins.achievementdiary.SkillRequirement;

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

    public void importFrom(File targ)
    {
        if (targ.exists())
        {
            try (Scanner sc = new Scanner(targ))
            {
                // CSV file, so comma delimiter
                sc.useDelimiter(",");

                Integer CustomTaskSize = Integer.valueOf(GetNextIgnoreBlank(sc));

                for (int i = 0; i < CustomTaskSize; ++i)
                {
                    TaskData NewCustomTask = new TaskData();

                    NewCustomTask.TaskName = GetNextIgnoreBlank(sc);
                    NewCustomTask.Difficulty = TaskDifficulty.valueOf(GetNextIgnoreBlank(sc));
                    NewCustomTask.TaskDescription = GetNextIgnoreBlank(sc);
                    NewCustomTask.GUID = UUID.fromString(GetNextIgnoreBlank(sc));
                    NewCustomTask.bIsCustomTask = Boolean.valueOf((GetNextIgnoreBlank(sc)));
                    NewCustomTask.CustomIcon = (GetNextIgnoreBlank(sc));

                    Integer RegionSize = Integer.valueOf((GetNextIgnoreBlank(sc)));
                    for (int j = 0; j < RegionSize; ++j)
                    {
                        RegionType NewRegionType = RegionType.valueOf(GetNextIgnoreBlank(sc));
                        NewCustomTask.Regions.add(NewRegionType);
                    }

                    Integer OverworldLocationsSize = Integer.valueOf((GetNextIgnoreBlank(sc)));
                    for (int j = 0; j < OverworldLocationsSize; ++j)
                    {
                        int WorldPointX = Integer.parseInt(GetNextIgnoreBlank(sc));
                        int WorldPointY = Integer.parseInt(GetNextIgnoreBlank(sc));
                        int WorldPointZ = Integer.parseInt(GetNextIgnoreBlank(sc));

                        NewCustomTask.OverworldLocations.add(new WorldPoint(WorldPointX, WorldPointY, WorldPointZ));
                    }

                    Integer LocationsSize = Integer.valueOf((GetNextIgnoreBlank(sc)));
                    for (int j = 0; j < LocationsSize; ++j)
                    {
                        int WorldPointX = Integer.parseInt(GetNextIgnoreBlank(sc));
                        int WorldPointY = Integer.parseInt(GetNextIgnoreBlank(sc));
                        int WorldPointZ = Integer.parseInt(GetNextIgnoreBlank(sc));

                        NewCustomTask.Locations.add(new WorldPoint(WorldPointX, WorldPointY, WorldPointZ));
                    }

                    Integer RequirementSize = Integer.valueOf((GetNextIgnoreBlank(sc)));
                    for (int j = 0; j < RequirementSize; ++j)
                    {
                        String ReqType = GetNextIgnoreBlank(sc);
                        Integer SkillLevel = Integer.valueOf(GetNextIgnoreBlank(sc));
                        if (ReqType.contains("COMBAT"))
                        {
                            CombatLevelRequirement CombatReq = new CombatLevelRequirement(SkillLevel);
                            NewCustomTask.Requirements.add(CombatReq);
                        }
                        else
                        {
                            Skill SkillVal = Skill.valueOf(ReqType);
                            SkillRequirement SkillReq = new SkillRequirement(SkillVal, SkillLevel);
                            NewCustomTask.Requirements.add(SkillReq);
                        }
                    }

                    CustomTasks.put(NewCustomTask.GUID, NewCustomTask);
                }

                Integer PlannedTaskSize = Integer.valueOf(GetNextIgnoreBlank(sc));
                for (int i = 0; i < PlannedTaskSize; ++i)
                {
                    UUID GUID = UUID.fromString(GetNextIgnoreBlank(sc));

                    Integer NewSortPriority = Integer.valueOf(GetNextIgnoreBlank(sc));
                    Integer NewLastSortOrder = Integer.valueOf(GetNextIgnoreBlank(sc));
                    Boolean bNewIsCustomTask = Boolean.valueOf(GetNextIgnoreBlank(sc));
                    TaskSortData NewSortData = new TaskSortData(NewSortPriority, bNewIsCustomTask);
                    NewSortData.LastSortOrder = NewLastSortOrder;

                    PlannedTasks.put(GUID, NewSortData);
                }


                Integer HiddenTaskSize = Integer.valueOf(GetNextIgnoreBlank(sc));
                for (int i = 0; i < HiddenTaskSize; ++i)
                {
                    UUID GUID = UUID.fromString(GetNextIgnoreBlank(sc));
                    HiddenTasks.add(GUID);
                }

                Integer CompletedTaskSize = Integer.valueOf(GetNextIgnoreBlank(sc));
                for (int i = 0; i < CompletedTaskSize; ++i)
                {
                    UUID GUID = UUID.fromString(GetNextIgnoreBlank(sc));
                    CompletedTasks.add(GUID);
                }

            } catch (IOException ignored)
            {
            }
        }
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
