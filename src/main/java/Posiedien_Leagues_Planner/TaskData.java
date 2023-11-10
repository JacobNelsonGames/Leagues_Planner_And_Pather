package Posiedien_Leagues_Planner;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.CombatLevelRequirement;
import net.runelite.client.plugins.achievementdiary.Requirement;
import net.runelite.client.plugins.achievementdiary.SkillRequirement;

import javax.swing.plaf.synth.Region;
import java.util.ArrayList;
import java.util.UUID;

public class TaskData
{
    boolean bIsRemoved = false;

    UUID GUID = null;

    TaskDifficulty Difficulty;

    String TaskName = "Blank";

    String TaskDescription = "Blank";

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
            else if (req.getClass() == CombatLevelRequirement.class)
            {
                CombatLevelRequirement CombatReq = (CombatLevelRequirement)req;

                int SkillDiff = CombatReq.getLevel() - client.getLocalPlayer().getCombatLevel();
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

    public String ExportUserDataFormat()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append(TaskName);
        Converted.append(",");
        Converted.append(Difficulty);
        Converted.append(",");
        Converted.append(TaskDescription);
        Converted.append(",");
        Converted.append(GUID);
        Converted.append(",");
        Converted.append(bIsCustomTask);
        Converted.append(",");
        Converted.append(CustomIcon);
        Converted.append(",");
        Converted.append(Regions.size());
        Converted.append(",");
        for (RegionType CurrentRegion : Regions)
        {
            Converted.append(CurrentRegion);
            Converted.append(",");
        }

        Converted.append(OverworldLocations.size());
        Converted.append(",");
        for (WorldPoint CurrentPoint : OverworldLocations)
        {
            Converted.append(CurrentPoint.getX());
            Converted.append(",");
            Converted.append(CurrentPoint.getY());
            Converted.append(",");
            Converted.append(CurrentPoint.getPlane());
            Converted.append(",");
        }

        Converted.append(Locations.size());
        Converted.append(",");
        for (WorldPoint CurrentPosition : Locations)
        {
            Converted.append(CurrentPosition.getX());
            Converted.append(",");
            Converted.append(CurrentPosition.getY());
            Converted.append(",");
            Converted.append(CurrentPosition.getPlane());
            Converted.append(",");
        }

        Converted.append(Requirements.size());
        Converted.append(",");
        for (Requirement CurrentRequirement : Requirements)
        {
            if (CurrentRequirement.getClass() == SkillRequirement.class)
            {
                SkillRequirement SkillReq = (SkillRequirement)CurrentRequirement;
                Converted.append(SkillReq.getSkill());
                Converted.append(",");
                Converted.append(SkillReq.getLevel());
            }
            else if (CurrentRequirement.getClass() == CombatLevelRequirement.class)
            {
                CombatLevelRequirement CombatReq = (CombatLevelRequirement)CurrentRequirement;
                Converted.append("COMBAT");
                Converted.append(",");
                Converted.append(CombatReq.getLevel());
            }
            Converted.append(",");
            Converted.append("\r\n");
        }

        return Converted.toString();
    }

    public String ExportData()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append(TaskName);
        Converted.append(",");
        Converted.append(Difficulty);
        Converted.append(",");
        Converted.append(TaskDescription);
        Converted.append(",");
        Converted.append(GUID);
        Converted.append(",");
        Converted.append(bIsCustomTask);
        Converted.append(",");
        Converted.append(CustomIcon);
        Converted.append(",");
        Converted.append("Regions Count: ,");
        Converted.append(Regions.size());
        Converted.append(",");
        for (RegionType CurrentRegion : Regions)
        {
            Converted.append(CurrentRegion);
            Converted.append(",");
        }

        Converted.append("Overworld Location Count (Auto-generated): ,");
        Converted.append(OverworldLocations.size());
        Converted.append(",");
        for (WorldPoint CurrentPoint : OverworldLocations)
        {
            Converted.append(CurrentPoint.getX());
            Converted.append(",");
            Converted.append(CurrentPoint.getY());
            Converted.append(",");
            Converted.append(CurrentPoint.getPlane());
            Converted.append(",");
        }

        Converted.append(",");
        Converted.append("\r\n");
        Converted.append(",");
        Converted.append("POSITIONS_START,");
        if (Locations.size() == 0)
        {
            Converted.append("NO_POSITIONS");
        }
        else
        {
            Converted.append("HAS_POSITIONS");
        }
        Converted.append(",");
        Converted.append("\r\n");
        Converted.append("X");
        Converted.append(",");
        Converted.append("Y");
        Converted.append(",");
        Converted.append("Z");
        Converted.append(",");
        Converted.append("\r\n");

        // Set up for manually adding things
        if (Locations.isEmpty())
        {
            Converted.append("\r\n");
        }

        for (WorldPoint CurrentPosition : Locations)
        {
            Converted.append(CurrentPosition.getX());
            Converted.append(",");
            Converted.append(CurrentPosition.getY());
            Converted.append(",");
            Converted.append(CurrentPosition.getPlane());
            Converted.append(",");
            Converted.append("\r\n");
        }

        Converted.append("POSITIONS_END");
        Converted.append(",");
        Converted.append("\r\n");

        Converted.append(",");
        Converted.append("\r\n");
        Converted.append("REQUIREMENTS_START");
        Converted.append(",");
        Converted.append("\r\n");
        Converted.append("SKILL NAME");
        Converted.append(",");
        Converted.append("LEVEL REQUIREMENT");
        Converted.append(",");
        Converted.append("\r\n");

        for (Requirement CurrentRequirement : Requirements)
        {
            if (CurrentRequirement.getClass() == SkillRequirement.class)
            {
                SkillRequirement SkillReq = (SkillRequirement)CurrentRequirement;
                Converted.append(SkillReq.getSkill());
                Converted.append(",");
                Converted.append(SkillReq.getLevel());
            }
            else if (CurrentRequirement.getClass() == CombatLevelRequirement.class)
            {
                CombatLevelRequirement CombatReq = (CombatLevelRequirement)CurrentRequirement;
                Converted.append("COMBAT");
                Converted.append(",");
                Converted.append(CombatReq.getLevel());
            }
            Converted.append(",");
            Converted.append("\r\n");
        }

        Converted.append("REQUIREMENTS_END");
        Converted.append(",");
        Converted.append("\r\n");
        Converted.append(",");
        Converted.append("\r\n");

        return Converted.toString();
    }
}
