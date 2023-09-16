package Posiedien_Leagues_Planner;

import java.awt.*;
import java.awt.geom.Area;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

public class RegionLine
{
    WorldPoint Start;
    WorldPoint End;

    public RegionLine(WorldPoint start, WorldPoint end)
    {
        Start = start;
        End = end;
    }

    public GraphicsLine ConvertToGraphicsLine(WorldMapOverlay worldMapOverlay, Area worldMapClipArea)
    {
        GraphicsLine newLine = new GraphicsLine();
        Point GraphicsStart = worldMapOverlay.mapWorldPointToGraphicsPoint(Start);
        if (GraphicsStart == null)
        {
            return null;
        }

        newLine.x1 = GraphicsStart.getX();
        newLine.y1 = GraphicsStart.getY();

        Point GraphicsEnd = worldMapOverlay.mapWorldPointToGraphicsPoint(End);
        if (GraphicsEnd == null)
        {
            return null;
        }

        newLine.x2 = GraphicsEnd.getX();
        newLine.y2 = GraphicsEnd.getY();

        return newLine;
    }
}
