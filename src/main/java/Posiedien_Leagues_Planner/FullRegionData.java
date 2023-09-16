package Posiedien_Leagues_Planner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.api.Tile;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class FullRegionData
{
    public ArrayList<LeagueRegionBounds> RegionData = new ArrayList<>();

    boolean IsTileInUnlockedRegion(LeaguesPlannerConfig config, WorldPoint TileLocation)
    {
        ArrayList<RegionType> RegionsForTile = GetTileRegions(TileLocation);

        // If not in any region, that means it available for all reasons
        if (!RegionsForTile.isEmpty())
        {
            for (RegionType regionType : RegionsForTile)
            {
                if (RegionType.GetRegionUnlocked(config, regionType))
                {
                    return true;
                }
            }
        }
        else
        {
            return true;
        }

        return false;
    }

    public ArrayList<RegionType> GetTileRegions(WorldPoint TileLocation)
    {
        ArrayList<RegionType> OutRegions = new ArrayList<RegionType>();
        for (LeagueRegionBounds regionDatum : RegionData)
        {
            ArrayList<WorldPointPolygon> RegionPolygons = regionDatum.RegionPolygons;
            for (WorldPointPolygon regionPolygon : RegionPolygons)
            {
                Polygon WorldPoly = regionPolygon.WorldPoly;
                int WorldPolyZ = regionPolygon.WorldPolyZ;
                if (WorldPolyZ == TileLocation.getPlane() && WorldPoly.contains(TileLocation.getX(), TileLocation.getY()))
                {
                    OutRegions.add(regionDatum.Type);
                    break;
                }
            }
        }

        return OutRegions;
    }

    public void exportTo(File file) throws IOException
    {
        try (FileWriter fw = new FileWriter(file))
        {
            fw.write(ExportData());
        }
    }

    // Fixup all of our linkage
    private void FixupAfterImport()
    {
        for (LeagueRegionBounds Region : RegionData)
        {
            for (HashMap.Entry<UUID, LeagueRegionPoint> entry : Region.RegionPoints.entrySet())
            {
                entry.getValue().ConnectedPoints.clear();
                for (UUID ConnectedGUID : entry.getValue().ConnectedPointGUIDs)
                {
                    entry.getValue().ConnectedPoints.add(Region.RegionPoints.get(ConnectedGUID));
                }
            }
        }
    }

    public void importFrom(File file)
    {
        try (Scanner sc = new Scanner(file))
        {
            sc.useDelimiter(",");
            ImportData(sc);
        } catch (IOException ignored)
        {
        }

        FixupAfterImport();
    }
    public String ExportData()
    {
        StringBuilder Converted = new StringBuilder();

        Converted.append(RegionData.size());
        Converted.append(",");

        for (LeagueRegionBounds Region : RegionData)
        {
            Converted.append(Region.ExportData());
        }
        
        return Converted.toString();
    }

    public void ImportData(Scanner sc)
    {
        RegionData.clear();
        int RegionSize = Integer.parseInt(sc.next());

        for (int i = 0; i < RegionSize; ++i)
        {
            LeagueRegionBounds NewRegionBounds = new LeagueRegionBounds();
            NewRegionBounds.ImportData(sc);
            RegionData.add(NewRegionBounds);
        }
    }
}
