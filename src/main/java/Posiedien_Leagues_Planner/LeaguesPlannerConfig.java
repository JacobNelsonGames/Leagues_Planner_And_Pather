package Posiedien_Leagues_Planner;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.ui.PluginPanel;

@ConfigGroup(PosiedienLeaguesPlannerPlugin.CONFIG_GROUP)
public interface LeaguesPlannerConfig extends Config
{
    public FullRegionData RegionData = new FullRegionData();

    public FullTaskData TaskData = new FullTaskData();

    @ConfigSection(
            position = 3,
            name = "Region Editor",
            description = "Editor functions",
            closedByDefault = true
    )
    String editorSection = "editorSection";

    @ConfigItem(
            keyName = "GetEditRegion",
            name = "Edit Region Bounds",
            description = "Current region bounds we want to edit",
            section = editorSection
    )
    default RegionType GetEditRegion()
    {
        return RegionType.NONE;
    }

    @ConfigItem(
            keyName = "DebugColorAlpha",
            name = "Debug Color Alpha (0-255)",
            description = "Alpha value for any debug color (value 0-255)",
            section = editorSection
    )
    default int DebugColorAlpha()
    {
        return 40;
    }

    @ConfigItem(
            keyName = "DebugColorDisabledAlpha",
            name = "Debug Color Disabled Alpha (0-255)",
            description = "Alpha value for disabled debug color (value 0-255)",
            section = editorSection
    )
    default int DebugColorDisabledAlpha()
    {
        return 255;
    }

    @ConfigSection(
            position = 2,
            name = "Region Plugin Colors",
            description = "The colors to use for our regions",
            closedByDefault = true
    )
    String regionColors = "regionColors";

    @ConfigItem(
            keyName = "MisthalinColor",
            name = "Misthalin Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color MisthalinColor()
    {
        return new Color(255, 2, 2, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "KaramjaColor",
            name = "Karamja Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color KaramjaColor()
    {
        return new Color(46, 108, 23, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "KandarinColor",
            name = "Kandarin Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color KandarinColor()
    {
        return new Color(231, 143, 10, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "AsgarniaColor",
            name = "Asgarnia Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color AsgarniaColor()
    {
        return new Color(46, 59, 234, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "FremennikColor",
            name = "Fremennik Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color FremennikColor()
    {
        return new Color(121, 67, 3, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "KourendColor",
            name = "Kourend Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color KourendColor()
    {
        return new Color(31, 224, 179, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "WildernessColor",
            name = "Wilderness Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color WildernessColor()
    {
        return new Color(94, 14, 14, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "MorytaniaColor",
            name = "Morytania Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color MorytaniaColor()
    {
        return new Color(102, 3, 114, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "TirannwnColor",
            name = "Tirannwn Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color TirannwnColor()
    {
        return new Color(130, 255, 105, DebugColorAlpha());
    }

    @ConfigItem(
            keyName = "DesertColor",
            name = "Desert Color",
            description = "The color for the region",
            section = regionColors
    )
    default Color DesertColor()
    {
        return new Color(255, 226, 1, DebugColorAlpha());
    }


    @ConfigSection(
            position = 1,
            name = "Regions Unlocked",
            description = "The regions we currently have unlocked"
    )
    String regionUnlock = "regionUnlock";

    @ConfigItem(
            keyName = "FilteredDifficulty",
            name = "Filter Difficulty",
            description = "Task Difficulty Filter",
            section = regionUnlock
    )
    default TaskDifficulty FilteredDifficulty()
    {
        return TaskDifficulty.NONE;
    }

    @ConfigItem(
            keyName = "FilteredOther",
            name = "Filter Other",
            description = "Task Other Filter",
            section = regionUnlock
    )
    default OtherFilter FilteredOther()
    {
        return OtherFilter.NONE;
    }

    @ConfigItem(
            keyName = "TaskSort",
            name = "Task Sort",
            description = "Task Sort",
            section = regionUnlock
    )
    default TaskSortMethod TaskSort()
    {
        return TaskSortMethod.NONE;
    }

    @ConfigItem(
            keyName = "MisthalinUnlocked",
            name = "Misthalin Unlocked",
            description = "Whether or not Misthalin is unlocked",
            section = regionUnlock
    )
    default boolean MisthalinUnlocked()
    {
        return true;
    }

    @ConfigItem(
            keyName = "KaramjaUnlocked",
            name = "Karamja Unlocked",
            description = "Whether or not Karamja is unlocked",
            section = regionUnlock
    )
    default boolean KaramjaUnlocked()
    {
        return true;
    }

    @ConfigItem(
            keyName = "KandarinUnlocked",
            name = "Kandarin Unlocked",
            description = "Whether or not Kandarin is unlocked",
            section = regionUnlock
    )
    default boolean KandarinUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "AsgarniaUnlocked",
            name = "Asgarnia Unlocked",
            description = "Whether or not Asgarnia is unlocked",
            section = regionUnlock
    )
    default boolean AsgarniaUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "FremennikUnlocked",
            name = "Fremennik Unlocked",
            description = "Whether or not Fremennik is unlocked",
            section = regionUnlock
    )
    default boolean FremennikUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "KourendUnlocked",
            name = "Kourend Unlocked",
            description = "Whether or not Kourend is unlocked",
            section = regionUnlock
    )
    default boolean KourendUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "WildernessUnlocked",
            name = "Wilderness Unlocked",
            description = "Whether or not Wilderness is unlocked",
            section = regionUnlock
    )
    default boolean WildernessUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "MorytaniaUnlocked",
            name = "Morytania Unlocked",
            description = "Whether or not Morytania is unlocked",
            section = regionUnlock
    )
    default boolean MorytaniaUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "TirannwnUnlocked",
            name = "Tirannwn Unlocked",
            description = "Whether or not Tirannwn is unlocked",
            section = regionUnlock
    )
    default boolean TirannwnUnlocked()
    {
        return false;
    }

    @ConfigItem(
            keyName = "DesertUnlocked",
            name = "Desert Unlocked",
            description = "Whether or not Desert is unlocked",
            section = regionUnlock
    )
    default boolean DesertUnlocked()
    {
        return false;
    }

    @ConfigSection(
            position = 4,
            name = "Leagues Planner Settings",
            description = "Settings relating to the leagues planner plugin",
            closedByDefault = true
    )
    String leaguesPlannerSection = "leaguesPlannerSection";

    @ConfigItem(
            keyName = "TaskMapStackingDistance",
            name = "Task Map Stacking Distance",
            description = "The maximum distance between tasks to start stacking in the map interface (in pixels)",
            section = leaguesPlannerSection
    )
    default int TaskMapStackingDistance()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "TaskMapIconSize",
            name = "Task map icon size",
            description = "Task map icon size (in pixels)",
            section = leaguesPlannerSection
    )
    default int TaskMapIconSize()
    {
        return 30;
    }

    @ConfigItem(
            keyName = "ScaleTaskMapIconBasedOnCount",
            name = "Scale Task Map Icon Based On Count",
            description = "Whether or not Scale Task Map Icon Based On Count",
            section = leaguesPlannerSection
    )
    default boolean ScaleTaskMapIconBasedOnCount()
    {
        return true;
    }

    @ConfigItem(
            keyName = "ScaleTaskMapIconBasedOnCountInvRate",
            name = "Scale Task Map Icon Based On Count, inverse rate",
            description = "Inverse rate on our scaling map icon based on count",
            section = leaguesPlannerSection
    )
    default int ScaleTaskMapIconBasedOnCountInvRate()
    {
        return 10;
    }

    @ConfigSection(
        name = "Pathfinding Settings",
        description = "Options for the pathfinding",
        position = 5,
            closedByDefault = true
    )
    String sectionSettings = "sectionSettings";

    @ConfigItem(
        keyName = "avoidWilderness",
        name = "Avoid wilderness",
        description = "Whether the wilderness should be avoided if possible<br>" +
            "(otherwise, will e.g. suggest using wilderness lever to travel from Edgeville to Ardougne)",
        position = 1,
        section = sectionSettings
    )
    default boolean avoidWilderness() {
        return true;
    }

    @ConfigItem(
        keyName = "useAgilityShortcuts",
        name = "Use agility shortcuts",
        description = "Whether to include agility shortcuts in the path.<br>You must also have the required agility level",
        position = 2,
        section = sectionSettings
    )
    default boolean useAgilityShortcuts() {
        return true;
    }

    @ConfigItem(
        keyName = "useGrappleShortcuts",
        name = "Use grapple shortcuts",
        description = "Whether to include crossbow grapple agility shortcuts in the path.<br>" +
            "You must also have the required agility, ranged and strength levels",
        position = 3,
        section = sectionSettings
    )
    default boolean useGrappleShortcuts() {
        return false;
    }

    @ConfigItem(
        keyName = "useBoats",
        name = "Use boats",
        description = "Whether to include small boats in the path<br>(e.g. the boat to Fishing Platform)",
        position = 4,
        section = sectionSettings
    )
    default boolean useBoats() {
        return true;
    }

    @ConfigItem(
        keyName = "useCanoes",
        name = "Use canoes",
        description = "Whether to include canoes in the path",
        position = 5,
        section = sectionSettings
    )
    default boolean useCanoes() {
        return false;
    }

    @ConfigItem(
        keyName = "useCharterShips",
        name = "Use charter ships",
        description = "Whether to include charter ships in the path",
        position = 6,
        section = sectionSettings
    )
    default boolean useCharterShips() {
        return false;
    }

    @ConfigItem(
        keyName = "useShips",
        name = "Use ships",
        description = "Whether to include passenger ships in the path<br>(e.g. the customs ships to Karamja)",
        position = 7,
        section = sectionSettings
    )
    default boolean useShips() {
        return true;
    }

    @ConfigItem(
        keyName = "useFairyRings",
        name = "Use fairy rings",
        description = "Whether to include fairy rings in the path.<br>" +
            "You must also have completed the required quests or miniquests",
        position = 8,
        section = sectionSettings
    )
    default boolean useFairyRings() {
        return true;
    }

    @ConfigItem(
        keyName = "useGnomeGliders",
        name = "Use gnome gliders",
        description = "Whether to include gnome gliders in the path",
        position = 9,
        section = sectionSettings
    )
    default boolean useGnomeGliders() {
        return true;
    }

    @ConfigItem(
        keyName = "useSpiritTrees",
        name = "Use spirit trees",
        description = "Whether to include spirit trees in the path",
        position = 10,
        section = sectionSettings
    )
    default boolean useSpiritTrees() {
        return true;
    }

    @ConfigItem(
        keyName = "useTeleportationLevers",
        name = "Use teleportation levers",
        description = "Whether to include teleportation levers in the path<br>(e.g. the lever from Edgeville to Wilderness)",
        position = 11,
        section = sectionSettings
    )
    default boolean useTeleportationLevers() {
        return true;
    }

    @ConfigItem(
        keyName = "useTeleportationPortals",
        name = "Use teleportation portals",
        description = "Whether to include teleportation portals in the path<br>(e.g. the portal from Ferox Enclave to Castle Wars)",
        position = 12,
        section = sectionSettings
    )
    default boolean useTeleportationPortals() {
        return true;
    }

    @ConfigItem(
        keyName = "cancelInstead",
        name = "Cancel instead of recalculating",
        description = "Whether the path should be cancelled rather than recalculated when the recalculate distance limit is exceeded",
        position = 13,
        section = sectionSettings
    )
    default boolean cancelInstead() {
        return false;
    }

    @Range(
        min = -1,
        max = 20000
    )
    @ConfigItem(
        keyName = "recalculateDistance",
        name = "Recalculate distance",
        description = "Distance from the path the player should be for it to be recalculated (-1 for never)",
        position = 14,
        section = sectionSettings
    )
    default int recalculateDistance() {
        return 10;
    }

    @Range(
        min = -1,
        max = 50
    )
    @ConfigItem(
        keyName = "finishDistance",
        name = "Finish distance",
        description = "Distance from the target tile at which the path should be ended (-1 for never)",
        position = 15,
        section = sectionSettings
    )
    default int reachedDistance() {
        return 5;
    }

    @ConfigItem(
        keyName = "showTileCounter",
        name = "Show tile counter",
        description = "Whether to display the number of tiles travelled, number of tiles remaining or disable counting",
        position = 16,
        section = sectionSettings
    )
    default TileCounter showTileCounter() {
        return TileCounter.DISABLED;
    }

    @ConfigItem(
        keyName = "tileCounterStep",
        name = "Tile counter step",
        description = "The number of tiles between the displayed tile counter numbers",
        position = 17,
        section = sectionSettings
    )
    default int tileCounterStep()
    {
        return 1;
    }

    /*@Units(
        value = Units.TICKS
    )
    @Range(
        min = 1,
        max = 30
    )
    @ConfigItem(
        keyName = "calculationCutoff",
        name = "Calculation cutoff",
        description = "The cutoff threshold in number of ticks (0.6 seconds) of no progress being<br>" +
            "made towards the path target before the calculation will be stopped",
        position = 18,
        section = sectionSettings
    )
    default int calculationCutoff()
    {
        return 5;
    }*/

    @ConfigSection(
        name = "Pathfinding Display",
        description = "Options for displaying the path on the world map, minimap and scene tiles",
        position = 19,
            closedByDefault = true
    )
    String sectionDisplay = "sectionDisplay";

    @ConfigItem(
        keyName = "drawMap",
        name = "Draw path on world map",
        description = "Whether the path should be drawn on the world map",
        position = 20,
        section = sectionDisplay
    )
    default boolean drawMap() {
        return true;
    }

    @ConfigItem(
        keyName = "drawMinimap",
        name = "Draw path on minimap",
        description = "Whether the path should be drawn on the minimap",
        position = 21,
        section = sectionDisplay
    )
    default boolean drawMinimap() {
        return true;
    }

    @ConfigItem(
        keyName = "drawTiles",
        name = "Draw path on tiles",
        description = "Whether the path should be drawn on the game tiles",
        position = 22,
        section = sectionDisplay
    )
    default boolean drawTiles() {
        return true;
    }

    @ConfigItem(
        keyName = "pathStyle",
        name = "Path style",
        description = "Whether to display the path as tiles or a segmented line",
        position = 23,
        section = sectionDisplay
    )
    default TileStyle pathStyle() {
        return TileStyle.TILES;
    }

    @ConfigSection(
        name = "Pathfinding Colors",
        description = "Colors for the path map, minimap and scene tiles",
        position = 24,
            closedByDefault = true
    )
    String sectionColours = "sectionColours";

    @Alpha
    @ConfigItem(
        keyName = "colourPath",
        name = "Path",
        description = "Colour of the path tiles on the world map, minimap and in the game scene",
        position = 25,
        section = sectionColours
    )
    default Color colourPath() {
        return new Color(255, 0, 0);
    }

    @Alpha
    @ConfigItem(
        keyName = "colourPathCalculating",
        name = "Calculating",
        description = "Colour of the path tiles while the pathfinding calculation is in progress",
        position = 26,
        section = sectionColours
    )
    default Color colourPathCalculating() {
        return new Color(0, 0, 255);
    }

    @Alpha
    @ConfigItem(
        keyName = "colourTransports",
        name = "Transports",
        description = "Colour of the transport tiles",
        position = 27,
        section = sectionColours
    )
    default Color colourTransports() {
        return new Color(0, 255, 0, 128);
    }

    @Alpha
    @ConfigItem(
        keyName = "colourCollisionMap",
        name = "Collision map",
        description = "Colour of the collision map tiles",
        position = 28,
        section = sectionColours
    )
    default Color colourCollisionMap() {
        return new Color(0, 128, 255, 128);
    }

    @Alpha
    @ConfigItem(
        keyName = "colourText",
        name = "Text",
        description = "Colour of the text of the tile counter and fairy ring codes",
        position = 29,
        section = sectionColours
    )
    default Color colourText() {
        return Color.WHITE;
    }

    @ConfigSection(
        name = "Pathfinding Debug Options",
        description = "Various options for debugging",
        position = 30,
        closedByDefault = true
    )
    String sectionDebug = "sectionDebug";

    @ConfigItem(
        keyName = "drawTransports",
        name = "Draw transports",
        description = "Whether transports should be drawn",
        position = 31,
        section = sectionDebug
    )
    default boolean drawTransports() {
        return false;
    }

    @ConfigItem(
        keyName = "drawCollisionMap",
        name = "Draw collision map",
        description = "Whether the collision map should be drawn",
        position = 32,
        section = sectionDebug
    )
    default boolean drawCollisionMap() {
        return false;
    }

    @ConfigItem(
        keyName = "drawDebugPanel",
        name = "Show debug panel",
        description = "Toggles displaying the pathfinding debug stats panel",
        position = 33,
        section = sectionDebug
    )
    default boolean drawDebugPanel() {
        return false;
    }
}
