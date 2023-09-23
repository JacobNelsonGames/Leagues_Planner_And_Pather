package Posiedien_Leagues_Planner;

import com.google.common.graph.Graph;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.*;

public class TaskOverlay extends Overlay
{
    private final Client client;
    private final PosiedienLeaguesPlannerPlugin plugin;
    private final LeaguesPlannerConfig config;
    @Inject
    private WorldMapOverlay worldMapOverlay;

    private Area WorldMapClipArea;

    ArrayList<TaskDisplayPoint> CachedTaskDisplayPoints = new ArrayList<>();

    float CachedZoomLevel = -100.0f;

    public ArrayList<TaskDisplayPoint> GetClickedDisplayPoint(WorldPoint ClickedPoint)
    {
        ArrayList<TaskDisplayPoint> OutDisplayPointsClicked = new ArrayList<>();
        Point ClickedGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(ClickedPoint);
        for (TaskDisplayPoint CurrentDisplayPoint : CachedTaskDisplayPoints)
        {
            Point DisplayGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(CurrentDisplayPoint.MapDisplayPoint);
            if (DisplayGraphicsPoint == null)
            {
                continue;
            }
            int TaskIconSize = config.TaskMapIconSize();

            if (config.ScaleTaskMapIconBasedOnCount())
            {
                TaskIconSize += (int) (config.TaskMapIconSize() * CurrentDisplayPoint.Tasks.size() * (1.0f / config.ScaleTaskMapIconBasedOnCountInvRate()));
            }

            if (ClickedGraphicsPoint.distanceTo(DisplayGraphicsPoint) < TaskIconSize)
            {
                OutDisplayPointsClicked.add(CurrentDisplayPoint);
            }

        }

        return OutDisplayPointsClicked;
    }

    private Area ObtainWorldMapClipArea(Rectangle baseRectangle)
    {
        final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
        final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

        Area clipArea = new Area(baseRectangle);

        if (overview != null && !overview.isHidden())
        {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (surfaceSelector != null && !surfaceSelector.isHidden())
        {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }

    private boolean ShouldMergeIntoDisplayPoint(TaskDisplayPoint DisplayPoint, WorldPoint InWorldPoint)
    {
        // Need to be on same plane
        if (DisplayPoint.MapDisplayPoint.getPlane() != InWorldPoint.getPlane())
        {
            return false;
        }

        Point GraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(InWorldPoint);
        Point DisplayGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(DisplayPoint.MapDisplayPoint);

        if (GraphicsPoint == null)
        {
            return false;
        }

        if (DisplayGraphicsPoint == null)
        {
            return false;
        }

        float DistanceBetweenPoints = GraphicsPoint.distanceTo(DisplayGraphicsPoint);
        float MaxStackingDistance = config.TaskMapStackingDistance();

        return DistanceBetweenPoints < MaxStackingDistance;
    }

    private void AddTaskWorldPointToDisplayPoints(UUID TaskGUID, WorldPoint InWorldPoint, boolean IsOverworldDungeon)
    {
        // First see if there is an existing display point we can be assigned to, if not create a new one
        for (TaskDisplayPoint CurrentDisplayPoint : CachedTaskDisplayPoints)
        {
            if (ShouldMergeIntoDisplayPoint(CurrentDisplayPoint, InWorldPoint))
            {
                // Merge
                CurrentDisplayPoint.TaskWorldPoints.add(InWorldPoint);
                CurrentDisplayPoint.Tasks.add(TaskGUID);

                if (IsOverworldDungeon)
                {
                    CurrentDisplayPoint.DungeonTasks.add(TaskGUID);
                }
                CurrentDisplayPoint.UpdateMapDisplayPoint();
                return;
            }
        }

        // No existing display points close enough to merge, so make a new one
        TaskDisplayPoint NewDisplayPoint = new TaskDisplayPoint();

        NewDisplayPoint.TaskWorldPoints.add(InWorldPoint);
        NewDisplayPoint.Tasks.add(TaskGUID);

        if (IsOverworldDungeon)
        {
            NewDisplayPoint.DungeonTasks.add(TaskGUID);
        }
        NewDisplayPoint.UpdateMapDisplayPoint();

        CachedTaskDisplayPoints.add(NewDisplayPoint);
    }

    private void CacheDisplayPointsIfDirty()
    {
        WorldMap worldMap = client.getWorldMap();
        float zoom = worldMap.getWorldMapZoom();
        if (zoom != CachedZoomLevel)
        {
            CachedZoomLevel = zoom;

            // Go through all of our tasks and figure out our display points for rendering
            CachedTaskDisplayPoints.clear();

            for (Map.Entry<UUID, TaskData> CurrentTaskPair : config.TaskData.LeaguesTaskList.entrySet())
            {
                // Go through all the task locations
                for (WorldPoint TaskWorldPoint : CurrentTaskPair.getValue().Locations)
                {
                    AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), TaskWorldPoint, false);
                }

                // Go through all the task overworld location
                for (WorldPoint TaskOverworldWorldPoint : CurrentTaskPair.getValue().OverworldLocations)
                {
                    AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), TaskOverworldWorldPoint, true);
                }
            }


        }
    }

    @Inject
    public TaskOverlay(Client Inclient, PosiedienLeaguesPlannerPlugin InPlugin, LeaguesPlannerConfig Inconfig)
    {
        client = Inclient;
        plugin = InPlugin;
        config = Inconfig;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }
    private static final BufferedImage TASK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/TaskIcon.png");

    private static final BufferedImage EASY_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Easy.png");
    private static final BufferedImage MEDIUM_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Medium.png");
    private static final BufferedImage HARD_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Hard.png");
    private static final BufferedImage ELITE_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Elite.png");
    private static final BufferedImage MASTER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Master.png");
    private static final BufferedImage DUNGEON_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/DungeonIcon.png");
    private static final BufferedImage BLANK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BlankIcon.png");
    private BufferedImage GetImageFromDifficulty(TaskDifficulty difficulty)
    {
        switch (difficulty)
        {
            case EASY:
                return EASY_IMAGE;
            case MEDIUM:
                return MEDIUM_IMAGE;
            case HARD:
                return HARD_IMAGE;
            case ELITE:
                return ELITE_IMAGE;
            case MASTER:
                return MASTER_IMAGE;
        }

        return EASY_IMAGE;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null)
        {
            return null;
        }

        CacheDisplayPointsIfDirty();

        WorldMapClipArea = ObtainWorldMapClipArea(Objects.requireNonNull(client.getWidget(WidgetInfo.WORLD_MAP_VIEW)).getBounds());
        graphics.setClip(WorldMapClipArea);

        // Go through all of our display points and render on our map
        Color taskIconFontColor = new Color(31, 58, 70,255);

        Color dungeonIconFontColor = new Color(31, 58, 70,255);

        for (TaskDisplayPoint CurrentDisplayPoint : CachedTaskDisplayPoints)
        {
            Point GraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(CurrentDisplayPoint.MapDisplayPoint);
            if (GraphicsPoint == null || !WorldMapClipArea.contains(GraphicsPoint.getX(), GraphicsPoint.getY()))
            {
                continue;
            }

            ArrayList<BufferedImage> ImageModifiers = new ArrayList<>();
            HashSet<TaskDifficulty> DisplayPointDifficulties = new HashSet<>();

            // Fill our image modifiers based on the tasks we represent
            for (UUID CurrentTaskID : CurrentDisplayPoint.Tasks)
            {
                TaskData CurrentTask = config.TaskData.LeaguesTaskList.get(CurrentTaskID);

                if (!DisplayPointDifficulties.contains(CurrentTask.Difficulty))
                {
                    ImageModifiers.add(GetImageFromDifficulty(CurrentTask.Difficulty));
                    DisplayPointDifficulties.add(CurrentTask.Difficulty);
                }
            }

            int TaskIconSize = config.TaskMapIconSize();

            if (config.ScaleTaskMapIconBasedOnCount())
            {
                TaskIconSize += (int) (config.TaskMapIconSize() * CurrentDisplayPoint.Tasks.size() * (1.0f / config.ScaleTaskMapIconBasedOnCountInvRate()));
            }

            int TaskIconSizeHalf = TaskIconSize / 2;

            String TaskWorldPointCountSize = String.valueOf(CurrentDisplayPoint.Tasks.size());
            if (CurrentDisplayPoint.Tasks.size() > 9)
            {
                TaskWorldPointCountSize = "9+";
            }

            int TaskCharacterSize = TaskWorldPointCountSize.length();

            graphics.drawImage(TASK_IMAGE, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);

            int TaskIconModifierSize = TaskIconSize / 3;

            // Rows of 4 and start stacking on top
            int RowCount = 4;
            for (int i = 0; i < ImageModifiers.size(); ++i)
            {
                BufferedImage Image = ImageModifiers.get(i);
                graphics.drawImage(Image,
                        GraphicsPoint.getX() + ((i % RowCount) - 2) * TaskIconModifierSize,
                        GraphicsPoint.getY() - TaskIconSize + TaskIconModifierSize / 2 - (i / RowCount) * TaskIconModifierSize,
                        TaskIconModifierSize,
                        TaskIconModifierSize,
                        null);
            }

            // Dungeon icon
            if (!CurrentDisplayPoint.DungeonTasks.isEmpty())
            {
                graphics.drawImage(DUNGEON_IMAGE,
                        GraphicsPoint.getX(),
                        GraphicsPoint.getY(),
                        (int)(TaskIconModifierSize * 2.0f),
                        (int)(TaskIconModifierSize * 2.0f),
                        null);
            }

            Font taskIconFont = new FontUIResource("TaskCountFont", Font.BOLD, TaskIconSizeHalf);
            graphics.setFont(taskIconFont);
            graphics.setColor(taskIconFontColor);

            int TextIconTextOffsetY = -TaskIconSizeHalf / 2;
            int TextIconTextOffsetX = (TaskIconSizeHalf / 4);

            graphics.drawChars(TaskWorldPointCountSize.toCharArray(),
                    0,
                    TaskCharacterSize,
                    GraphicsPoint.getX() - TextIconTextOffsetX,
                    GraphicsPoint.getY() - TextIconTextOffsetY);
        }


        return null;
    }
}
