package Posiedien_Leagues_Planner;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.achievementdiary.Requirement;

import java.util.ArrayList;
import java.util.UUID;

public class TaskData
{
    UUID GUID;

    TaskDifficulty Difficulty;

    String TaskName = "";

    String TaskDescription = "";

    ArrayList<RegionType> Regions = new ArrayList<>();

    ArrayList<WorldPoint> Locations = new ArrayList<>();


    // Keep a separate cache for overworld locations so we can show those on the dungeon entrances
    ArrayList<WorldPoint> OverworldLocations = new ArrayList<>();

    ArrayList<Requirement> Requirements = new ArrayList<>();

}
