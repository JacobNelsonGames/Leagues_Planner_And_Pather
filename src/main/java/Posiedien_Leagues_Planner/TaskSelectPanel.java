
package Posiedien_Leagues_Planner;

import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TaskSelectPanel extends JPanel
{
    private static final ImageIcon START_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/start.png"));
    private static final ImageIcon UNCHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/unchecked.png"));
    private static final ImageIcon CHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/checked.png"));
    private static final ImageIcon HIDDEN_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/HIDDEN.png"));
    private static final ImageIcon SHOWN_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/SHOWN.png"));

    public PosiedienLeaguesPlannerPlugin plugin;
    public TaskData taskData;

    public TaskSelectPanel(PosiedienLeaguesPlannerPlugin InPlugin, TaskData inTask)
    {
        this.plugin = InPlugin;
        this.taskData = inTask;

        setLayout(new BorderLayout(3, 0));
        setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 70));

        double ShortestDistance = 1000000;
        Player player = plugin.client.getLocalPlayer();
        if (player != null)
        {
            WorldPoint playerLocation = player.getWorldLocation();

            for (WorldPoint CurrentLocation : taskData.Locations)
            {
                double TaskDistance = CurrentLocation.distanceTo(playerLocation);
                if (TaskDistance < ShortestDistance)
                {
                    ShortestDistance = TaskDistance;
                }
            }

            for (WorldPoint CurrentLocation : taskData.OverworldLocations)
            {
                double TaskDistance = CurrentLocation.distanceTo(playerLocation);
                if (TaskDistance < ShortestDistance)
                {
                    ShortestDistance = TaskDistance;
                }
            }
        }

        double DistanceFromPlayer = ShortestDistance;
        String DistanceText;
        if (DistanceFromPlayer > 900000)
        {
            DistanceFromPlayer = 0;
        }

        if (DistanceFromPlayer == 0)
        {
            DistanceText = "Unknown";
        }
        else
        {
            DistanceText = String.valueOf(DistanceFromPlayer);
        }

        boolean DebugSortEnabled = false;

        double DebugSortValue = 0;
        if (DebugSortEnabled)
        {
            TaskSortMethod SortConfig = plugin.config.TaskSort();

            if (SortConfig == TaskSortMethod.DISTANCE)
            {
                DebugSortValue = DistanceFromPlayer;
            }
            else if (SortConfig == TaskSortMethod.DIFFICULTY)
            {
                DebugSortValue = inTask.Difficulty.hashCode();
            }
        }

        String LabelString = "Dist: " + DistanceText + " \n" + taskData.TaskName;

        if (DebugSortEnabled)
        {
            LabelString = LabelString + " DebugSort: " + DebugSortValue;
        }

        JLabel nameLabel = new JLabel("<html>"+ LabelString +"</html>");

        Color color = TaskDifficulty.GetTaskDifficultyColor(taskData.Difficulty);

        JButton markHiddenButton = new JButton();
        markHiddenButton.setBorder(new EmptyBorder(10, 0, 10, 0));
        String ActionName = taskData.TaskName;
        //if (questHelperPlugin.getConfig().gethiddenTaskSection().contains(ActionName))
        {
            markHiddenButton.setIcon(HIDDEN_ICON);
            //color = Color.BLACK;
        }
        //else
        //{
        //    markHiddenButton.setIcon(SHOWN_ICON);
        //}

        nameLabel.setForeground(color);
        add(nameLabel, BorderLayout.CENTER);

        JPanel ButtonCombo = new JPanel();
        ButtonCombo.setBorder(new EmptyBorder(0, 0, 0, 0));
        ButtonCombo.setLayout(new BorderLayout());

        markHiddenButton.addActionListener(e ->
        {
            //Set<String> HiddenActions = questHelperPlugin.getConfig().gethiddenTaskSection();
            //if (HiddenActions.contains(ActionName))
            //{
            //    HiddenActions.remove(ActionName);
            //    questHelperPlugin.getConfigManager().setConfiguration("leaguesOptimizer", "gethiddenTaskSection", HiddenActions);
            //}
            //else
            //{
            //    HiddenActions.add(ActionName);
            //    questHelperPlugin.getConfigManager().setConfiguration("leaguesOptimizer", "gethiddenTaskSection", HiddenActions);
           // }
        });
        ButtonCombo.add(markHiddenButton, BorderLayout.NORTH);

        JButton startButton = new JButton();
        startButton.setBorder(new EmptyBorder(10, 0, 10, 0));
        startButton.setIcon(START_ICON);
        startButton.addActionListener(e ->
        {
            //questHelperPlugin.nextSelectedAction = actionHelper;
        });
        ButtonCombo.add(startButton, BorderLayout.SOUTH);
        add(ButtonCombo, BorderLayout.LINE_END);
    }
}