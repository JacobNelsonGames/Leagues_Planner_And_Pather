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
import java.awt.geom.Rectangle2D;
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

            int TaskCount = Math.min(CurrentDisplayPoint.Tasks.size(), 5);

            if (config.ScaleTaskMapIconBasedOnCount())
            {
                TaskIconSize += (int) (config.TaskMapIconSize() * TaskCount * (1.0f / config.ScaleTaskMapIconBasedOnCountInvRate()));
            }

            if (ClickedGraphicsPoint.distanceTo(DisplayGraphicsPoint) < TaskIconSize)
            {
                OutDisplayPointsClicked.add(CurrentDisplayPoint);
            }

        }

        return OutDisplayPointsClicked;
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

    private void AddTaskWorldPointToDisplayPoints(UUID TaskGUID, boolean bIsCustomTask, WorldPoint InWorldPoint, boolean IsOverworldDungeon)
    {
        // First see if there is an existing display point we can be assigned to, if not create a new one
        for (Map.Entry<UUID, TaskDisplayPoint> CurrentTaskPair : CachedTaskDisplayPoints.entrySet())
        {
            TaskDisplayPoint CurrentDisplayPoint = CurrentTaskPair.getValue();
            if (ShouldMergeIntoDisplayPoint(CurrentDisplayPoint, InWorldPoint))
            {
                // Merge
                CurrentDisplayPoint.TaskWorldPoints.add(InWorldPoint);
                CurrentDisplayPoint.Tasks.put(TaskGUID, bIsCustomTask);

                if (IsOverworldDungeon)
                {
                    CurrentDisplayPoint.DungeonTasks.put(TaskGUID, bIsCustomTask);
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
        NewDisplayPoint.Tasks.put(TaskGUID, bIsCustomTask);

        if (IsOverworldDungeon)
        {
            NewDisplayPoint.DungeonTasks.put(TaskGUID, bIsCustomTask);
        }
        NewDisplayPoint.UpdateMapDisplayPoint();
        NewDisplayPoint.DisplayPointGUID = UUID.randomUUID();

        CachedTaskDisplayPoints.put(NewDisplayPoint.DisplayPointGUID, NewDisplayPoint);

        TaskToDisplayPoints.putIfAbsent(TaskGUID, new HashSet<>(Collections.singleton(NewDisplayPoint.DisplayPointGUID)));
        TaskToDisplayPoints.get(TaskGUID).add(NewDisplayPoint.DisplayPointGUID);
    }

    public TaskDifficulty DiffFilter = TaskDifficulty.NONE;
    public OtherFilter OthFilter = OtherFilter.NONE;
    public FilterRequirements ReqFilter = FilterRequirements.NONE;

    public HashSet<RegionType> RegionsUnlocked = new HashSet<>();

    public HashSet<RegionType> TempRegionsUnlocked = new HashSet<>();

    private void AddTaskListToDisplayPointCache(HashMap<UUID, TaskData> TaskCache)
    {

        Integer MaxSortPriority = config.UserData.FindSortPriorityMaxOfIndices(5);

        for (Map.Entry<UUID, TaskData> CurrentTaskPair : TaskCache.entrySet())
        {

            if (CurrentTaskPair.getValue().bIsRemoved)
            {
                continue;
            }

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

            // Don't display completed on map
            boolean bIsCompleted = plugin.config.UserData.CompletedTasks.contains(CurrentTaskPair.getKey());
            if (bIsCompleted)
            {
                continue;
            }

            int ReqDifferent = CurrentTaskPair.getValue().CalculateNeededRequirementsForTask(plugin.client);
            if (ReqFilter == FilterRequirements.MEETS_REQ)
            {
                if (ReqDifferent != 0)
                {
                    continue;
                }
            }
            else if (ReqFilter == FilterRequirements.NEAR_REQ)
            {
                // 10 levels away
                if (ReqDifferent > 10)
                {
                    continue;
                }
            }

            if (OthFilter == OtherFilter.NO_PLAN)
            {
                if (bIsPartOfPlan)
                {
                    continue;
                }
            }

            if (OthFilter == OtherFilter.NEXT_5_TASKS)
            {
                if (bIsPartOfPlan)
                {
                    if (config.UserData.PlannedTasks.get(CurrentTaskPair.getKey()).SortPriority > MaxSortPriority)
                    {
                        continue;
                    }
                }
            }

            if (OthFilter == OtherFilter.ONLY_MAP_PLAN ||
                    OthFilter == OtherFilter.ONLY_PLAN)
            {
                if (!bIsPartOfPlan && !CurrentTaskPair.getValue().bIsCustomTask)
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

            // Full cancel
            if (bCancelDisplayPointTaskInProgress)
            {
                return;
            }

            // Go through all the task locations
            for (WorldPoint TaskWorldPoint : CurrentTaskPair.getValue().Locations)
            {
                if (config.RegionData.IsTileInUnlockedRegion(config, TaskWorldPoint))
                {
                    AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), CurrentTaskPair.getValue().bIsCustomTask, TaskWorldPoint, false);
                }
            }

            // Go through all the task overworld location
            for (WorldPoint TaskOverworldWorldPoint : CurrentTaskPair.getValue().OverworldLocations)
            {
                if (config.RegionData.IsTileInUnlockedRegion(config, TaskOverworldWorldPoint))
                {
                    AddTaskWorldPointToDisplayPoints(CurrentTaskPair.getKey(), CurrentTaskPair.getValue().bIsCustomTask, TaskOverworldWorldPoint, true);
                }
            }
        }
    }

    boolean bDisplayPointTaskInProgress = false;
    boolean bCancelDisplayPointTaskInProgress = false;

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
                ReqFilter != config.FilteredRequirements() ||
                !RegionsUnlocked.containsAll(TempRegionsUnlocked) ||
                !TempRegionsUnlocked.containsAll(RegionsUnlocked) )
        {

            if (!bDisplayPointTaskInProgress)
            {
                plugin.bMapDisplayPointsDirty = false;
                CachedZoomLevel = zoom;
                DiffFilter = config.FilteredDifficulty();
                OthFilter = config.FilteredOther();
                ReqFilter = config.FilteredRequirements();
                RegionsUnlocked.clear();
                RegionsUnlocked.addAll(TempRegionsUnlocked);

                bDisplayPointTaskInProgress = true;
                plugin.getClientThread().invokeLater(() ->
                {
                    // Go through all of our tasks and figure out our display points for rendering
                    CachedTaskDisplayPoints.clear();
                    TaskToDisplayPoints.clear();

                    AddTaskListToDisplayPointCache(config.TaskData.LeaguesTaskList);
                    AddTaskListToDisplayPointCache(config.UserData.CustomTasks);

                    config.UserData.CacheSortedPlannedTasks();

                    bCancelDisplayPointTaskInProgress = false;
                    bDisplayPointTaskInProgress = false;
                });
            }
            else
            {
                bCancelDisplayPointTaskInProgress = true;
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

    private static final BufferedImage BEGINNER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Beginner.png");
    private static final BufferedImage EASY_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Easy.png");
    private static final BufferedImage MEDIUM_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Medium.png");
    private static final BufferedImage HARD_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Hard.png");
    private static final BufferedImage ELITE_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Elite.png");
    private static final BufferedImage MASTER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Master.png");
    private static final BufferedImage CUSTOM_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/Custom.png");
    private static final BufferedImage DUNGEON_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/DungeonIcon.png");
    private static final BufferedImage BLANK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BlankIcon.png");
    private BufferedImage GetImageFromDifficulty(TaskDifficulty difficulty)
    {
        switch (difficulty)
        {
            case BEGINNER:
                return BEGINNER_IMAGE;
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
            case CUSTOM:
                return CUSTOM_IMAGE;
        }

        return BEGINNER_IMAGE;
    }


    public void RenderingChangingIconUI(Graphics2D graphics)
    {
        Point HighlightGraphicsPoint = new Point(0,0);
        if (plugin.getSelectedWorldPoint() != null)
        {
            HighlightGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(plugin.getSelectedWorldPoint());
            if (HighlightGraphicsPoint == null)
            {
                HighlightGraphicsPoint = new Point(0,0);
            }
        }

        // Render all of our options centered around the center of the screen
        int TotalIconCount = plugin.CustomIconsMap.size();
        int IconSize = 30;
        int IconSizeHalf = IconSize / 2;

        Rectangle2D Bounds = WorldMapClipArea.getBounds2D();

        int RowAndHeightSize = (int) Math.sqrt(TotalIconCount);
        int i = 0;
        for (Map.Entry<String, BufferedImage> CustomIcon : plugin.CustomIconsMap.entrySet())
        {
            int OffsetX = (int) (Bounds.getCenterX() - (RowAndHeightSize / 2) * IconSize);
            int OffsetY = (int) (Bounds.getCenterY() - (RowAndHeightSize / 2) * IconSize);

            OffsetX += ((i % RowAndHeightSize) * IconSize);
            OffsetY += ((i / RowAndHeightSize) * IconSize);

            Point IconPoint = new Point(OffsetX, OffsetY);
            if (HighlightGraphicsPoint.distanceTo(IconPoint) < IconSizeHalf)
            {
                int HighlightedSize = (int) (IconSize * 1.5f);
                int HighlightedHalf = HighlightedSize / 2;
                graphics.drawImage(PosiedienLeaguesPlannerPlugin.BOUNDS_SELECTED, (int) OffsetX - HighlightedHalf, (int) OffsetY - HighlightedHalf, HighlightedSize, HighlightedSize, null);
            }
            graphics.drawImage(CustomIcon.getValue(), (int) OffsetX - IconSizeHalf, (int) OffsetY - IconSizeHalf, IconSize, IconSize, null);

            ++i;
        }

    }

    static class OverlayQueuedText
    {
        String TextValue;
        Font FontValue;
        Color ColorValue;
        int Offset;
        int X;
        int Y;

        public OverlayQueuedText(String taskWorldPointCountSize,
                                 int InOffset,
                                 int InX,
                                 int InY,
                                 Font taskIconFont,
                                 Color taskIconFontColor)
        {
            TextValue = taskWorldPointCountSize;
            FontValue = taskIconFont;
            ColorValue = taskIconFontColor;
            Offset = InOffset;
            X = InX;
            Y = InY;
        }
    };

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.bIsInitializing)
        {
            return null;
        }

        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null)
        {
            return null;
        }

        WorldMapClipArea = plugin.ObtainWorldMapClipArea(Objects.requireNonNull(client.getWidget(WidgetInfo.WORLD_MAP_VIEW)).getBounds());
        graphics.setClip(WorldMapClipArea);

        // We are selecting a custom icon, do not render anything else
        if (plugin.CustomTask_ChangingIcon != null)
        {
            RenderingChangingIconUI(graphics);
            return null;
        }

        CacheDisplayPointsIfDirty();

        if (bDisplayPointTaskInProgress)
        {
            return null;
        }
        ArrayList<OverlayQueuedText> QueuedTextCommands = new ArrayList<>();

        // Go through all of our display points and render on our map
        Color taskIconFontColor = new Color(31, 58, 70,255);

        Color highlightnamecolor2 = new Color(0, 0, 0,255);

        Integer MaxSortPriority = config.UserData.FindSortPriorityMaxOfIndices(5);
        boolean bSetHighlighted = false;
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
            BufferedImage OverrideImage = null;
            for (HashMap.Entry<UUID, Boolean > mapElement : CurrentDisplayPoint.Tasks.entrySet())
            {
                TaskData CurrentTask = plugin.GetTaskData(mapElement.getKey(), mapElement.getValue());

                if (CurrentTask != null && !DisplayPointDifficulties.contains(CurrentTask.Difficulty))
                {
                    ImageModifiers.add(GetImageFromDifficulty(CurrentTask.Difficulty));
                    DisplayPointDifficulties.add(CurrentTask.Difficulty);
                }

                if (CurrentTask != null && CurrentTask.CustomIcon != null)
                {
                    OverrideImage = plugin.CustomIconsMap.get(CurrentTask.CustomIcon);
                }
            }

            int TaskCount = Math.min(CurrentDisplayPoint.Tasks.size(), 5);

            int TaskIconSize = config.TaskMapIconSize();

            if (config.ScaleTaskMapIconBasedOnCount())
            {
                TaskIconSize += (int) (config.TaskMapIconSize() * TaskCount * (1.0f / config.ScaleTaskMapIconBasedOnCountInvRate()));
            }

            int TaskIconSizeHalf = TaskIconSize / 2;

            String TaskWorldPointCountSize = String.valueOf(TaskCount);
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
            for (HashMap.Entry<UUID, Boolean > mapElement : CurrentDisplayPoint.Tasks.entrySet())
            {
                if (config.UserData.PlannedTasks.containsKey(mapElement.getKey()))
                {
                    bIsTaskPlanned = true;
                }
            }

            BufferedImage DrawImage = null;
            int HighlightedSize = (int) (TaskIconSize * 1.5f);
            int HighlightedHalf = HighlightedSize / 2;

            boolean bIsHighlighted = false;
            if (!bSetHighlighted && HighlightGraphicsPoint.distanceTo(GraphicsPoint) < TaskIconSize)
            {
                bIsHighlighted = true;
                bSetHighlighted = true;
                if (CurrentDisplayPoint.Tasks.size() == CurrentDisplayPoint.DungeonTasks.size())
                {
                    if (bIsTaskPlanned)
                    {
                        DrawImage = PLANNED_HIGHLIGHTED_TASK_IMAGE_DUNGEON;
                    }
                    else
                    {
                        DrawImage = HIGHLIGHTED_TASK_IMAGE_DUNGEON;
                    }
                }
                else
                {
                    if (bIsTaskPlanned)
                    {
                        DrawImage = PLANNED_HIGHLIGHTED_TASK_IMAGE;
                    }
                    else
                    {
                        DrawImage = HIGHLIGHTED_TASK_IMAGE;
                    }
                }

                if (OverrideImage != null)
                {
                    graphics.drawImage(PosiedienLeaguesPlannerPlugin.BOUNDS_SELECTED, (int) GraphicsPoint.getX() - HighlightedHalf, (int) GraphicsPoint.getY()  - HighlightedHalf, HighlightedSize, HighlightedSize, null);
                }
            }
            else
            {
                if (CurrentDisplayPoint.Tasks.size() == CurrentDisplayPoint.DungeonTasks.size())
                {
                    if (bIsTaskPlanned)
                    {
                        DrawImage = PLANNED_TASK_IMAGE_DUNGEON;
                        if (OverrideImage != null)
                        {
                            graphics.drawImage(PosiedienLeaguesPlannerPlugin.BOUNDS_SELECTED, (int) GraphicsPoint.getX() - HighlightedHalf, (int) GraphicsPoint.getY()  - HighlightedHalf, HighlightedSize, HighlightedSize, null);
                        }
                    }
                    else
                    {
                        DrawImage = TASK_IMAGE_DUNGEON;
                    }
                }
                else
                {
                    if (bIsTaskPlanned)
                    {
                        DrawImage = PLANNED_TASK_IMAGE;
                        if (OverrideImage != null)
                        {
                            graphics.drawImage(PosiedienLeaguesPlannerPlugin.BOUNDS_SELECTED, (int) GraphicsPoint.getX() - HighlightedHalf, (int) GraphicsPoint.getY()  - HighlightedHalf, HighlightedSize, HighlightedSize, null);
                        }
                    }
                    else
                    {
                        DrawImage = TASK_IMAGE;
                    }
                }
            }

            if (OverrideImage != null)
            {
                DrawImage = OverrideImage;
            }

            graphics.drawImage(DrawImage, GraphicsPoint.getX() - TaskIconSizeHalf, GraphicsPoint.getY() - TaskIconSizeHalf, TaskIconSize, TaskIconSize, null);

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
            int TextIconTextOffsetY = -TaskIconSizeHalf / 2;
            int TextIconTextOffsetX = (TaskIconSizeHalf / 4);

            QueuedTextCommands.add(new OverlayQueuedText(TaskWorldPointCountSize,
                    0,
                    GraphicsPoint.getX() - TextIconTextOffsetX,
                    GraphicsPoint.getY() - TextIconTextOffsetY,
                    taskIconFont,
                    taskIconFontColor));

            boolean bIsCloseToMouse = bIsHighlighted;
            if (bIsTaskPlanned || bIsCloseToMouse)
            {
                Font taskhighlightFont2 = new FontUIResource("taskhighlightFont2", Font.BOLD, 15);

                int TaskNum2 = 0;
                for (HashMap.Entry<UUID, Boolean > mapElement : CurrentDisplayPoint.Tasks.entrySet())
                {
                    TaskData CurrentTask = plugin.GetTaskData(mapElement.getKey(), mapElement.getValue());
                    String ModifiedString = CurrentTask.TaskName;

                    if (config.UserData.PlannedTasks.containsKey(CurrentTask.GUID))
                    {
                        ModifiedString += " (" + config.UserData.PlannedTasks.get(CurrentTask.GUID).LastSortOrder + ")";
                    }

                    if (bIsCloseToMouse)
                    {
                        QueuedTextCommands.add(new OverlayQueuedText(ModifiedString,
                                0,
                                HighlightGraphicsPoint.getX() - 1,
                                HighlightGraphicsPoint.getY() - TaskNum2 * 15 + 1,
                                taskhighlightFont2,
                                highlightnamecolor2));

                        ++TaskNum2;
                    }
                    // bIsTaskPlanned
                    else if (config.UserData.PlannedTasks.containsKey(CurrentTask.GUID))
                    {
                        if (config.UserData.PlannedTasks.get(CurrentTask.GUID).SortPriority <= MaxSortPriority)
                        {
                            QueuedTextCommands.add(new OverlayQueuedText(ModifiedString,
                                    0,
                                    GraphicsPoint.getX() - 1,
                                    GraphicsPoint.getY() - TaskNum2 * 15 + 1,
                                    taskhighlightFont2,
                                    highlightnamecolor2));

                            ++TaskNum2;
                        }
                    }
                }
                Font taskhighlightFont = new FontUIResource("taskhighlightFont", Font.BOLD, 15);

                int TaskNum = 0;
                for (HashMap.Entry<UUID, Boolean > mapElement : CurrentDisplayPoint.Tasks.entrySet())
                {
                    TaskData CurrentTask = plugin.GetTaskData(mapElement.getKey(), mapElement.getValue());
                    String ModifiedString = CurrentTask.TaskName;

                    if (config.UserData.PlannedTasks.containsKey(CurrentTask.GUID))
                    {
                        ModifiedString += " (" + config.UserData.PlannedTasks.get(CurrentTask.GUID).LastSortOrder + ")";
                    }

                    if (bIsCloseToMouse)
                    {
                        QueuedTextCommands.add(new OverlayQueuedText(ModifiedString,
                                0,
                                HighlightGraphicsPoint.getX(),
                                HighlightGraphicsPoint.getY() - TaskNum * 15,
                                taskhighlightFont,
                                TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));

                        ++TaskNum;
                    }
                    // bIsTaskPlanned
                    else if (config.UserData.PlannedTasks.containsKey(CurrentTask.GUID))
                    {
                        if (config.UserData.PlannedTasks.get(CurrentTask.GUID).SortPriority <= MaxSortPriority)
                        {
                            QueuedTextCommands.add(new OverlayQueuedText(ModifiedString,
                                    0,
                                    GraphicsPoint.getX(),
                                    GraphicsPoint.getY() - TaskNum * 15,
                                    taskhighlightFont,
                                    TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));

                            ++TaskNum;
                        }
                    }
                }
            }
        }

        // Go through our plan and draw lines connecting them
        Player player = plugin.client.getLocalPlayer();
        ArrayList<Pathfinder> pathfinderArray = plugin.panel.getPathfinderArray();
        if (player != null)
        {
            WorldPoint LastWorldPoint = player.getWorldLocation();
            WorldPoint LastActualWorldPoint = player.getWorldLocation();

            int Iter = 0;
            int SortTaskIter = 0;
            for (SortedTask SortedTaskIter : config.UserData.SortedPlannedTasks)
            {
                graphics.setColor(new Color(255, 226, 1, Math.max(0, 255 - Iter * 50)));

                ++Iter;
                if (OthFilter == OtherFilter.NEXT_5_TASKS)
                {
                    if (SortTaskIter > 4)
                    {
                        break;
                    }
                }

                TaskData CurrentTask = plugin.GetTaskData(SortedTaskIter.TaskGUID, SortedTaskIter.bIsCustomTask);

                // Find our display points with this task
                float ClosestDistance = 9000000.0f;
                float ClosestActualDistance = 9000000.0f;
                WorldPoint ClosestWorldPoint = null;
                WorldPoint ClosestActualWorldPoint = null;
                if (CurrentTask == null || !TaskToDisplayPoints.containsKey(CurrentTask.GUID))
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

                if (plugin.panel.CurrentPathfinderIndex == SortTaskIter && pathfinderArray != null)
                {
                    boolean bIsCurrentPathfindingDone = false;
                    synchronized (plugin.panel.pathfinderMutex)
                    {
                        bIsCurrentPathfindingDone = pathfinderArray.isEmpty() ||
                                pathfinderArray.get(SortTaskIter - 1).isDone();
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

        for (OverlayQueuedText CurrentTextCommand :QueuedTextCommands)
        {
            graphics.setFont(CurrentTextCommand.FontValue);
            graphics.setColor(CurrentTextCommand.ColorValue);
            graphics.drawChars(CurrentTextCommand.TextValue.toCharArray(),
                    CurrentTextCommand.Offset,
                    CurrentTextCommand.TextValue.length(),
                    CurrentTextCommand.X,
                    CurrentTextCommand.Y);
        }

        return null;
    }
}
