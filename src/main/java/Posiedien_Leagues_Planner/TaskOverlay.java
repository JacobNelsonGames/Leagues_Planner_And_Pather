package Posiedien_Leagues_Planner;

import Posiedien_Leagues_Planner.pathfinder.Pathfinder;
import net.runelite.api.Client;
import net.runelite.api.Player;
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

    public WorldMapOverlay worldMapOverlay;

    private Area WorldMapClipArea;

    HashMap<UUID, TaskDisplayPoint> CachedTaskDisplayPoints = new HashMap<>();
    HashMap<UUID, HashSet<UUID> > TaskToDisplayPoints = new HashMap<>();

    float CachedZoomLevel = -100.0f;

    public ArrayList<TaskDisplayPoint> GetClickedDisplayPoint(WorldPoint ClickedPoint)
    {
        ArrayList<TaskDisplayPoint> OutDisplayPointsClicked = new ArrayList<>();
        Point ClickedGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(ClickedPoint);
        for (Map.Entry<UUID, TaskDisplayPoint> CurrentTaskPair : CachedTaskDisplayPoints.entrySet())
        {
            TaskDisplayPoint CurrentDisplayPoint = CurrentTaskPair.getValue();
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
        for (Map.Entry<UUID, TaskDisplayPoint> CurrentTaskPair : CachedTaskDisplayPoints.entrySet())
        {
            TaskDisplayPoint CurrentDisplayPoint = CurrentTaskPair.getValue();
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
                TaskToDisplayPoints.putIfAbsent(TaskGUID, new HashSet<>(Collections.singleton(CurrentDisplayPoint.DisplayPointGUID)));
                TaskToDisplayPoints.get(TaskGUID).add(CurrentDisplayPoint.DisplayPointGUID);
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
        NewDisplayPoint.DisplayPointGUID = UUID.randomUUID();

        CachedTaskDisplayPoints.put(NewDisplayPoint.DisplayPointGUID, NewDisplayPoint);

        TaskToDisplayPoints.putIfAbsent(TaskGUID, new HashSet<>(Collections.singleton(NewDisplayPoint.DisplayPointGUID)));
        TaskToDisplayPoints.get(TaskGUID).add(NewDisplayPoint.DisplayPointGUID);
    }

    public TaskDifficulty DiffFilter = TaskDifficulty.NONE;
    public OtherFilter OthFilter = OtherFilter.NONE;

    public HashSet<RegionType> RegionsUnlocked = new HashSet<>();

    public HashSet<RegionType> TempRegionsUnlocked = new HashSet<>();


    private void CacheDisplayPointsIfDirty()
    {
        WorldMap worldMap = client.getWorldMap();
        float zoom = worldMap.getWorldMapZoom();

        TempRegionsUnlocked.clear();
        for (RegionType CurrentRegion : RegionType.values())
        {
            if (RegionType.GetRegionUnlocked(config, CurrentRegion))
            {
                TempRegionsUnlocked.add(CurrentRegion);
            }
        }

        if (plugin.bMapDisplayPointsDirty ||
                zoom != CachedZoomLevel ||
                DiffFilter != config.FilteredDifficulty() ||
                OthFilter != config.FilteredOther() ||
                !RegionsUnlocked.containsAll(TempRegionsUnlocked) ||
                !TempRegionsUnlocked.containsAll(RegionsUnlocked) )
        {
            plugin.bMapDisplayPointsDirty = false;
            CachedZoomLevel = zoom;
            DiffFilter = config.FilteredDifficulty();
            OthFilter = config.FilteredOther();
            RegionsUnlocked.clear();
            RegionsUnlocked.addAll(TempRegionsUnlocked);

            // Go through all of our tasks and figure out our display points for rendering
            CachedTaskDisplayPoints.clear();
            TaskToDisplayPoints.clear();

            for (Map.Entry<UUID, TaskData> CurrentTaskPair : config.TaskData.LeaguesTaskList.entrySet())
            {
                // Skip due to some filter
                boolean bIsPartOfPlan = config.UserData.PlannedTasks.containsKey(CurrentTaskPair.getKey());
                if (!bIsPartOfPlan && DiffFilter != TaskDifficulty.NONE && DiffFilter != CurrentTaskPair.getValue().Difficulty)
                {
                    continue;
                }

                // Don't display hidden on map
                boolean bIsHidden = plugin.config.UserData.HiddenTasks.contains(CurrentTaskPair.getKey());
                if (bIsHidden)
                {
                    continue;
                }

                if (OthFilter == OtherFilter.ONLY_MAP_PLAN ||
                    OthFilter == OtherFilter.ONLY_PLAN)
                {
                    if (!bIsPartOfPlan)
                    {
                        continue;
                    }
                }

                boolean bSkipTask = false;
                for (RegionType ReqRegion : CurrentTaskPair.getValue().Regions)
                {
                    if (!RegionType.GetRegionUnlocked(config, ReqRegion))
                    {
                        bSkipTask = true;
                        break;
                    }
                }
                if (bSkipTask)
                {
                    continue;
                }

                // Go through all the task locations
                for (WorldPoint TaskWorldPoint : CurrentTaskPair.getValue().Locations)
                {
                    if (config.RegionData.IsTileInUnlockedRegion(config, TaskWorldPoint))
                    {
                        AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), TaskWorldPoint, false);
                    }
                }

                // Go through all the task overworld location
                for (WorldPoint TaskOverworldWorldPoint : CurrentTaskPair.getValue().OverworldLocations)
                {
                    if (config.RegionData.IsTileInUnlockedRegion(config, TaskOverworldWorldPoint))
                    {
                        AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), TaskOverworldWorldPoint, true);
                    }
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

    private static final BufferedImage HIGHLIGHTED_TASK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/HighlightedTaskIcon.png");

    private static final BufferedImage PLANNED_TASK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/PlannedTaskIcon.png");

    private static final BufferedImage PLANNED_HIGHLIGHTED_TASK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/HighlightedPlannedTaskIcon.png");

    private static final BufferedImage TASK_IMAGE_DUNGEON = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/TaskIconDungeon.png");

    private static final BufferedImage HIGHLIGHTED_TASK_IMAGE_DUNGEON = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/HighlightedTaskIconDungeon.png");

    private static final BufferedImage PLANNED_TASK_IMAGE_DUNGEON = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/PlannedTaskIconDungeon.png");
    private static final BufferedImage PLANNED_HIGHLIGHTED_TASK_IMAGE_DUNGEON = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/HighlightedPlannedTaskIconDungeon.png");

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

        Color highlightnamecolor2 = new Color(0, 0, 0,255);

        for (Map.Entry<UUID, TaskDisplayPoint> CurrentTaskPair : CachedTaskDisplayPoints.entrySet())
        {
            TaskDisplayPoint CurrentDisplayPoint = CurrentTaskPair.getValue();
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

                if (CurrentTask != null && !DisplayPointDifficulties.contains(CurrentTask.Difficulty))
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

            Point HighlightGraphicsPoint = new Point(0,0);
            if (plugin.getSelectedWorldPoint() != null)
            {
                HighlightGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(plugin.getSelectedWorldPoint());
                if (HighlightGraphicsPoint == null)
                {
                    HighlightGraphicsPoint = new Point(0,0);
                }
            }

            boolean bIsTaskPlanned = false;
            for (UUID CurrentTaskID : CurrentDisplayPoint.Tasks)
            {
                if (config.UserData.PlannedTasks.containsKey(CurrentTaskID))
                {
                    bIsTaskPlanned = true;
                }
            }

            if (HighlightGraphicsPoint.distanceTo(GraphicsPoint) < TaskIconSize)
            {
                if (CurrentDisplayPoint.Tasks.size() == CurrentDisplayPoint.DungeonTasks.size())
                {
                    if (bIsTaskPlanned)
                    {
                        graphics.drawImage(PLANNED_HIGHLIGHTED_TASK_IMAGE_DUNGEON, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                    else
                    {
                        graphics.drawImage(HIGHLIGHTED_TASK_IMAGE_DUNGEON, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                }
                else
                {
                    if (bIsTaskPlanned)
                    {
                        graphics.drawImage(PLANNED_HIGHLIGHTED_TASK_IMAGE, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                    else
                    {
                        graphics.drawImage(HIGHLIGHTED_TASK_IMAGE, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                }
            }
            else
            {
                if (CurrentDisplayPoint.Tasks.size() == CurrentDisplayPoint.DungeonTasks.size())
                {
                    if (bIsTaskPlanned)
                    {
                        graphics.drawImage(PLANNED_TASK_IMAGE_DUNGEON, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                    else
                    {
                        graphics.drawImage(TASK_IMAGE_DUNGEON, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                }
                else
                {
                    if (bIsTaskPlanned)
                    {
                        graphics.drawImage(PLANNED_TASK_IMAGE, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                    else
                    {
                        graphics.drawImage(TASK_IMAGE, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);
                    }
                }
            }

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

            boolean bIsCloseToMouse = HighlightGraphicsPoint.distanceTo(GraphicsPoint) < TaskIconSize;
            if (bIsTaskPlanned || bIsCloseToMouse)
            {
                Font taskhighlightFont2 = new FontUIResource("taskhighlightFont2", Font.BOLD, 15);
                graphics.setFont(taskhighlightFont2);
                graphics.setColor(highlightnamecolor2);

                int TaskNum2 = 0;
                for (UUID TaskGUID : CurrentDisplayPoint.Tasks)
                {
                    TaskData CurrentTask = config.TaskData.LeaguesTaskList.get(TaskGUID);
                    String ModifiedString = CurrentTask.TaskName;

                    if (config.UserData.PlannedTasks.containsKey(TaskGUID))
                    {
                        ModifiedString += " (" + config.UserData.PlannedTasks.get(TaskGUID).LastSortOrder + ")";
                    }

                    if (bIsCloseToMouse)
                    {
                        graphics.drawChars(ModifiedString.toCharArray(),
                                0,
                                ModifiedString.length(),
                                HighlightGraphicsPoint.getX() - 1,
                                HighlightGraphicsPoint.getY() - TaskNum2 * 15 + 1);

                        ++TaskNum2;
                    }
                    // bIsTaskPlanned
                    else if (config.UserData.PlannedTasks.containsKey(TaskGUID))
                    {
                        graphics.drawChars(ModifiedString.toCharArray(),
                                0,
                                ModifiedString.length(),
                                GraphicsPoint.getX() - 1,
                                GraphicsPoint.getY() - TaskNum2 * 15 + 1);

                        ++TaskNum2;
                    }
                }
                Font taskhighlightFont = new FontUIResource("taskhighlightFont", Font.BOLD, 15);
                graphics.setFont(taskhighlightFont);

                int TaskNum = 0;
                for (UUID TaskGUID : CurrentDisplayPoint.Tasks)
                {
                    TaskData CurrentTask = config.TaskData.LeaguesTaskList.get(TaskGUID);
                    String ModifiedString = CurrentTask.TaskName;

                    if (config.UserData.PlannedTasks.containsKey(TaskGUID))
                    {
                        ModifiedString += " (" + config.UserData.PlannedTasks.get(TaskGUID).LastSortOrder + ")";
                    }

                    if (bIsCloseToMouse)
                    {
                        graphics.setColor(TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty));
                        graphics.drawChars(ModifiedString.toCharArray(),
                                0,
                                ModifiedString.length(),
                                HighlightGraphicsPoint.getX(),
                                HighlightGraphicsPoint.getY() - TaskNum * 15);

                        ++TaskNum;
                    }
                    // bIsTaskPlanned
                    else if (config.UserData.PlannedTasks.containsKey(TaskGUID))
                    {
                        graphics.setColor(TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty));
                        graphics.drawChars(ModifiedString.toCharArray(),
                                0,
                                ModifiedString.length(),
                                GraphicsPoint.getX(),
                                GraphicsPoint.getY() - TaskNum * 15);

                        ++TaskNum;
                    }
                }
            }
        }

        // Go through our plan and draw lines connecting them
        Player player = plugin.client.getLocalPlayer();
        if (player != null)
        {
            graphics.setColor(new Color(255, 226, 1,255));
            WorldPoint LastWorldPoint = player.getWorldLocation();
            WorldPoint LastActualWorldPoint = player.getWorldLocation();

            int SortTaskIter = 0;
            for (SortedTask SortedTaskIter : config.UserData.SortedPlannedTasks)
            {
                TaskData CurrentTask = config.TaskData.LeaguesTaskList.get(SortedTaskIter.TaskGUID);

                // Find our display points with this task
                float ClosestDistance = 9000000.0f;
                float ClosestActualDistance = 9000000.0f;
                WorldPoint ClosestWorldPoint = null;
                WorldPoint ClosestActualWorldPoint = null;
                if (!TaskToDisplayPoints.containsKey(CurrentTask.GUID))
                {
                    continue;
                }

                for (UUID DisplayPointGUID : TaskToDisplayPoints.get(CurrentTask.GUID))
                {
                    WorldPoint CurrentWorldPoint =  CachedTaskDisplayPoints.get(DisplayPointGUID).MapDisplayPoint;
                    float NextDistance = LastWorldPoint.distanceTo(CurrentWorldPoint);
                    if ( NextDistance < ClosestDistance)
                    {
                        ClosestDistance = NextDistance;
                        ClosestWorldPoint = CurrentWorldPoint;
                    }
                }

                for (WorldPoint ActualLocation : CurrentTask.Locations)
                {
                    float NextDistance = LastActualWorldPoint.distanceTo(ActualLocation);
                    if ( NextDistance < ClosestActualDistance)
                    {
                        ClosestActualDistance = NextDistance;
                        ClosestActualWorldPoint = ActualLocation;
                    }
                }

                if (plugin.panel.CurrentPathfinderIndex == SortTaskIter)
                {
                    boolean bIsCurrentPathfindingDone = false;
                    synchronized (plugin.panel.pathfinderMutex)
                    {
                        bIsCurrentPathfindingDone = plugin.panel.pathfinderArray.isEmpty() ||
                                plugin.panel.pathfinderArray.get(SortTaskIter - 1).isDone();
                    }

                    // Start next path
                    if (bIsCurrentPathfindingDone)
                    {
                        ++plugin.panel.CurrentPathfinderIndex;
                        WorldPoint finalLastWorldPoint = LastActualWorldPoint;
                        WorldPoint finalClosestWorldPoint = ClosestActualWorldPoint;
                        plugin.getClientThread().invokeLater(() ->
                        {
                            plugin.pathfinderConfig.refresh();
                            synchronized (plugin.panel.pathfinderMutex)
                            {
                                Pathfinder NewPathfinder = new Pathfinder(plugin.pathfinderConfig, finalLastWorldPoint, finalClosestWorldPoint, false);
                                plugin.panel.pathfinderArray.add(NewPathfinder);
                                plugin.panel.pathfinderFuture = plugin.panel.pathfindingExecutor.submit(NewPathfinder);
                            }
                        });
                    }
                }

                ++SortTaskIter;
                LastActualWorldPoint = ClosestActualWorldPoint;

                // Draw arrow from closest distance to next task
                Point GraphicsStart = worldMapOverlay.mapWorldPointToGraphicsPoint(ClosestWorldPoint);
                if (GraphicsStart == null)
                {
                    LastWorldPoint = ClosestWorldPoint;
                    continue;
                }

                Point GraphicsEnd = worldMapOverlay.mapWorldPointToGraphicsPoint(LastWorldPoint);
                if (GraphicsEnd == null)
                {
                    LastWorldPoint = ClosestWorldPoint;
                    continue;
                }

                graphics.drawLine(GraphicsStart.getX(), GraphicsStart.getY(), GraphicsEnd.getX(), GraphicsEnd.getY());
                LastWorldPoint = ClosestWorldPoint;
            }


        }

        return null;
    }
}
