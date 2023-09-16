package Posiedien_Leagues_Planner;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class LeagueRegionPoint
{

    public UUID GUID = null;
    public ArrayList<LeagueRegionPoint> ConnectedPoints = new ArrayList<>();

    // Temporary for setup
    public ArrayList<UUID> ConnectedPointGUIDs = new ArrayList<>();

    public WorldMapPoint OurPoint = null;
    public WorldPoint OurWorldPoint = null;

    RegionType Region = null;

    public String ExportData()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append(GUID);
        Converted.append(",");

        Converted.append(Region);
        Converted.append(",");

        Converted.append(ConnectedPoints.size());
        Converted.append(",");

        for (LeagueRegionPoint ConnectedPoint : ConnectedPoints)
        {
            Converted.append(ConnectedPoint.GUID);
            Converted.append(",");
        }

        Converted.append(OurWorldPoint.getX());
        Converted.append(",");

        Converted.append(OurWorldPoint.getY());
        Converted.append(",");

        Converted.append(OurWorldPoint.getPlane());
        Converted.append(",");

        return Converted.toString();
    }

    public void ImportData(Scanner sc)
    {
        GUID = UUID.fromString(sc.next());

        Region = RegionType.valueOf((sc.next()));

        int ConnectedPointsSize = Integer.parseInt(sc.next());
        for (int i = 0; i < ConnectedPointsSize; ++i)
        {
            ConnectedPointGUIDs.add(UUID.fromString(sc.next()));
        }

        OurWorldPoint = new WorldPoint(
                Integer.parseInt(sc.next()),
                Integer.parseInt(sc.next()),
                Integer.parseInt(sc.next()));
    }
}
