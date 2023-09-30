
package Posiedien_Leagues_Planner;

import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.UUID;

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

        String LabelString = "Dist: " + DistanceText + ", " + taskData.TaskName;

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

        if (plugin.config.UserData.PlannedTasks.containsKey(taskData.GUID))
        {
            /* Priority input */
            TextField activeOrder = new TextField();
            activeOrder.setPreferredSize(new Dimension(markHiddenButton.getWidth(), 30));
            activeOrder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            activeOrder.setText(String.valueOf(plugin.config.UserData.PlannedTasks.get(taskData.GUID).SortPriority));

            activeOrder.addTextListener(new TextListener()
            {
                @Override
                public void textValueChanged(TextEvent e)
                {

                    try
                    {
                        int i = Integer.parseInt(((TextField)(e.getSource())).getText());
                    }
                    catch (NumberFormatException nfe)
                    {
                        plugin.QueueRefresh();
                        return;
                    }


                    plugin.config.UserData.PlannedTasks.remove(taskData.GUID);
                    plugin.config.UserData.PlannedTasks.put(taskData.GUID, new TaskSortData(Integer.valueOf(((TextField)(e.getSource())).getText())));
                    plugin.QueueRefresh();
                }

            });
            ButtonCombo.add(activeOrder, BorderLayout.SOUTH);
        }
        else
        {
            JButton startButton = new JButton();
            startButton.setBorder(new EmptyBorder(10, 0, 10, 0));
            startButton.setIcon(START_ICON);
            startButton.addActionListener(e ->
            {
                // Find greatest value of planned tasks
                int CurrentOrder = 0;

                for (HashMap.Entry<UUID, TaskSortData> mapElement : plugin.config.UserData.PlannedTasks.entrySet())
                {
                    if (mapElement.getValue().SortPriority > CurrentOrder)
                    {
                        CurrentOrder = mapElement.getValue().SortPriority;
                    }

                }

                plugin.config.UserData.PlannedTasks.put(taskData.GUID, new TaskSortData(CurrentOrder + 1));
                plugin.bMapDisplayPointsDirty = true;
                plugin.QueueRefresh();
            });
            ButtonCombo.add(startButton, BorderLayout.SOUTH);
        }
        add(ButtonCombo, BorderLayout.LINE_END);
    }
}