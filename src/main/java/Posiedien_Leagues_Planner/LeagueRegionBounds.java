package Posiedien_Leagues_Planner;

import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import javax.sound.sampled.Line;
import java.awt.*;
import java.util.*;

public class LeagueRegionBounds
{
    public RegionType Type = null;

    // Basically a linked list of points
    public HashMap<UUID, LeagueRegionPoint> RegionPoints = new HashMap<UUID, LeagueRegionPoint>();

    // Transient
    public ArrayList<WorldPointPolygon> RegionPolygons = new ArrayList<>();
    public ArrayList<RegionLine> RegionLines = new ArrayList<>();

    public LeagueRegionBounds()
    {
    }
    public LeagueRegionBounds(RegionType currentRegion)

    {
        Type = currentRegion;
    }

    public String ExportData()
    {
        StringBuilder ExportString = new StringBuilder();
        ExportString.append(Type);
        ExportString.append(",");

        ExportString.append(RegionPoints.size());
        ExportString.append(",");

        for (HashMap.Entry<UUID, LeagueRegionPoint> entry : RegionPoints.entrySet())
        {
            ExportString.append(entry.getValue().ExportData());
        }

        return ExportString.toString();
    }

    public void ImportData(Scanner sc)
    {
        Type = RegionType.valueOf(sc.next());

        int RegionPointSize = Integer.parseInt(sc.next());
        for (int i = 0; i < RegionPointSize; ++i)
        {
            LeagueRegionPoint NewRegionPoint = new LeagueRegionPoint();
            NewRegionPoint.ImportData(sc);
            RegionPoints.put(NewRegionPoint.GUID, NewRegionPoint);
        }

    }
}
