package Posiedien_Leagues_Planner;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.sound.sampled.Line;
import java.awt.*;
import java.awt.geom.Area;
import java.util.*;

public class RegionBoundOverlay extends Overlay
{
    private final Client client;
    private final PosiedienLeaguesPlannerPlugin plugin;
    private final LeaguesPlannerConfig config;
    @Inject
    private WorldMapOverlay worldMapOverlay;

    private Area WorldMapClipArea;

    @Inject
    public RegionBoundOverlay(Client Inclient, PosiedienLeaguesPlannerPlugin InPlugin, LeaguesPlannerConfig Inconfig)
    {
        client = Inclient;
        plugin = InPlugin;
        config = Inconfig;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    private Area ObtainWorldMapClipArea(Rectangle baseRectangle)
    {
        final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);

        if (overview != null && !overview.isHidden()) {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden()) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null)
        {
            return null;
        }
        WorldMapClipArea = ObtainWorldMapClipArea(client.getWidget(WidgetInfo.WORLD_MAP_VIEW).getBounds());
        graphics.setClip(WorldMapClipArea);

        // Display a line for all of our connections
        for (LeagueRegionBounds regionDatum : config.RegionData.RegionData)
        {
            Color DrawColor = RegionType.GetRegionColor(config, regionDatum.Type);
            graphics.setColor(DrawColor);

            if (config.GetEditRegion() != RegionType.NONE)
            {
                for (RegionLine line : regionDatum.RegionLines)
                {
                    GraphicsLine convertedLine = line.ConvertToGraphicsLine(worldMapOverlay);
                    if (convertedLine != null)
                    {
                        graphics.drawLine(convertedLine.x1, convertedLine.y1, convertedLine.x2, convertedLine.y2);
                    }
                }
            }

            for (WorldPointPolygon WorldPointPoly : regionDatum.RegionPolygons)
            {
                Polygon convertedPoly = WorldPointPoly.ConvertToGraphicsPolygon(worldMapOverlay);
                if (convertedPoly != null)
                {
                    graphics.fillPolygon(convertedPoly);
                }
            }
        }

        return null;
    }
}
