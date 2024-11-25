package Posiedien_Leagues_Planner.pathfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import Posiedien_Leagues_Planner.LeaguesPlannerConfig;
import Posiedien_Leagues_Planner.PrimitiveIntHashMap;
import Posiedien_Leagues_Planner.Transport;
import Posiedien_Leagues_Planner.WorldPointUtil;

import static net.runelite.api.Constants.REGION_SIZE;

public class PathfinderConfig {
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    private final SplitFlagMap mapData;
    private final ThreadLocal<CollisionMap> map;
    private final Map<WorldPoint, List<Transport>> allTransports;
    @Getter
    private Map<WorldPoint, List<Transport>> transports;

    // Copy of transports with packed positions for the hotpath; lists are not copied and are the same reference in both maps
    @Getter
    private PrimitiveIntHashMap<List<Transport>> transportsPacked;

    private final Client client;
    final LeaguesPlannerConfig config;

    @Getter
    private long calculationCutoffMillis;
    @Getter
    private boolean avoidWilderness;
    private boolean useAgilityShortcuts,
        useGrappleShortcuts,
        useBoats,
        useCanoes,
        useCharterShips,
        useShips,
        useFairyRings,
        useGnomeGliders,
        useSpiritTrees,
        useTeleportationLevers,
        useTeleportationPortals;
    private int agilityLevel;
    private int rangedLevel;
    private int strengthLevel;
    private int prayerLevel;
    private int woodcuttingLevel;
    private Map<Quest, QuestState> questStates = new HashMap<>();

    public PathfinderConfig(SplitFlagMap mapData, Map<WorldPoint, List<Transport>> transports, Client client,
                            LeaguesPlannerConfig config) {
        this.mapData = mapData;
        this.map = ThreadLocal.withInitial(() -> new CollisionMap(this.mapData));
        this.allTransports = transports;
        this.transports = new HashMap<>(allTransports.size());
        this.transportsPacked = new PrimitiveIntHashMap<>(allTransports.size());
        this.client = client;
        this.config = config;


        // Go through every flag map
        if (config != null)
        {
            for (FlagMap regionMap : mapData.regionMaps)
            {
                if (regionMap == null)
                {
                    continue;
                }
                for (int x = 0; x < REGION_SIZE; ++x)
                {
                    int AdjustedX = regionMap.minX + x;
                    for (int y = 0; y < REGION_SIZE; ++y)
                    {
                        int AdjustedY = regionMap.minY + y;
                        for (int z = 0; z < regionMap.getPlaneCount(); ++z)
                        {
                            if (mapData.bCanceled)
                            {
                                return;
                            }
                            WorldPoint ConvertedPoint = new WorldPoint(AdjustedX, AdjustedY, z);

                            // Not in region, mark as blocked
                            if (!config.RegionData.IsTileInUnlockedRegion(config, ConvertedPoint))
                            {
                                regionMap.set(AdjustedX, AdjustedY, z, 0, false);
                                regionMap.set(AdjustedX, AdjustedY, z, 1, false);
                            }
                        }
                    }
                }
            }
        }
    }

    public CollisionMap getMap() {
        return map.get();
    }

    public void refresh() {
        //calculationCutoffMillis = config.calculationCutoff() * Constants.GAME_TICK_LENGTH;
        if (GameState.LOGGED_IN.equals(client.getGameState()))
        {
            avoidWilderness = config.avoidWilderness();
            useAgilityShortcuts = config.useAgilityShortcuts();
            useGrappleShortcuts = config.useGrappleShortcuts();
            useBoats = config.useBoats();
            useCanoes = config.useCanoes();
            useCharterShips = config.useCharterShips();
            useShips = config.useShips();
            useFairyRings = config.useFairyRings();
            useSpiritTrees = config.useSpiritTrees();
            useGnomeGliders = config.useGnomeGliders();
            useTeleportationLevers = config.useTeleportationLevers();
            useTeleportationPortals = config.useTeleportationPortals();

            agilityLevel = client.getBoostedSkillLevel(Skill.AGILITY);
            rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
            strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
            prayerLevel = client.getBoostedSkillLevel(Skill.PRAYER);
            woodcuttingLevel = client.getBoostedSkillLevel(Skill.WOODCUTTING);

            refreshTransportData();
        }
        // Not logged in
        else
        {
            avoidWilderness = false;
            useAgilityShortcuts = true;
            useGrappleShortcuts = true;
            useBoats = true;
            useCanoes = true;
            useCharterShips = true;
            useShips = true;
            useFairyRings = false;
            useSpiritTrees = false;
            useGnomeGliders = false;
            useTeleportationLevers = false;
            useTeleportationPortals = false;

            agilityLevel = 99;
            rangedLevel = 99;
            strengthLevel = 99;
            prayerLevel = 99;
            woodcuttingLevel = 99;

            refreshTransportData();
        }
    }

    private void refreshTransportData() {
        if (!Thread.currentThread().equals(client.getClientThread())) {
            return; // Has to run on the client thread; data will be refreshed when path finding commences
        }

        useFairyRings &= !QuestState.NOT_STARTED.equals(getQuestState(Quest.FAIRYTALE_II__CURE_A_QUEEN));
        useGnomeGliders &= QuestState.FINISHED.equals(getQuestState(Quest.THE_GRAND_TREE));
        useSpiritTrees &= QuestState.FINISHED.equals(getQuestState(Quest.TREE_GNOME_VILLAGE));

        transports.clear();
        transportsPacked.clear();
        for (Map.Entry<WorldPoint, List<Transport>> entry : allTransports.entrySet()) {
            List<Transport> usableTransports = new ArrayList<>(entry.getValue().size());
            for (Transport transport : entry.getValue()) {
                for (Quest quest : transport.getQuests()) {
                    try {
                        questStates.put(quest, getQuestState(quest));
                    } catch (NullPointerException ignored) {
                    }
                }

                if (useTransport(transport)) {
                    usableTransports.add(transport);
                }
            }

            WorldPoint point = entry.getKey();
            transports.put(point, usableTransports);
            transportsPacked.put(WorldPointUtil.packWorldPoint(point), usableTransports);
        }
    }

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 || WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }

    public static boolean isInWilderness(int packedPoint) {
        return WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_ABOVE_GROUND) == 0 || WorldPointUtil.distanceToArea(packedPoint, WILDERNESS_UNDERGROUND) == 0;
    }

    public boolean avoidWilderness(int packedPosition, int packedNeightborPosition, boolean targetInWilderness) {
        return avoidWilderness && !isInWilderness(packedPosition) && isInWilderness(packedNeightborPosition) && !targetInWilderness;
    }

    public QuestState getQuestState(Quest quest)
    {
        if (client == null)
        {
            return QuestState.FINISHED;
        }

        return quest.getState(client);
    }

    private boolean completedQuests(Transport transport) {
        for (Quest quest : transport.getQuests()) {
            if (!QuestState.FINISHED.equals(questStates.getOrDefault(quest, QuestState.NOT_STARTED))) {
                return false;
            }
        }
        return true;
    }

    private boolean useTransport(Transport transport) {
        final int transportAgilityLevel = transport.getRequiredLevel(Skill.AGILITY);
        final int transportRangedLevel = transport.getRequiredLevel(Skill.RANGED);
        final int transportStrengthLevel = transport.getRequiredLevel(Skill.STRENGTH);
        final int transportPrayerLevel = transport.getRequiredLevel(Skill.PRAYER);
        final int transportWoodcuttingLevel = transport.getRequiredLevel(Skill.WOODCUTTING);

        final boolean isAgilityShortcut = transport.isAgilityShortcut();
        final boolean isGrappleShortcut = transport.isGrappleShortcut();
        final boolean isBoat = transport.isBoat();
        final boolean isCanoe = transport.isCanoe();
        final boolean isCharterShip = transport.isCharterShip();
        final boolean isShip = transport.isShip();
        final boolean isFairyRing = transport.isFairyRing();
        final boolean isGnomeGlider = transport.isGnomeGlider();
        final boolean isSpiritTree = transport.isSpiritTree();
        final boolean isTeleportationLever = transport.isTeleportationLever();
        final boolean isTeleportationPortal = transport.isTeleportationPortal();
        final boolean isPrayerLocked = transportPrayerLevel > 1;
        final boolean isQuestLocked = transport.isQuestLocked();

        if (isAgilityShortcut) {
            if (!useAgilityShortcuts || agilityLevel < transportAgilityLevel) {
                return false;
            }

            if (isGrappleShortcut && (!useGrappleShortcuts || rangedLevel < transportRangedLevel || strengthLevel < transportStrengthLevel)) {
                return false;
            }
        }

        if (isBoat && !useBoats) {
            return false;
        }

        if (isCanoe && (!useCanoes || woodcuttingLevel < transportWoodcuttingLevel)) {
            return false;
        }

        if (isCharterShip && !useCharterShips) {
            return false;
        }

        if (isShip && !useShips) {
            return false;
        }

        if (isFairyRing && !useFairyRings) {
            return false;
        }

        if (isGnomeGlider && !useGnomeGliders) {
            return false;
        }

        if (isSpiritTree && !useSpiritTrees) {
            return false;
        }

        if (isTeleportationLever && !useTeleportationLevers) {
            return false;
        }

        if (isTeleportationPortal && !useTeleportationPortals) {
            return false;
        }

        if (isPrayerLocked && prayerLevel < transportPrayerLevel) {
            return false;
        }

        if (isQuestLocked && !completedQuests(transport)) {
            return false;
        }

        return true;
    }
}
