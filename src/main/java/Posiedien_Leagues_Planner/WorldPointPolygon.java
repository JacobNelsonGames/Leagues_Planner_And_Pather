package Posiedien_Leagues_Planner;

import net.runelite.api.Point;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;

public class WorldPointPolygon
{
    ArrayList<WorldPoint> WorldPoints = new ArrayList<>();
    Polygon WorldPoly = new Polygon();
    int WorldPolyZ = 0;

    public void CacheWorldPoly()
    {
        WorldPoly.reset();
        for (WorldPoint currentPoint : WorldPoints)
        {
            WorldPoly.addPoint(currentPoint.getX(), currentPoint.getY());

            // Should be all the same
            WorldPolyZ = currentPoint.getPlane();
        }
    }

    void AddWorldPoint(WorldPoint newWorldPoint)
    {
        WorldPoints.add(newWorldPoint);
    }

    public Polygon ConvertToGraphicsPolygon(WorldMapOverlay worldMapOverlay, Area worldMapClipArea)
    {
        Polygon newPoly = new Polygon();
        boolean shouldDraw = true;
        for (WorldPoint currentPoint : WorldPoints)
        {
            Point GraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(currentPoint);
            if (GraphicsPoint == null)
            {
                return null;
            }

            newPoly.addPoint(GraphicsPoint.getX(), GraphicsPoint.getY());
        }

        return newPoly;
    }
}
