package Posiedien_Leagues_Planner;

import java.awt.*;

public enum TaskDifficulty
{
    NONE,
    BEGINNER,
    EASY,
    MEDIUM,
    HARD,
    ELITE,
    MASTER,
    CUSTOM;

    static Color GetTaskDifficultyColor(TaskDifficulty InTaskDiff)
    {
        switch (InTaskDiff)
        {
            case BEGINNER:
                return new Color(121, 67, 3,255);
            case EASY:
                return new Color(255, 142, 9,255);
            case MEDIUM:
                return new Color(190, 190, 190,255);
            case HARD:
                return new Color(161, 250, 11,255);
            case ELITE:
                return new Color(6, 193, 255,255);
            case MASTER:
                return new Color(255, 0, 0, 255);
            case CUSTOM:
                return new Color(255, 242, 0, 255);
        }
        return new Color(253, 253, 253, 255);
    }
}
