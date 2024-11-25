package Posiedien_Leagues_Planner;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provides;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import Posiedien_Leagues_Planner.pathfinder.CollisionMap;
import Posiedien_Leagues_Planner.pathfinder.Pathfinder;
import Posiedien_Leagues_Planner.pathfinder.PathfinderConfig;
import Posiedien_Leagues_Planner.pathfinder.SplitFlagMap;

import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "Posiedien Leagues Planner",
        description = "The plugin allows people to put in their own custom tasks or hook into tasks I've manually added into the planning.",
        tags = {"leagues", "trailblazer", "posiedien", "planning", "pathfinder", "map", "waypoint", "navigation"}
)

public class PosiedienLeaguesPlannerPlugin extends Plugin {
    protected static final String CONFIG_GROUP = "Posiedien_Leagues_Planner";
    private static final String ADD_START = "Add start";
    private static final String ADD_END = "Add end";
    private static final String CLEAR = "Clear";
    private static final String PATH = ColorUtil.wrapWithColorTag("Path", JagexColors.MENU_TARGET);
    private static final String SET = "Set";
    private static final String START = ColorUtil.wrapWithColorTag("Pathfinding Start", JagexColors.MENU_TARGET);
    private static final String TARGET = ColorUtil.wrapWithColorTag("Pathfinding Target", JagexColors.MENU_TARGET);
    private static final String TRANSPORT = ColorUtil.wrapWithColorTag("Transport", JagexColors.MENU_TARGET);
    private static final String WALK_HERE = "Walk here";
    private static final BufferedImage MARKER_IMAGE = ImageUtil.loadImageResource(PosiedienLeaguesPlannerPlugin.class, "/marker.png");
    private static final BufferedImage TASK_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/TaskIcon.png");
    public boolean enableAutoRecalculate = false;

    @Inject
    public Client client;
    public boolean bMapDisplayPointsDirty;

    @Getter
    @Inject
    ConfigManager configManager;


    @Inject
    private MouseManager mouseManager;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    public LeaguesPlannerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PathTileOverlay pathOverlay;

    @Inject
    private PathMinimapOverlay pathMinimapOverlay;

    @Inject
    private PathMapOverlay pathMapOverlay;

    @Inject
    private PathMapTooltipOverlay pathMapTooltipOverlay;

    @Inject
    private DebugOverlayPanel debugOverlayPanel;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    private Point lastMenuOpenedPoint;
    private WorldMapPoint marker;
    private WorldPoint transportStart;
    private WorldPoint lastLocation = new WorldPoint(0, 0, 0);
    private MenuEntry lastClick;
    private Shape minimapClipFixed;
    private Shape minimapClipResizeable;
    private BufferedImage minimapSpriteFixed;
    private BufferedImage minimapSpriteResizeable;
    private Rectangle minimapRectangle = new Rectangle();

    private ExecutorService pathfindingExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService initExecutor = Executors.newSingleThreadExecutor();
    private Future<?> pathfinderFuture;
    private final Object pathfinderMutex = new Object();
    @Getter
    private Pathfinder pathfinder;
    public PathfinderConfig pathfinderConfig;
    @Getter
    private boolean startPointSet = false;

    public LeaguesPlannerPanel panel;
    public NavigationButton navButton;

    public volatile boolean bIsInitializing = false;


    private boolean isTaskWindowOpen()
    {
        Widget widget = client.getWidget(657, 10);
        return widget != null && !widget.isHidden();
    }

    public void InitializeFromOtherThread()
    {
        taskOverlay = new TaskOverlay(client, this, config);
        taskOverlay.worldMapOverlay = worldMapOverlay;

        Map<WorldPoint, List<Transport>> transports = Transport.loadAllFromResources();
        if (bCalculateOverworldPositions)
        {
            SplitFlagMap map = SplitFlagMap.fromResources(null, null);
            pathfinderConfig = new PathfinderConfig(map, transports, client, config);
        }

        overlayManager.add(pathOverlay);
        overlayManager.add(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
        overlayManager.add(pathMapTooltipOverlay);

        if (config.drawDebugPanel()) {
            overlayManager.add(debugOverlayPanel);
        }

        try {
            InitializeRegionData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            InitializeTaskData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        InitializeCustomIconsMap();

        mouseManager.registerMouseListener(MouseListenerObject);

        SplitFlagMap NewMap = SplitFlagMap.fromResources(this, config);
        pathfinderConfig = new PathfinderConfig(NewMap, transports, client, config);

        EventQueue.invokeLater(() ->
        {
            panel = new LeaguesPlannerPanel(this);
            navButton = NavigationButton.builder()
                    .tooltip("Posiedien's Leagues Planner")
                    .icon(TASK_IMAGE)
                    .priority(7)
                    .panel(panel)
                    .build();
            clientToolbar.addNavigation(navButton);
            panel.refresh();

            bIsInitializing = false;
        });
    }

    @Override
    protected void startUp() throws Exception
    {
        bIsInitializing = true;

        PluginInitializer newPluginInitializer = new PluginInitializer(this);

        if (initExecutor == null)
        {
            initExecutor = Executors.newSingleThreadExecutor();
        }
        initExecutor.submit(newPluginInitializer);
    }

    @Override
    protected void shutDown() throws IOException
    {
        SaveRegionBounds();
        SaveTaskData();

        ShutdownRegionData();
        ShutdownTaskData();

        overlayManager.remove(pathOverlay);
        overlayManager.remove(pathMinimapOverlay);
        overlayManager.remove(pathMapOverlay);
        overlayManager.remove(pathMapTooltipOverlay);
        overlayManager.remove(debugOverlayPanel);

        mouseManager.unregisterMouseListener(MouseListenerObject);

        if (initExecutor != null) {
            initExecutor.shutdownNow();
            initExecutor = null;
        }


        if (pathfindingExecutor != null) {
            pathfindingExecutor.shutdownNow();
            pathfindingExecutor = null;
        }

        if (panel.pathfindingExecutor != null) {
            panel.pathfindingExecutor.shutdownNow();
            panel.pathfindingExecutor = null;
        }

        clientToolbar.removeNavigation(navButton);

        taskOverlay = null;
        panel = null;
        navButton = null;
    }

    private void SaveTaskData() throws IOException
    {
        if (bSaveTaskData)
        {
            // Task info
            File targ = new File("TaskData.csv");
            config.TaskData.exportToConverted(targ, this);
        }

        {
            // Task info
            File targ = new File("UserData.csv");
            config.UserData.exportTo(targ);
        }
    }

    public boolean bQueuedPathfinderTask = false;
    public void restartPathfinding(WorldPoint start, WorldPoint end, boolean bJustFindOverworld)
    {
        synchronized (pathfinderMutex) {
            if (pathfinder != null) {
                bQueuedPathfinderTask = true;
                pathfinder.cancel();
                pathfinderFuture.cancel(true);
            }

            if (pathfindingExecutor == null) {
                ThreadFactory shortestPathNaming = new ThreadFactoryBuilder().setNameFormat("shortest-path-%d").build();
                pathfindingExecutor = Executors.newSingleThreadExecutor(shortestPathNaming);
            }
        }

        getClientThread().invokeLater(() -> {
            bQueuedPathfinderTask = false;
            pathfinderConfig.refresh();
            synchronized (pathfinderMutex)
            {
                pathfinder = new Pathfinder(pathfinderConfig, start, end, bJustFindOverworld);
                pathfinderFuture = pathfindingExecutor.submit(pathfinder);
            }
        });
    }

    public boolean isNearPath(WorldPoint location) {
        if (pathfinder == null || pathfinder.getPath() == null || pathfinder.getPath().isEmpty() ||
            config.recalculateDistance() < 0 || lastLocation.equals(lastLocation = location)) {
            return true;
        }

        for (WorldPoint point : pathfinder.getPath()) {
            if (location.distanceTo2D(point) < config.recalculateDistance()) {
                return true;
            }
        }

        return false;
    }

    private final Pattern TRANSPORT_OPTIONS_REGEX = Pattern.compile("^(avoidWilderness|use\\w+)$");

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (bIsInitializing)
        {
            return;
        }

        CurrentRegion = GetRegionBounds(config.GetEditRegion());
        if (event.getKey().equals("GetEditRegion"))
        {
            if (config.GetEditRegion() == RegionType.NONE)
            {
                worldMapPointManager.removeIf(x -> x.getName() != null && x.getName().contains("LP: Region Bounds:"));
            }
            else
            {
                // Add all the serialized markers
                for (LeagueRegionBounds LocalCurrentRegion : config.RegionData.RegionData)
                {
                    LocalCurrentRegion.RegionPoints.forEach((key, value) ->
                    {
                        SetMarkerActivation(value, false);
                    });
                }
            }
        }

        if (!CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        if ("drawDebugPanel".equals(event.getKey())) {
            if (config.drawDebugPanel()) {
                overlayManager.add(debugOverlayPanel);
            } else {
                overlayManager.remove(debugOverlayPanel);
            }
            return;
        }

        // Transport option changed; rerun pathfinding
        if (TRANSPORT_OPTIONS_REGEX.matcher(event.getKey()).find()) {
            if (pathfinder != null) {
                restartPathfinding(pathfinder.getStart(), pathfinder.getTarget(), false);
            }
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        if (bIsInitializing)
        {
            return;
        }

        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (map == null)
        {
            return;
        }

        if (!map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
        {
            return;
        }

        CurrentRegion = GetRegionBounds(config.GetEditRegion());
        AddRightClickMenuEntries(event);

        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    public void QueueRefresh()
    {
        if (bIsInitializing)
        {
            return;
        }

        EventQueue.invokeLater(() -> {
            if (panel != null)
            {
                panel.refresh();
            }
            bMapDisplayPointsDirty = true;
                }
        );
    }

    Player CachedPlayer = null;
    float Timer = 0.0f;
    double SinceLastInputTimer = 0.0f;

    // Taken from osleague-runelite-plugin
    private boolean isTaskCompleted(Widget taskLabel)
    {
        return taskLabel.getTextColor() != 0x9f9f9f;
    }

    // Taken from osleague-runelite-plugin
    private void MarkCompletedTasksFromList()
    {
        Widget taskLabelsWidget = client.getWidget(657, 10);
        Widget taskPointsWidget = client.getWidget(657, 11);
        Widget taskDifficultiesWidget = client.getWidget(657, 16);
        if (taskLabelsWidget == null || taskPointsWidget == null || taskDifficultiesWidget == null)
        {
            return;
        }

        Widget[] taskLabels = taskLabelsWidget.getDynamicChildren();
        Widget[] taskPoints = taskPointsWidget.getDynamicChildren();
        Widget[] taskDifficulties = taskDifficultiesWidget.getDynamicChildren();
        if (taskLabels.length != taskPoints.length || taskPoints.length != taskDifficulties.length)
        {
            return;
        }

        for (Widget taskLabel : taskLabels)
        {
            String name = taskLabel.getText();

            // Find our task from the name
            for (Map.Entry<UUID, TaskData> SearchingTask : config.TaskData.LeaguesTaskList.entrySet())
            {
                if (SearchingTask.getValue().TaskName.contains(name))
                {
                    if (isTaskCompleted(taskLabel))
                    {
                        config.UserData.CompletedTasks.add(SearchingTask.getKey());
                    }
                }
            }
        }
    }

    boolean bWasTaskWindowOpen = false;

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (bIsInitializing)
        {
            return;
        }
        Player localPlayer = client.getLocalPlayer();

        SinceLastInputTimer += 0.6f;
        if (enableAutoRecalculate)
        {
            // Each tick is around 0.6 seconds according to docs
            Timer += 0.6f;
            if (Timer > 5.0f)
            {
                Timer = 0.0f;
                QueueRefresh();
            }
        }

        if (isTaskWindowOpen())
        {
            if (!bWasTaskWindowOpen)
            {
                MarkCompletedTasksFromList();
            }
            bWasTaskWindowOpen = true;
        }
        else
        {
            bWasTaskWindowOpen = false;
        }

        CachedPlayer = localPlayer;
        if (localPlayer == null || pathfinder == null)
        {
            return;
        }

        WorldPoint currentLocation = client.isInInstancedRegion() ?
            WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()) : localPlayer.getWorldLocation();
        if (currentLocation.distanceTo(pathfinder.getTarget()) < config.reachedDistance()) {
            setTarget(null);
            return;
        }

        if (!startPointSet && !isNearPath(currentLocation)) {
            if (config.cancelInstead()) {
                setTarget(null);
                return;
            }
            restartPathfinding(currentLocation, pathfinder.getTarget(), false);
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (bIsInitializing)
        {
            return;
        }

        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals(WALK_HERE) && event.getTarget().isEmpty()) {
            if (config.drawTransports()) {
                addMenuEntry(event, ADD_START, TRANSPORT, 1);
                addMenuEntry(event, ADD_END, TRANSPORT, 1);
                // addMenuEntry(event, "Copy Position");
            }

            addMenuEntry(event, SET, TARGET, 1);
            if (pathfinder != null) {
                if (pathfinder.getTarget() != null) {
                    addMenuEntry(event, SET, START, 1);
                }
                WorldPoint selectedTile = getSelectedWorldPoint();
                if (pathfinder.getPath() != null) {
                    for (WorldPoint tile : pathfinder.getPath()) {
                        if (tile.equals(selectedTile)) {
                            addMenuEntry(event, CLEAR, PATH, 1);
                            break;
                        }
                    }
                }
            }
        }

        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

        if (map != null && map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            addMenuEntry(event, SET, TARGET, 0);
            if (pathfinder != null) {
                if (pathfinder.getTarget() != null) {
                    addMenuEntry(event, SET, START, 0);
                    addMenuEntry(event, CLEAR, PATH, 0);
                }
            }
        }

        final Shape minimap = getMinimapClipArea();

        if (minimap != null && pathfinder != null &&
            minimap.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
            addMenuEntry(event, CLEAR, PATH, 0);
        }

        if (minimap != null && pathfinder != null &&
            ("Floating World Map".equals(Text.removeTags(event.getOption())) ||
             "Close Floating panel".equals(Text.removeTags(event.getOption())))) {
            addMenuEntry(event, CLEAR, PATH, 1);
        }
    }

    public Map<WorldPoint, List<Transport>> getTransports() {
        return pathfinderConfig.getTransports();
    }

    public CollisionMap getMap() {
        return pathfinderConfig.getMap();
    }

    public Area ObtainWorldMapClipArea(Rectangle baseRectangle)
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

    MouseListener MouseListenerObject = new MouseListener()
    {
        @Override
        public MouseEvent mouseClicked(MouseEvent e)
        {
            if (CustomTask_ChangingIcon == null)
            {
                return e;
            }

            if (SinceLastInputTimer < 0.7f)
            {
                return e;
            }

            getClientThread().invokeLater(() ->
            {
                Area WorldMapClipArea = ObtainWorldMapClipArea(Objects.requireNonNull(client.getWidget(WidgetInfo.WORLD_MAP_VIEW)).getBounds());
                Point HighlightGraphicsPoint = new Point(0,0);
                if (getSelectedWorldPoint() != null)
                {
                    HighlightGraphicsPoint = worldMapOverlay.mapWorldPointToGraphicsPoint(getSelectedWorldPoint());
                    if (HighlightGraphicsPoint == null)
                    {
                        HighlightGraphicsPoint = new Point(0,0);
                    }
                }

                // Render all of our options centered around the center of the screen
                int TotalIconCount = CustomIconsMap.size();
                int IconSize = 30;
                int IconSizeHalf = IconSize / 2;

                Rectangle2D Bounds = WorldMapClipArea.getBounds2D();

                int RowAndHeightSize = (int) Math.sqrt(TotalIconCount);
                int i = 0;
                for (Map.Entry<String, BufferedImage> CustomIcon : CustomIconsMap.entrySet())
                {
                    int OffsetX = (int) (Bounds.getCenterX() - (RowAndHeightSize / 2) * IconSize);
                    int OffsetY = (int) (Bounds.getCenterY() - (RowAndHeightSize / 2) * IconSize);

                    OffsetX += ((i % RowAndHeightSize) * IconSize);
                    OffsetY += ((i / RowAndHeightSize) * IconSize);

                    Point IconPoint = new Point(OffsetX, OffsetY);
                    if (HighlightGraphicsPoint.distanceTo(IconPoint) < IconSizeHalf)
                    {
                        CustomTask_ChangingIcon.CustomIcon = CustomIcon.getKey();
                        break;
                    }

                    ++i;
                }

                CustomTask_ChangingIcon = null;
            });
            return e;
        }

        @Override
        public MouseEvent mousePressed(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public MouseEvent mouseReleased(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public MouseEvent mouseEntered(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public MouseEvent mouseExited(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public MouseEvent mouseDragged(MouseEvent mouseEvent) {
            return mouseEvent;
        }

        @Override
        public MouseEvent mouseMoved(MouseEvent mouseEvent) {
            return mouseEvent;
        }
    };

    private void onMenuOptionClicked(MenuEntry entry) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        WorldPoint currentLocation = client.isInInstancedRegion() ?
            WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()) : localPlayer.getWorldLocation();
        if (entry.getOption().equals(ADD_START) && entry.getTarget().equals(TRANSPORT)) {
            transportStart = currentLocation;
        }

        if (entry.getOption().equals(ADD_END) && entry.getTarget().equals(TRANSPORT)) {
            WorldPoint transportEnd = client.isInInstancedRegion() ?
                WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()) : localPlayer.getWorldLocation();
            System.out.println(transportStart.getX() + " " + transportStart.getY() + " " + transportStart.getPlane() + " " +
                    currentLocation.getX() + " " + currentLocation.getY() + " " + currentLocation.getPlane() + " " +
                    lastClick.getOption() + " " + Text.removeTags(lastClick.getTarget()) + " " + lastClick.getIdentifier()
            );
            Transport transport = new Transport(transportStart, transportEnd);
            pathfinderConfig.getTransports().computeIfAbsent(transportStart, k -> new ArrayList<>()).add(transport);
        }

        if (entry.getOption().equals("Copy Position")) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection("(" + currentLocation.getX() + ", "
                            + currentLocation.getY() + ", "
                            + currentLocation.getPlane() + ")"), null);
        }

        if (entry.getOption().equals(SET) && entry.getTarget().equals(TARGET)) {
            setTarget(getSelectedWorldPoint());
        }

        if (entry.getOption().equals(SET) && entry.getTarget().equals(START)) {
            setStart(getSelectedWorldPoint());
        }

        if (entry.getOption().equals(CLEAR) && entry.getTarget().equals(PATH)) {
            setTarget(null);
        }

        if (entry.getType() != MenuAction.WALK) {
            lastClick = entry;
        }
    }

    public WorldPoint getSelectedWorldPoint() {
        if (client.getWidget(WidgetInfo.WORLD_MAP_VIEW) == null) {
            if (client.getSelectedSceneTile() != null) {
                return client.isInInstancedRegion() ?
                    WorldPoint.fromLocalInstance(client, client.getSelectedSceneTile().getLocalLocation()) :
                    client.getSelectedSceneTile().getWorldLocation();
            }
        } else {
            return calculateMapPoint(client.isMenuOpen() ? lastMenuOpenedPoint : client.getMouseCanvasPosition());
        }
        return null;
    }

    private void setTarget(WorldPoint target) {
        Player localPlayer = client.getLocalPlayer();
        if (!startPointSet && localPlayer == null) {
            return;
        }

        if (target == null) {
            synchronized (pathfinderMutex) {
                if (pathfinder != null) {
                    pathfinder.cancel();
                }
                pathfinder = null;
            }

            worldMapPointManager.remove(marker);
            marker = null;
            startPointSet = false;
        } else {
            worldMapPointManager.removeIf(x -> x == marker);
            marker = new WorldMapPoint(target, MARKER_IMAGE);
            marker.setName("Target");
            marker.setTarget(marker.getWorldPoint());
            marker.setJumpOnClick(true);
            worldMapPointManager.add(marker);

            WorldPoint start = client.isInInstancedRegion() ?
                WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation()) : localPlayer.getWorldLocation();
            lastLocation = start;
            if (startPointSet && pathfinder != null) {
                start = pathfinder.getStart();
            }
            restartPathfinding(start, target, false);
        }
    }

    private void setStart(WorldPoint start) {
        if (pathfinder == null) {
            return;
        }
        startPointSet = true;
        restartPathfinding(start, pathfinder.getTarget(), false);
    }

    public WorldPoint calculateMapPoint(Point point)
    {
        WorldMap worldMap = client.getWorldMap();
        float zoom = worldMap.getWorldMapZoom();
        final WorldPoint mapPoint = new WorldPoint(worldMap.getWorldMapPosition().getX(), worldMap.getWorldMapPosition().getY(), 0);
        final Point middle = mapWorldPointToGraphicsPoint(mapPoint);

        if (point == null || middle == null) {
            return null;
        }

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    public Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint) {
        WorldMap worldMap = client.getWorldMap();

        float pixelsPerTile = worldMap.getWorldMapZoom();

        Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (map != null) {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            Point worldMapPosition = worldMap.getWorldMapPosition();

            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
            xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff, yGraphDiff);
        }
        return null;
    }

    private void addMenuEntry(MenuEntryAdded event, String option, String target, int position) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenuEntries()));

        if (entries.stream().anyMatch(e -> e.getOption().equals(option) && e.getTarget().equals(target))) {
            return;
        }

        client.createMenuEntry(position)
            .setOption(option)
            .setTarget(target)
            .setParam0(event.getActionParam0())
            .setParam1(event.getActionParam1())
            .setIdentifier(event.getIdentifier())
            .setType(MenuAction.RUNELITE)
            .onClick(this::onMenuOptionClicked);
    }

    private Widget getMinimapDrawWidget() {
        if (client.isResized()) {
            if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1) {
                return client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_DRAW_AREA);
            }
            return client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_STONES_DRAW_AREA);
        }
        return client.getWidget(WidgetInfo.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
    }

    private Shape getMinimapClipAreaSimple() {
        Widget minimapDrawArea = getMinimapDrawWidget();

        if (minimapDrawArea == null || minimapDrawArea.isHidden()) {
            return null;
        }

        Rectangle bounds = minimapDrawArea.getBounds();

        return new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public Shape getMinimapClipArea() {
        Widget minimapWidget = getMinimapDrawWidget();

        if (minimapWidget == null || minimapWidget.isHidden() || !minimapRectangle.equals(minimapRectangle = minimapWidget.getBounds())) {
            minimapClipFixed = null;
            minimapClipResizeable = null;
            minimapSpriteFixed = null;
            minimapSpriteResizeable = null;
        }

        if (client.isResized()) {
            if (minimapClipResizeable != null) {
                return minimapClipResizeable;
            }
            if (minimapSpriteResizeable == null) {
                minimapSpriteResizeable = spriteManager.getSprite(SpriteID.RESIZEABLE_MODE_MINIMAP_ALPHA_MASK, 0);
            }
            if (minimapSpriteResizeable != null) {
                minimapClipResizeable = bufferedImageToPolygon(minimapSpriteResizeable);
                return minimapClipResizeable;
            }
            return getMinimapClipAreaSimple();
        }
        if (minimapClipFixed != null) {
            return minimapClipFixed;
        }
        if (minimapSpriteFixed == null) {
            minimapSpriteFixed = spriteManager.getSprite(SpriteID.FIXED_MODE_MINIMAP_ALPHA_MASK, 0);
        }
        if (minimapSpriteFixed != null) {
            minimapClipFixed = bufferedImageToPolygon(minimapSpriteFixed);
            return minimapClipFixed;
        }
        return getMinimapClipAreaSimple();
    }

    private Polygon bufferedImageToPolygon(BufferedImage image) {
        Color outsideColour = null;
        Color previousColour;
        final int width = image.getWidth();
        final int height = image.getHeight();
        List<java.awt.Point> points = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            previousColour = outsideColour;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb & 0xff000000) >>> 24;
                int r = (rgb & 0x00ff0000) >> 16;
                int g = (rgb & 0x0000ff00) >> 8;
                int b = (rgb & 0x000000ff) >> 0;
                Color colour = new Color(r, g, b, a);
                if (x == 0 && y == 0) {
                    outsideColour = colour;
                    previousColour = colour;
                }
                if (!colour.equals(outsideColour) && previousColour.equals(outsideColour)) {
                    points.add(new java.awt.Point(x, y));
                }
                if ((colour.equals(outsideColour) || x == (width - 1)) && !previousColour.equals(outsideColour)) {
                    points.add(0, new java.awt.Point(x, y));
                }
                previousColour = colour;
            }
        }
        int offsetX = minimapRectangle.x;
        int offsetY = minimapRectangle.y;
        Polygon polygon = new Polygon();
        for (java.awt.Point point : points) {
            polygon.addPoint(point.x + offsetX, point.y + offsetY);
        }
        return polygon;
    }


    public LeagueRegionBounds CurrentRegion = null;
    public LeagueRegionPoint LastClickedRegionPoint = null;
    @Inject
    public RegionBoundOverlay regionBoundOverlay = new RegionBoundOverlay(client, this, config);

    @Inject
    public TaskOverlay taskOverlay = null;

    public LeagueRegionPoint CurrentFocusedPoint;


    // Debug flags for saving/loading new data
    private boolean bLoadRawWikiData = true;
    private boolean bCalculateOverworldPositions = false;
    private boolean bSaveTaskData = false;
    private boolean bSaveRegionData = false;

    void SaveRegionBounds() throws IOException
    {
        if (bSaveRegionData)
        {
            File targ = new File("RegionBoundData.csv");
            config.RegionData.exportTo(targ);
        }
    }

    void LoadRegionBounds() throws IOException
    {
        File targ = new File("RegionBoundData.csv");
        config.RegionData.importFrom(targ);
    }

    void LoadTaskData() throws IOException
    {
        config.TaskData.LeaguesTaskList.clear();
        config.TaskData.StringToTask.clear();

        {
            // Converted task data
            File targ = new File("ConvertedTaskData/TrailblazerTaskData.csv");
            config.TaskData.importFromConverted(targ);
        }

        {
            // Task info, Shattered
            //File targ = new File("ConvertedTaskData/ShatteredTaskData.csv");
            //config.TaskData.importFromConverted(targ);
        }

        if (bLoadRawWikiData)
        {
            config.TaskData.importFromRaw();
        }

        if (bCalculateOverworldPositions)
        {
            config.TaskData.CalculateAndCacheOverworldLocations(this);
        }

        {
            // Custom Task/Plan info
            File targ = new File("UserData.csv");
            config.UserData.importFrom(targ);
        }

    }

    private boolean GatherRegionBounds(WorldPointPolygon poly, ArrayList<RegionLine> regionLines, Set<UUID> VisitedPoints, LeagueRegionPoint nextPoint, LeagueRegionPoint parentPoint)
    {
        if (VisitedPoints.contains(nextPoint.GUID))
        {
            // Connected back!
            return true;
        }
        VisitedPoints.add(nextPoint.GUID);
        WorldPoint end = nextPoint.OurWorldPoint;
        poly.AddWorldPoint(end);

        if (parentPoint != null)
        {
            WorldPoint start = parentPoint.OurWorldPoint;
            regionLines.add(new RegionLine(start, end));
        }

        // Recursively visit all the points and draw a polygon if one exists
        for (LeagueRegionPoint connectedPoint : nextPoint.ConnectedPoints)
        {
            // don't go backwards
            if (parentPoint == connectedPoint)
            {
                continue;
            }

            if (GatherRegionBounds(poly, regionLines, VisitedPoints, connectedPoint, nextPoint))
            {
                // Connected back!
                return true;
            }
        }

        return false;
    }

    private void RefreshRegionBounds()
    {
        Set<UUID> VisitedPoints = new HashSet<>();
        for (LeagueRegionBounds regionDatum : config.RegionData.RegionData)
        {
            Color DrawColor = RegionType.GetRegionColor(config, regionDatum.Type, true);
            regionDatum.RegionPolygons.clear();
            regionDatum.RegionLines.clear();

            regionDatum.RegionPoints.forEach((key, value) ->
            {
                // Early out
                if (VisitedPoints.contains(value.GUID))
                {
                    return;
                }

                WorldPointPolygon newPolygon = new WorldPointPolygon();

                if (GatherRegionBounds(newPolygon, regionDatum.RegionLines, VisitedPoints, value, null))
                {
                    regionDatum.RegionPolygons.add(newPolygon);
                    newPolygon.CacheWorldPoly();
                }
            });
        }
    }

    public void InitializeRegionData() throws Exception
    {
        config.RegionData.RegionData.clear();
        for (RegionType CurrentRegion : RegionType.values())
        {
            if (CurrentRegion == RegionType.NONE)
            {
                continue;
            }

            config.RegionData.RegionData.add(new LeagueRegionBounds(CurrentRegion));
        }

        LoadRegionBounds();
        // Add all the serialized markers
        for (LeagueRegionBounds LocalCurrentRegion : config.RegionData.RegionData)
        {
            LocalCurrentRegion.RegionPoints.forEach((key, value) ->
            {
                SetMarkerActivation(value, false);
            });
        }

        RefreshRegionBounds();
        overlayManager.add(regionBoundOverlay);
    }

    public void ShutdownRegionData()
    {
        overlayManager.remove(regionBoundOverlay);
        worldMapPointManager.removeIf(x -> x.getName() != null && x.getName().contains("LP:"));
    }

    public LeagueRegionBounds GetRegionBounds(RegionType Type)
    {
        for (LeagueRegionBounds CurrentBounds : config.RegionData.RegionData)
        {
            if (CurrentBounds.Type == Type)
            {
                return CurrentBounds;
            }
        }

        return null;
    }


    public void InitializeTaskData() throws Exception
    {
        config.TaskData.LeaguesTaskList.clear();
        LoadTaskData();

        overlayManager.add(taskOverlay);
    }

    public void ShutdownTaskData()
    {
        overlayManager.remove(taskOverlay);
    }

    public WorldPoint LastDisplayedWorldPoint;

    private static final BufferedImage ACTIVE_MARKER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/activemarker.png");

    public static final BufferedImage BOUNDS_SELECTED = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Selected.png");
    private static final BufferedImage BOUNDS_MISTHALIN = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Misthalin.png");
    private static final BufferedImage BOUNDS_KARAMJA = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Karamja.png");
    private static final BufferedImage BOUNDS_KANDARIN = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Kandarin.png");
    private static final BufferedImage BOUNDS_ASGARNIA = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Asgarnia.png");
    private static final BufferedImage BOUNDS_FREMENNIK = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Fremennik.png");
    private static final BufferedImage BOUNDS_KOUREND = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Kourend.png");
    private static final BufferedImage BOUNDS_WILDERNESS = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Wilderness.png");
    private static final BufferedImage BOUNDS_MORYTANIA = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Morytania.png");
    private static final BufferedImage BOUNDS_TIRANNWN = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Tirannwn.png");
    private static final BufferedImage BOUNDS_DESERT = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Desert.png");

    public BufferedImage GetRegionImage(RegionType Type)
    {
        switch(Type)
        {
            case MISTHALIN:
                return BOUNDS_MISTHALIN;
            case KARAMJA:
                return BOUNDS_KARAMJA;
            case KANDARIN:
                return BOUNDS_KANDARIN;
            case ASGARNIA:
                return BOUNDS_ASGARNIA;
            case FREMENNIK:
                return BOUNDS_FREMENNIK;
            case KOUREND:
                return BOUNDS_KOUREND;
            case WILDERNESS:
                return BOUNDS_WILDERNESS;
            case MORYTANIA:
                return BOUNDS_MORYTANIA;
            case TIRANNWN:
                return BOUNDS_TIRANNWN;
            case DESERT:
                return BOUNDS_DESERT;
        }
        return MARKER_IMAGE;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            //client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Posiedien Leagues Planner says " + config.greeting(), null);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (bIsInitializing)
        {
            return;
        }

        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (map == null)
        {
            return;
        }

        if (!map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
        {
            return;
        }

        LeagueRegionPoint ClickedPoint = GetClickedRegionPoint(Collections.singletonList(event.getMenuEntry()));
        if (ClickedPoint != null && event.getMenuOption().contains("Focus on"))
        {
            // Toggle activation
            SetMarkerActivation(ClickedPoint, CurrentFocusedPoint != ClickedPoint);

            RefreshRegionBounds();
        }
    }

    @Provides
    LeaguesPlannerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LeaguesPlannerConfig.class);
    }

    private WorldPoint CalculateMapPoint(Point point)
    {
        if (point == null)
        {
            return null;
        }

        float zoom = client.getRenderOverview().getWorldMapZoom();
        RenderOverview renderOverview = client.getRenderOverview();
        final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
        final Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);
        if (middle == null)
        {
            return null;
        }

        final int dx = (int) ((point.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    private final void SetMarkerActivation(LeagueRegionPoint RegionPoint, boolean shouldActivate)
    {
        if (config.GetEditRegion() == RegionType.NONE)
        {
            return;
        }

        if (shouldActivate)
        {
            if (CurrentFocusedPoint != null)
            {
                SetMarkerActivation(CurrentFocusedPoint, false);
            }

            CurrentFocusedPoint = RegionPoint;

            WorldMapPoint OldMarker = RegionPoint.OurPoint;
            worldMapPointManager.removeIf(x -> x == OldMarker);

            WorldPoint WorldPointLocation;
            if (RegionPoint.OurWorldPoint == null)
            {
                WorldPointLocation = LastDisplayedWorldPoint;
            }
            else
            {
                WorldPointLocation = RegionPoint.OurWorldPoint;
            }

            RegionPoint.OurPoint = new WorldMapPoint(WorldPointLocation, BOUNDS_SELECTED);
            RegionPoint.OurWorldPoint = WorldPointLocation;
            WorldMapPoint NewMarker = RegionPoint.OurPoint;
            NewMarker.setTarget(WorldPointLocation);
            NewMarker.setJumpOnClick(true);
            NewMarker.setName("LP: Region Bounds: " + RegionPoint.Region+ " " + WorldPointLocation + " guid:"+ RegionPoint.GUID);
            worldMapPointManager.add(NewMarker);
        }
        else
        {
            WorldMapPoint OldMarker = RegionPoint.OurPoint;
            worldMapPointManager.removeIf(x -> x == OldMarker);

            WorldPoint WorldPointLocation;
            if (RegionPoint.OurWorldPoint == null)
            {
                WorldPointLocation = LastDisplayedWorldPoint;
            }
            else
            {
                WorldPointLocation = RegionPoint.OurWorldPoint;
            }

            RegionPoint.OurPoint = new WorldMapPoint(WorldPointLocation, GetRegionImage(RegionPoint.Region));
            RegionPoint.OurWorldPoint = WorldPointLocation;
            WorldMapPoint NewMarker = RegionPoint.OurPoint;
            NewMarker.setTarget(WorldPointLocation);
            NewMarker.setJumpOnClick(true);
            NewMarker.setName("LP: Region Bounds: " + RegionPoint.Region + " " + WorldPointLocation + " guid:"+ RegionPoint.GUID);
            worldMapPointManager.add(NewMarker);
        }
    }

    private final Consumer<MenuEntry> SetNextRegionPointEntryCallback = n ->
    {
        LeagueRegionPoint LastRegionPoint = CurrentFocusedPoint;
        LeagueRegionPoint NewRegionPoint = new LeagueRegionPoint();

        UUID uuid = UUID.randomUUID();
        NewRegionPoint.GUID = uuid;
        NewRegionPoint.Region = CurrentRegion.Type;
        CurrentRegion.RegionPoints.put(NewRegionPoint.GUID, NewRegionPoint);

        SetMarkerActivation(NewRegionPoint, true);

        if (LastRegionPoint != null)
        {
            // At capacity, remove last added
            if (LastRegionPoint.ConnectedPoints.size() == 2)
            {
                LastRegionPoint.ConnectedPoints.get(1).ConnectedPoints.remove(LastRegionPoint);
                LastRegionPoint.ConnectedPoints.remove(1);
            }

            LastRegionPoint.ConnectedPoints.add(NewRegionPoint);
            NewRegionPoint.ConnectedPoints.add(LastRegionPoint);
        }


        RefreshRegionBounds();
    };

    HashMap<Integer, TaskData> HashCodeToHash = new HashMap<>();

    private final Consumer<MenuEntry> AddTaskToBackOfPlan = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());

        // Find greatest value of planned tasks
        int CurrentOrder = -1;

        for (HashMap.Entry<UUID, TaskSortData> mapElement : config.UserData.PlannedTasks.entrySet())
        {
            if (mapElement.getValue().SortPriority > CurrentOrder)
            {
                CurrentOrder = mapElement.getValue().SortPriority;
            }

        }

        config.UserData.PlannedTasks.put(CurrentTask.GUID, new TaskSortData(CurrentOrder + 1, CurrentTask.bIsCustomTask));

        config.UserData.HiddenTasks.remove(CurrentTask.GUID);
        QueueRefresh();
    };

    TaskData GetTaskData(UUID TaskID, Boolean bIsCustomTask)
    {
        if (bIsCustomTask)
        {
            return config.UserData.CustomTasks.get(TaskID);
        }

        return config.TaskData.LeaguesTaskList.get(TaskID);
    }

    ArrayList<UUID> TempArray = new ArrayList<>();
    private final Consumer<MenuEntry> AddTaskToFrontOfPlan = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());

        // Find smallest value of planned tasks
        int CurrentOrder = 9000000;

        TempArray.clear();
        for (HashMap.Entry<UUID, TaskSortData> mapElement : config.UserData.PlannedTasks.entrySet())
        {
            if (mapElement.getValue().SortPriority < CurrentOrder)
            {
                CurrentOrder = mapElement.getValue().SortPriority;
            }
            TempArray.add(mapElement.getKey());
        }

        for (UUID SearchingTaskGUID : TempArray)
        {
            int OldOrder = config.UserData.PlannedTasks.get(SearchingTaskGUID).SortPriority;
            Boolean bOldIsCustomTask = config.UserData.PlannedTasks.get(SearchingTaskGUID).bIsCustomTask;

            config.UserData.PlannedTasks.remove(SearchingTaskGUID);
            config.UserData.PlannedTasks.put(SearchingTaskGUID, new TaskSortData(OldOrder + 1, bOldIsCustomTask));

            config.UserData.HiddenTasks.remove(SearchingTaskGUID);
        }

        if (CurrentOrder == 9000000)
        {
            CurrentOrder = 0;
        }

        config.UserData.PlannedTasks.put(CurrentTask.GUID, new TaskSortData(CurrentOrder, CurrentTask.bIsCustomTask));
        config.UserData.HiddenTasks.remove(CurrentTask.GUID);
        QueueRefresh();
    };

    private final Consumer<MenuEntry> RemoveTaskFromPlan = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());
        config.UserData.PlannedTasks.remove(CurrentTask.GUID);
        QueueRefresh();
    };

    private final Consumer<MenuEntry> MoveForwardOnPlan = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());

        // Find value closest to our order that is smallest
        // Between these two values, closest to current
        int CurrentHighest = -1;
        int CurrentOrder = config.UserData.PlannedTasks.get(CurrentTask.GUID).SortPriority;
        UUID ClosestTask = null;

        TempArray.clear();
        for (HashMap.Entry<UUID, TaskSortData> mapElement : config.UserData.PlannedTasks.entrySet())
        {
            if (!mapElement.getKey().equals(CurrentTask.GUID) && mapElement.getValue().SortPriority <= CurrentOrder && mapElement.getValue().SortPriority > CurrentHighest)
            {
                CurrentHighest = mapElement.getValue().SortPriority;
                ClosestTask = mapElement.getKey();
            }
            TempArray.add(mapElement.getKey());
        }

        // Replace at closest task (otherwise we are first!)
        if (ClosestTask != null)
        {
            // Replace this spot
            config.UserData.PlannedTasks.remove(CurrentTask.GUID);
            config.UserData.PlannedTasks.put(CurrentTask.GUID, new TaskSortData(CurrentHighest, CurrentTask.bIsCustomTask));

            config.UserData.HiddenTasks.remove(CurrentTask.GUID);
            for (UUID SearchingTaskGUID : TempArray)
            {
                if (SearchingTaskGUID.equals(CurrentTask.GUID))
                {
                    continue;
                }

                // Push these all back (insert)
                int OldOrder = config.UserData.PlannedTasks.get(SearchingTaskGUID).SortPriority;
                Boolean bOldCustomTask = config.UserData.PlannedTasks.get(SearchingTaskGUID).bIsCustomTask;
                if (OldOrder >= CurrentHighest)
                {
                    config.UserData.PlannedTasks.remove(SearchingTaskGUID);
                    config.UserData.PlannedTasks.put(SearchingTaskGUID, new TaskSortData(OldOrder + 1, bOldCustomTask));
                    config.UserData.HiddenTasks.remove(SearchingTaskGUID);
                }
            }

            QueueRefresh();
        }
    };

    private final Consumer<MenuEntry> MoveBackOnPlan = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());

        // Find value closest to our order that is smallest
        // Between these two values, closest to current
        int CurrentLowest = 900000;
        int CurrentOrder = config.UserData.PlannedTasks.get(CurrentTask.GUID).SortPriority;
        UUID ClosestTask = null;

        TempArray.clear();
        for (HashMap.Entry<UUID, TaskSortData> mapElement : config.UserData.PlannedTasks.entrySet())
        {
            if (!mapElement.getKey().equals(CurrentTask.GUID) && mapElement.getValue().SortPriority >= CurrentOrder && mapElement.getValue().SortPriority < CurrentLowest)
            {
                CurrentLowest = mapElement.getValue().SortPriority;
                ClosestTask = mapElement.getKey();
            }
            TempArray.add(mapElement.getKey());
        }

        // Replace at closest task (otherwise we are first!)
        if (ClosestTask != null)
        {
            // Move up a little
            Boolean bOldCustomTaskClosest = config.UserData.PlannedTasks.get(ClosestTask).bIsCustomTask;
            config.UserData.PlannedTasks.remove(ClosestTask);
            config.UserData.PlannedTasks.put(ClosestTask, new TaskSortData(CurrentLowest - 1, bOldCustomTaskClosest));

            config.UserData.HiddenTasks.remove(ClosestTask);

            // Replace this spot
            config.UserData.PlannedTasks.remove(CurrentTask.GUID);
            config.UserData.PlannedTasks.put(CurrentTask.GUID, new TaskSortData(CurrentLowest, CurrentTask.bIsCustomTask));

            config.UserData.HiddenTasks.remove(CurrentTask.GUID);

            for (UUID SearchingTaskGUID : TempArray)
            {
                if (SearchingTaskGUID.equals(CurrentTask.GUID))
                {
                    continue;
                }

                // Push these all back (insert)
                int OldOrder = config.UserData.PlannedTasks.get(SearchingTaskGUID).SortPriority;
                Boolean bOldCustomTask = config.UserData.PlannedTasks.get(SearchingTaskGUID).bIsCustomTask;
                if (OldOrder >= CurrentLowest)
                {
                    config.UserData.PlannedTasks.remove(SearchingTaskGUID);
                    config.UserData.PlannedTasks.put(SearchingTaskGUID, new TaskSortData(OldOrder + 1, bOldCustomTask));
                    config.UserData.HiddenTasks.remove(SearchingTaskGUID);

                }
            }

            QueueRefresh();
        }
    };

    public void FocusOnTaskOnWorldMap(TaskData CurrentTask)
    {
        if (bIsInitializing)
        {
            return;
        }

        if (client != null && client.getWorldMap() != null)
        {
            double ShortestMapViewDistance = 1000000;
            WorldPoint ShortestMapViewWorldPoint = null;
            WorldMap worldMap = client.getWorldMap();
            if (worldMap != null)
            {
                WorldPoint mapPoint = new WorldPoint(worldMap.getWorldMapPosition().getX(), worldMap.getWorldMapPosition().getY(), 0);
                for (WorldPoint CurrentLocation : CurrentTask.Locations)
                {
                    double TaskDistance = CurrentLocation.distanceTo(mapPoint);
                    if (TaskDistance < ShortestMapViewDistance)
                    {
                        ShortestMapViewDistance = TaskDistance;
                        ShortestMapViewWorldPoint = CurrentLocation;
                    }
                }

                for (WorldPoint CurrentLocation : CurrentTask.OverworldLocations)
                {
                    double TaskDistance = CurrentLocation.distanceTo(mapPoint);
                    if (TaskDistance < ShortestMapViewDistance)
                    {
                        ShortestMapViewDistance = TaskDistance;
                        ShortestMapViewWorldPoint = CurrentLocation;
                    }
                }
            }

            WorldPoint FocusLocation = ShortestMapViewWorldPoint;

            if (FocusLocation == null && !CurrentTask.Locations.isEmpty())
            {
                FocusLocation = CurrentTask.Locations.get(0);
            }

            if (FocusLocation != null)
            {
                client.getWorldMap().setWorldMapPositionTarget(FocusLocation);
            }
        }
    }
    private final Consumer<MenuEntry> FocusOnTaskLocation = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());
        FocusOnTaskOnWorldMap(CurrentTask);
    };

    private final Consumer<MenuEntry> CreateCustomTask = n ->
    {
        UUID newCustomTaskGUID = UUID.randomUUID();
        TaskData newTask = new TaskData();

        newTask.GUID = newCustomTaskGUID;
        newTask.Locations.add(LastDisplayedWorldPoint);
        newTask.Difficulty = TaskDifficulty.CUSTOM;
        newTask.TaskName = "Edit task in Panel";
        newTask.bIsCustomTask = true;

        config.UserData.CustomTasks.put(newCustomTaskGUID, newTask);

        bMapDisplayPointsDirty = true;
        QueueRefresh();
    };

    private final Consumer<MenuEntry> DestroyCustomTask = n ->
    {
        TaskData CurrentTask = HashCodeToHash.get(n.getParam0());
        config.UserData.CustomTasks.remove(CurrentTask.GUID);
        config.UserData.PlannedTasks.remove(CurrentTask.GUID);

        bMapDisplayPointsDirty = true;
        QueueRefresh();
    };


    HashMap<String, BufferedImage> CustomIconsMap = new HashMap<>();

    public void RegisterCustomIcon(String CustomIconName)
    {
        CustomIconsMap.put(CustomIconName, ImageUtil.loadImageResource(PosiedienLeaguesPlannerPlugin.class, CustomIconName));
    }
    public void InitializeCustomIconsMap()
    {
        RegisterCustomIcon("/AGILITY.png");
        RegisterCustomIcon("/ATTACK.png");
        RegisterCustomIcon("/CONSTRUCTION.png");
        RegisterCustomIcon("/COOKING.png");
        RegisterCustomIcon("/CRAFTING.png");
        RegisterCustomIcon("/DEFENCE.png");
        RegisterCustomIcon("/FARMING.png");
        RegisterCustomIcon("/FIREMAKING.png");
        RegisterCustomIcon("/FISHING.png");
        RegisterCustomIcon("/FLETCHING.png");
        RegisterCustomIcon("/HERBLORE.png");
        RegisterCustomIcon("/HITPOINTS.png");
        RegisterCustomIcon("/HUNTER.png");
        RegisterCustomIcon("/MAGIC.png");
        RegisterCustomIcon("/MINING.png");
        RegisterCustomIcon("/PRAYER.png");
        RegisterCustomIcon("/RANGED.png");
        RegisterCustomIcon("/RUNECRAFT.png");
        RegisterCustomIcon("/SMITHING.png");
        RegisterCustomIcon("/STRENGTH.png");
        RegisterCustomIcon("/THIEVING.png");
        RegisterCustomIcon("/taskMarkerPurp.png");
        RegisterCustomIcon("/taskMarkerRed.png");
        RegisterCustomIcon("/taskMarker.png");
        RegisterCustomIcon("/taskMarkerGreen.png");
        RegisterCustomIcon("/taskMarkerGreenEasy.png");
        RegisterCustomIcon("/taskMarkerGreenElite.png");
        RegisterCustomIcon("/taskMarkerGreenHard.png");
        RegisterCustomIcon("/taskMarkerGreenMedium.png");
        RegisterCustomIcon("/STRENGTH.png");
        RegisterCustomIcon("/icon_background.png");
        RegisterCustomIcon("/BlankIcon.png");
    }

    TaskData CustomTask_ChangingIcon = null;


    private final Consumer<MenuEntry> ChangeIconCustomTask = n ->
    {
        CustomTask_ChangingIcon = HashCodeToHash.get(n.getParam0());
        SinceLastInputTimer = 0.0f;
    };


    private final Consumer<MenuEntry> SetActiveRegionPointEntryCallback = n ->
    {
        SetMarkerActivation(LastClickedRegionPoint, true);


        RefreshRegionBounds();
    };

    private final Consumer<MenuEntry> DeleteRegionPointEntryCallback = n ->
    {
        // Remove connections
        for (LeagueRegionPoint ConnectedPoint : LastClickedRegionPoint.ConnectedPoints)
        {
            ConnectedPoint.ConnectedPoints.remove(LastClickedRegionPoint);
        }
        LastClickedRegionPoint.ConnectedPoints.clear();

        worldMapPointManager.removeIf(x -> x == LastClickedRegionPoint.OurPoint);

        CurrentRegion.RegionPoints.remove(LastClickedRegionPoint.GUID);
        if (LastClickedRegionPoint == CurrentFocusedPoint)
        {
            CurrentFocusedPoint = null;
        }


        RefreshRegionBounds();
    };

    private final Consumer<MenuEntry> ConnectRegionPointEntryCallback = n ->
    {
        // If we are already connected, disconnect instead
        for (LeagueRegionPoint ConnectedPoint : CurrentFocusedPoint.ConnectedPoints)
        {
            if (ConnectedPoint == LastClickedRegionPoint)
            {
                ConnectedPoint.ConnectedPoints.remove(CurrentFocusedPoint);
                CurrentFocusedPoint.ConnectedPoints.remove(ConnectedPoint);

                RefreshRegionBounds();
                return;
            }
        }

        // At capacity, remove last added
        if (CurrentFocusedPoint.ConnectedPoints.size() == 2)
        {
            CurrentFocusedPoint.ConnectedPoints.get(1).ConnectedPoints.remove(CurrentFocusedPoint);
            CurrentFocusedPoint.ConnectedPoints.remove(1);
        }

        CurrentFocusedPoint.ConnectedPoints.add(LastClickedRegionPoint);

        // At capacity, remove last added
        if (LastClickedRegionPoint.ConnectedPoints.size() == 2)
        {
            LastClickedRegionPoint.ConnectedPoints.get(1).ConnectedPoints.remove(LastClickedRegionPoint);
            LastClickedRegionPoint.ConnectedPoints.remove(1);
        }

        LastClickedRegionPoint.ConnectedPoints.add(CurrentFocusedPoint);


        RefreshRegionBounds();
    };

    private final LeagueRegionPoint GetClickedRegionPoint(List<MenuEntry> entries)
    {
        for (MenuEntry CurrentEntry : entries)
        {
            if (!CurrentEntry.getOption().contains("Focus"))
            {
                continue;
            }

            String TargetString = CurrentEntry.getTarget();
            if (CurrentRegion != null && TargetString.contains("Region Bounds: "))
            {
                int index = TargetString.indexOf("guid:");

                // Guids are 36 indices long
                String SubString = TargetString.substring(index + 5, index + 5 + 36);

                UUID FoundGUID = UUID.fromString(SubString);
                LeagueRegionPoint FoundPoint = CurrentRegion.RegionPoints.get(FoundGUID);
                if (FoundPoint != null)
                {
                    return FoundPoint;
                }
            }
        }

        return null;
    }

    private void AddRightClickMenuEntries(MenuOpened event)
    {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(event.getMenuEntries()));

        LastDisplayedWorldPoint = CalculateMapPoint(client.isMenuOpen() ? lastMenuOpenedPoint : client.getMouseCanvasPosition());
        if (LastDisplayedWorldPoint == null)
        {
            return;
        }

        LastClickedRegionPoint = GetClickedRegionPoint(entries);
        if (CurrentFocusedPoint != null && CurrentRegion != null && CurrentRegion.Type != CurrentFocusedPoint.Region)
        {
            SetMarkerActivation(CurrentFocusedPoint, false);
            CurrentFocusedPoint = null;
        }

        // How many tasks are we colliding with?
        if (taskOverlay != null)
        {
            MenuEntry customtaskMenu = client.createMenuEntry(-1);
            customtaskMenu.setTarget(ColorUtil.wrapWithColorTag("Custom Task", Color.YELLOW));
            customtaskMenu.setOption("Create");
            customtaskMenu.onClick(this.CreateCustomTask);
            customtaskMenu.setType(MenuAction.RUNELITE);
            entries.add(0, customtaskMenu);

            HashCodeToHash.clear();
            ArrayList<TaskDisplayPoint> ClickedPoints = taskOverlay.GetClickedDisplayPoint(getSelectedWorldPoint());

            for (TaskDisplayPoint CurrentDisplayPoint : ClickedPoints)
            {
                for (HashMap.Entry<UUID, Boolean> mapElement : CurrentDisplayPoint.Tasks.entrySet())
                {
                    UUID TaskGUID = mapElement.getKey();
                    boolean bIsCustomTask = mapElement.getValue();
                    TaskData CurrentTask = GetTaskData(TaskGUID, bIsCustomTask);

                    HashCodeToHash.put(TaskGUID.hashCode(), CurrentTask);

                    if (bIsCustomTask)
                    {
                        MenuEntry customtaskMenu2 = client.createMenuEntry(-1);
                        customtaskMenu2.setTarget(ColorUtil.wrapWithColorTag("Custom Task", Color.YELLOW));
                        customtaskMenu2.setOption("Destroy");
                        customtaskMenu2.onClick(this.DestroyCustomTask);
                        customtaskMenu2.setType(MenuAction.RUNELITE);
                        customtaskMenu2.setParam0(TaskGUID.hashCode());
                        entries.add(0, customtaskMenu2);


                        MenuEntry customtaskMenu3 = client.createMenuEntry(-1);
                        customtaskMenu3.setTarget(ColorUtil.wrapWithColorTag("Custom Task", Color.YELLOW));
                        customtaskMenu3.setOption("Change Icon");
                        customtaskMenu3.onClick(this.ChangeIconCustomTask);
                        customtaskMenu3.setType(MenuAction.RUNELITE);
                        customtaskMenu3.setParam0(TaskGUID.hashCode());
                        entries.add(0, customtaskMenu3);
                    }

                    /*
                    We don't need this option, but good to have in case we want to debug

                    MenuEntry taskMenu = client.createMenuEntry(-1);
                    taskMenu.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                    taskMenu.setOption("Focus on");
                    taskMenu.onClick(this.FocusOnTaskLocation);
                    taskMenu.setType(MenuAction.RUNELITE);
                    taskMenu.setParam0(TaskGUID.hashCode());
                    entries.add(0, taskMenu);
                    */

                    if (!config.UserData.PlannedTasks.containsKey(TaskGUID))
                    {
                        MenuEntry plannedTaskMenu = client.createMenuEntry(-1);
                        plannedTaskMenu.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                        plannedTaskMenu.setOption("Add to plan (back)");
                        plannedTaskMenu.onClick(this.AddTaskToBackOfPlan);
                        plannedTaskMenu.setType(MenuAction.RUNELITE);
                        plannedTaskMenu.setParam0(TaskGUID.hashCode());
                        entries.add(0, plannedTaskMenu);

                        MenuEntry plannedTaskMenu2 = client.createMenuEntry(-1);
                        plannedTaskMenu2.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                        plannedTaskMenu2.setOption("Add to plan (front)");
                        plannedTaskMenu2.onClick(this.AddTaskToFrontOfPlan);
                        plannedTaskMenu2.setType(MenuAction.RUNELITE);
                        plannedTaskMenu2.setParam0(TaskGUID.hashCode());
                        entries.add(0, plannedTaskMenu2);

                    }
                    else
                    {
                        MenuEntry plannedTaskMenu3 = client.createMenuEntry(-1);
                        plannedTaskMenu3.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                        plannedTaskMenu3.setOption("Move back on plan");
                        plannedTaskMenu3.onClick(this.MoveBackOnPlan);
                        plannedTaskMenu3.setType(MenuAction.RUNELITE);
                        plannedTaskMenu3.setParam0(TaskGUID.hashCode());
                        entries.add(0, plannedTaskMenu3);

                        MenuEntry plannedTaskMenu2 = client.createMenuEntry(-1);
                        plannedTaskMenu2.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                        plannedTaskMenu2.setOption("Move forward on plan");
                        plannedTaskMenu2.onClick(this.MoveForwardOnPlan);
                        plannedTaskMenu2.setType(MenuAction.RUNELITE);
                        plannedTaskMenu2.setParam0(TaskGUID.hashCode());
                        entries.add(0, plannedTaskMenu2);

                        MenuEntry plannedTaskMenu = client.createMenuEntry(-1);
                        plannedTaskMenu.setTarget(ColorUtil.wrapWithColorTag(CurrentTask.TaskName, TaskDifficulty.GetTaskDifficultyColor(CurrentTask.Difficulty)));
                        plannedTaskMenu.setOption("Remove from plan");
                        plannedTaskMenu.onClick(this.RemoveTaskFromPlan);
                        plannedTaskMenu.setType(MenuAction.RUNELITE);
                        plannedTaskMenu.setParam0(TaskGUID.hashCode());
                        entries.add(0, plannedTaskMenu);

                    }

                }
            }
        }

        String nextOption = null;
        if (config.GetEditRegion() != RegionType.NONE)
        {
            if (LastClickedRegionPoint == null)
            {
                nextOption = "Set Next Region Point";
                String finalNextOption1 = nextOption;
                if (entries.stream().noneMatch(e -> e.getOption().equals(finalNextOption1)))
                {
                    MenuEntry SetNextRegionPointEntry = client.createMenuEntry(-1);
                    SetNextRegionPointEntry.setOption(nextOption);
                    SetNextRegionPointEntry.setType(MenuAction.RUNELITE);
                    SetNextRegionPointEntry.onClick(this.SetNextRegionPointEntryCallback);
                    entries.add(0, SetNextRegionPointEntry);
                }
            }
            else
            {
                if (CurrentFocusedPoint != LastClickedRegionPoint)
                {
                    nextOption = "Set Active Region Point";
                    String finalNextOption2 = nextOption;
                    if (entries.stream().noneMatch(e -> e.getOption().equals(finalNextOption2)))
                    {
                        MenuEntry SetActiveRegionPointEntry = client.createMenuEntry(-1);
                        SetActiveRegionPointEntry.setOption(nextOption);
                        SetActiveRegionPointEntry.setType(MenuAction.RUNELITE);
                        SetActiveRegionPointEntry.onClick(this.SetActiveRegionPointEntryCallback);
                        entries.add(0, SetActiveRegionPointEntry);
                    }

                    if (CurrentFocusedPoint != null)
                    {
                        nextOption = "Connect Region Point";
                        String finalNextOption4 = nextOption;
                        if (entries.stream().noneMatch(e -> e.getOption().equals(finalNextOption4)))
                        {
                            MenuEntry ConnectRegionPointEntry = client.createMenuEntry(-1);
                            ConnectRegionPointEntry.setOption(nextOption);
                            ConnectRegionPointEntry.setType(MenuAction.RUNELITE);
                            ConnectRegionPointEntry.onClick(this.ConnectRegionPointEntryCallback);
                            entries.add(0, ConnectRegionPointEntry);
                        }
                    }
                }

                nextOption = "Delete Region Point";
                String finalNextOption3 = nextOption;
                if (entries.stream().noneMatch(e -> e.getOption().equals(finalNextOption3)))
                {
                    MenuEntry DeleteRegionPointEntry = client.createMenuEntry(-1);
                    DeleteRegionPointEntry.setOption(nextOption);
                    DeleteRegionPointEntry.setType(MenuAction.RUNELITE);
                    DeleteRegionPointEntry.onClick(this.DeleteRegionPointEntryCallback);
                    entries.add(0, DeleteRegionPointEntry);
                }

            }
        }

        // Debug helper
        //if (0 == 1)
        {
            if (config.GetEditRegion() == RegionType.NONE)
            {
                nextOption = "Map Coordinate: " + LastDisplayedWorldPoint;
                String finalNextOption = nextOption;
                if (entries.stream().noneMatch(e -> e.getOption().equals(finalNextOption)))
                {
                    MenuEntry MapCoordEntry = client.createMenuEntry(-1);
                    MapCoordEntry.setOption(nextOption);
                    MapCoordEntry.setType(MenuAction.RUNELITE);
                    entries.add(0, MapCoordEntry);
                }
            }
        }

        client.setMenuEntries(entries.toArray(new MenuEntry[0]));
    }
}
