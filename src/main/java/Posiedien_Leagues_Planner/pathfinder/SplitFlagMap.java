package Posiedien_Leagues_Planner.pathfinder;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import Posiedien_Leagues_Planner.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

import static net.runelite.api.Constants.REGION_SIZE;

public class SplitFlagMap implements Runnable{
    @Getter
    private static RegionExtent regionExtents;

    @Getter
    private final byte[] regionMapPlaneCounts;
    // Size is automatically chosen based on the max extents of the collision data
    final FlagMap[] regionMaps;
    private final int widthInclusive;
    private final PosiedienLeaguesPlannerPlugin plugin;

    private LeaguesPlannerConfig config;

    public SplitFlagMap(Map<Integer, byte[]> compressedRegions, PosiedienLeaguesPlannerPlugin plugin, LeaguesPlannerConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        widthInclusive = regionExtents.getWidth() + 1;
        final int heightInclusive = regionExtents.getHeight() + 1;
        regionMaps = new FlagMap[widthInclusive * heightInclusive];
        regionMapPlaneCounts = new byte[regionMaps.length];

        for (Map.Entry<Integer, byte[]> entry : compressedRegions.entrySet()) {
            final int pos = entry.getKey();
            final int x = unpackX(pos);
            final int y = unpackY(pos);
            final int index = getIndex(x, y);
            FlagMap flagMap = new FlagMap(x * REGION_SIZE, y * REGION_SIZE, entry.getValue());
            regionMaps[index] = flagMap;
            regionMapPlaneCounts[index] = flagMap.getPlaneCount();
        }
    }

    public boolean get(int x, int y, int z, int flag) {
        final int index = getIndex(x / REGION_SIZE, y / REGION_SIZE);
        if (index < 0 || index >= regionMaps.length || regionMaps[index] == null) {
            return false;
        }

        return regionMaps[index].get(x, y, z, flag);
    }

    private int getIndex(int regionX, int regionY) {
        return (regionX - regionExtents.getMinX()) + (regionY - regionExtents.getMinY()) * widthInclusive;
    }

    public static int unpackX(int position) {
        return position & 0xFFFF;
    }

    public static int unpackY(int position) {
        return (position >> 16) & 0xFFFF;
    }

    public static int packPosition(int x, int y) {
        return (x & 0xFFFF) | ((y & 0xFFFF) << 16);
    }

    public static SplitFlagMap fromResources(PosiedienLeaguesPlannerPlugin plugin, LeaguesPlannerConfig config)
    {
        Map<Integer, byte[]> compressedRegions = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(PosiedienLeaguesPlannerPlugin.class.getResourceAsStream("/collision-map.zip"))) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = 0;
            int maxY = 0;

            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String[] n = entry.getName().split("_");
                final int x = Integer.parseInt(n[0]);
                final int y = Integer.parseInt(n[1]);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);

                compressedRegions.put(SplitFlagMap.packPosition(x, y), Util.readAllBytes(in));
            }

            regionExtents = new RegionExtent(minX, minY, maxX, maxY);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new SplitFlagMap(compressedRegions, plugin, config);
    }

    @Override
    public void run()
    {
        Map<WorldPoint, List<Transport>> transports = Transport.loadAllFromResources();
        PathfinderConfig newPathfinderConfig = new PathfinderConfig(this, transports, plugin.client, config);

        if (!bCanceled)
        {
            EventQueue.invokeLater(() ->
                    {
                        plugin.pathfinderConfig = newPathfinderConfig;
                    }
            );
        }
    }

    boolean bCanceled = false;
    public void cancel()
    {
        bCanceled = true;
    }

    @RequiredArgsConstructor
    @Getter
    public static class RegionExtent {
        public final int minX, minY, maxX, maxY;

        public int getWidth() {
            return maxX - minX;
        }

        public int getHeight() {
            return maxY - minY;
        }
    }
}
