package Posiedien_Leagues_Planner;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.SpriteID;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
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
import net.runelite.client.ui.JagexColors;
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

@PluginDescriptor(
        name = "Posiedien Leagues Planner",
    description = "Helper planner plugin for leagues",
    tags = {"pathfinder", "map", "waypoint", "navigation", "leagues"}
)
public class PosiedienLeaguesPlannerPlugin extends Plugin {
    protected static final String CONFIG_GROUP = "Posiedien_Leagues_Planner";
    private static final String ADD_START = "Add start";
    private static final String ADD_END = "Add end";
    private static final String CLEAR = "Clear";
    private static final String PATH = ColorUtil.wrapWithColorTag("Path", JagexColors.MENU_TARGET);
    private static final String SET = "Set";
    private static final String START = ColorUtil.wrapWithColorTag("Start", JagexColors.MENU_TARGET);
    private static final String TARGET = ColorUtil.wrapWithColorTag("Target", JagexColors.MENU_TARGET);
    private static final String TRANSPORT = ColorUtil.wrapWithColorTag("Transport", JagexColors.MENU_TARGET);
    private static final String WALK_HERE = "Walk here";
    private static final BufferedImage MARKER_IMAGE = ImageUtil.loadImageResource(PosiedienLeaguesPlannerPlugin.class, "/marker.png");

    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private LeaguesPlannerConfig config;

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
    private Future<?> pathfinderFuture;
    private final Object pathfinderMutex = new Object();
    @Getter
    private Pathfinder pathfinder;
    private PathfinderConfig pathfinderConfig;
    @Getter
    private boolean startPointSet = false;

    @Override
    protected void startUp() {
        SplitFlagMap map = SplitFlagMap.fromResources();
        Map<WorldPoint, List<Transport>> transports = Transport.loadAllFromResources();

        pathfinderConfig = new PathfinderConfig(map, transports, client, config);

        overlayManager.add(pathOverlay);
        overlayManager.add(pathMinimapOverlay);
        overlayManager.add(pathMapOverlay);
        overlayManager.add(pathMapTooltipOverlay);

        if (config.drawDebugPanel()) {
            overlayManager.add(debugOverlayPanel);
        }
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(pathOverlay);
        overlayManager.remove(pathMinimapOverlay);
        overlayManager.remove(pathMapOverlay);
        overlayManager.remove(pathMapTooltipOverlay);
        overlayManager.remove(debugOverlayPanel);

        if (pathfindingExecutor != null) {
            pathfindingExecutor.shutdownNow();
            pathfindingExecutor = null;
        }
    }

    public void restartPathfinding(WorldPoint start, WorldPoint end) {
        synchronized (pathfinderMutex) {
            if (pathfinder != null) {
                pathfinder.cancel();
                pathfinderFuture.cancel(true);
            }

            if (pathfindingExecutor == null) {
                ThreadFactory shortestPathNaming = new ThreadFactoryBuilder().setNameFormat("shortest-path-%d").build();
                pathfindingExecutor = Executors.newSingleThreadExecutor(shortestPathNaming);
            }
        }

        getClientThread().invokeLater(() -> {
            pathfinderConfig.refresh();
            synchronized (pathfinderMutex) {
                pathfinder = new Pathfinder(pathfinderConfig, start, end);
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
    public void onConfigChanged(ConfigChanged event) {
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
                restartPathfinding(pathfinder.getStart(), pathfinder.getTarget());
            }
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || pathfinder == null) {
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
            restartPathfinding(currentLocation, pathfinder.getTarget());
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
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

    private WorldPoint getSelectedWorldPoint() {
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
            restartPathfinding(start, target);
        }
    }

    private void setStart(WorldPoint start) {
        if (pathfinder == null) {
            return;
        }
        startPointSet = true;
        restartPathfinding(start, pathfinder.getTarget());
    }

    public WorldPoint calculateMapPoint(Point point) {
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

    @Inject
    private Client client;

    @Inject
    private LeaguesPlannerConfig config;
    @Inject
    public OverlayManager overlayManager;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private WorldMapOverlay worldMapOverlay;


    private Point LastMenuOpenedPoint;

    public LeagueRegionBounds CurrentRegion = null;
    public LeagueRegionPoint LastClickedRegionPoint = null;
    @Inject
    public RegionBoundOverlay regionBoundOverlay = new RegionBoundOverlay(client, this, config);

    public LeagueRegionPoint CurrentFocusedPoint;

    void SaveRegionBounds() throws IOException
    {
        File targ = new File("RegionBoundData.csv");
        config.RegionData.exportTo(targ);
    }

    void LoadRegionBounds() throws IOException
    {
        File targ = new File("RegionBoundData.csv");
        config.RegionData.importFrom(targ);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
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
            Color DrawColor = RegionType.GetRegionColor(config, regionDatum.Type);
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


    public WorldPoint LastDisplayedWorldPoint;

    private static final BufferedImage MARKER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/marker.png");
    private static final BufferedImage ACTIVE_MARKER_IMAGE = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/activemarker.png");

    private static final BufferedImage BOUNDS_SELECTED = ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/BoundPoint_Selected.png");
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

    @Override
    protected void startUp() throws Exception
    {
        InitializeRegionData();
        overlayManager.add(regionBoundOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        SaveRegionBounds();
        overlayManager.remove(regionBoundOverlay);
        worldMapPointManager.removeIf(x -> x.getName() != null && x.getName().contains("LP:"));
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

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (map == null)
        {
            return;
        }

        if (!map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
        {
            return;
        }

        LastMenuOpenedPoint = client.getMouseCanvasPosition();
        CurrentRegion = GetRegionBounds(config.GetEditRegion());
        AddRightClickMenuEntries(event);
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        //log.info("TEST 2!");
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
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

        LastDisplayedWorldPoint = CalculateMapPoint(client.isMenuOpen() ? LastMenuOpenedPoint : client.getMouseCanvasPosition());
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

        client.setMenuEntries(entries.toArray(new MenuEntry[0]));
    }
}