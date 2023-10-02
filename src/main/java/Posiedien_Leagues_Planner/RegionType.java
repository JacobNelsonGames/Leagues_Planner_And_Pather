package Posiedien_Leagues_Planner;

import java.awt.*;

public enum RegionType
{
    NONE,
    GENERAL,
    MISTHALIN,
    KARAMJA,
    KANDARIN,
    ASGARNIA,
    FREMENNIK,
    KOUREND,
    WILDERNESS,
    MORYTANIA,
    TIRANNWN,
    DESERT;

    static public boolean GetRegionUnlocked(LeaguesPlannerConfig config, RegionType Type)
    {
        switch (Type)
        {
            case MISTHALIN:
                return config.MisthalinUnlocked();
            case KARAMJA:
                return config.KaramjaUnlocked();
            case KANDARIN:
                return config.KandarinUnlocked();
            case ASGARNIA:
                return config.AsgarniaUnlocked();
            case FREMENNIK:
                return config.FremennikUnlocked();
            case KOUREND:
                return config.KourendUnlocked();
            case WILDERNESS:
                return config.WildernessUnlocked();
            case MORYTANIA:
                return config.MorytaniaUnlocked();
            case TIRANNWN:
                return config.TirannwnUnlocked();
            case DESERT:
                return config.DesertUnlocked();
        }

        return true;
    }

    static public Color GetRegionColor(LeaguesPlannerConfig config, RegionType Type, boolean bConsiderUnlocked)
    {
        Color OutColor = Color.WHITE;

        switch (Type)
        {
            case MISTHALIN:
                OutColor = config.MisthalinColor();
                break;
            case KARAMJA:
                OutColor = config.KaramjaColor();
                break;
            case KANDARIN:
                OutColor = config.KandarinColor();
                break;
            case ASGARNIA:
                OutColor = config.AsgarniaColor();
                break;
            case FREMENNIK:
                OutColor = config.FremennikColor();
                break;
            case KOUREND:
                OutColor = config.KourendColor();
                break;
            case WILDERNESS:
                OutColor = config.WildernessColor();
                break;
            case MORYTANIA:
                OutColor = config.MorytaniaColor();
                break;
            case TIRANNWN:
                OutColor = config.TirannwnColor();
                break;
            case DESERT:
                OutColor = config.DesertColor();
                break;
        }

        int DebugColorAlpha = 0;
        if (bConsiderUnlocked && !GetRegionUnlocked(config, Type))
        {
            OutColor = new Color(42, 42, 42,100);
            DebugColorAlpha = Math.max(0, Math.min(config.DebugColorDisabledAlpha(), 255));
        }
        else
        {
            DebugColorAlpha = Math.max(0, Math.min(config.DebugColorAlpha(), 255));
        }

        OutColor = new Color(OutColor.getRed(), OutColor.getGreen(), OutColor.getBlue(), DebugColorAlpha);
        return OutColor;
    }
}
