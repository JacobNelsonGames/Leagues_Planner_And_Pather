package Posiedien_Leagues_Planner;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.Requirement;
import net.runelite.client.plugins.achievementdiary.SkillRequirement;

import java.util.ArrayList;
import java.util.UUID;

public class TaskData
{
    UUID GUID;

    TaskDifficulty Difficulty;

    String TaskName = "";

    Boolean bIsCustomTask = false;

    String CustomIcon = null;

    ArrayList<RegionType> Regions = new ArrayList<>();

    ArrayList<WorldPoint> Locations = new ArrayList<>();


    // Keep a separate cache for overworld locations so we can show those on the dungeon entrances
    ArrayList<WorldPoint> OverworldLocations = new ArrayList<>();

    ArrayList<Requirement> Requirements = new ArrayList<>();

    int CalculateNeededRequirementsForTask(Client client)
    {
        int OutReqDiff = 0;
        for (Requirement req : Requirements)
        {
            if (req.getClass() == SkillRequirement.class)
            {
                SkillRequirement SkillReq = (SkillRequirement)req;

                int SkillDiff = SkillReq.getLevel() - client.getRealSkillLevel(SkillReq.getSkill());
                if (SkillDiff > 0)
                {
                    OutReqDiff += SkillDiff;
                }
            }
            else
            {
                if (!req.satisfiesRequirement(client))
                {
                    ++OutReqDiff;
                }
            }
        }

        return OutReqDiff;
    }

}
