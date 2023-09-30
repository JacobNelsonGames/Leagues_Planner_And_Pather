
package Posiedien_Leagues_Planner;

import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.worldmap.WorldMap;
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

    private static final ImageIcon LOCATE_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarker.png"));
    private static final ImageIcon LOCATE_BEGINNER_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerBeginner.png"));

    private static final ImageIcon LOCATE_EASY_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerEasy.png"));

    private static final ImageIcon LOCATE_MEDIUM_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerMedium.png"));

    private static final ImageIcon LOCATE_HARD_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerHard.png"));

    private static final ImageIcon LOCATE_ELITE_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerElite.png"));

    private static final ImageIcon LOCATE_MASTER_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarkerMaster.png"));

    private static final ImageIcon LOCATE_CUSTOM_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/taskMarker.png"));

    private static final ImageIcon CLOSE_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/close.png"));

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
            DistanceText = "?";
        }
        else
        {
            DistanceText = String.valueOf(DistanceFromPlayer);
        }

        Color color = Color.LIGHT_GRAY;

        JPanel ButtonCombo = new JPanel();
        ButtonCombo.setBorder(new EmptyBorder(0, 0, 0, 0));
        ButtonCombo.setLayout(new BorderLayout());

        JButton markHiddenButton = new JButton();
        markHiddenButton.setBorder(new EmptyBorder(10, 0, 10, 0));

        boolean bIsPlanned = plugin.config.UserData.PlannedTasks.containsKey(taskData.GUID);
        if (bIsPlanned)
        {
            color = TaskDifficulty.GetTaskDifficultyColor(taskData.Difficulty);
        }

        boolean bIsHidden = plugin.config.UserData.HiddenTasks.contains(taskData.GUID);
        if (bIsHidden)
        {
            markHiddenButton.setIcon(HIDDEN_ICON);
            color = Color.BLACK;
        }
        else
        {
            markHiddenButton.setIcon(SHOWN_ICON);
        }

        String LabelString = "Dist: " + DistanceText + ", " + taskData.TaskName;

        JLabel nameLabel = new JLabel("<html>"+ LabelString +"</html>");
        nameLabel.setForeground(color);
        add(nameLabel, BorderLayout.CENTER);

        markHiddenButton.addActionListener(e ->
        {
            boolean bLocalIsHidden = plugin.config.UserData.HiddenTasks.contains(taskData.GUID);
            if (bLocalIsHidden)
            {
                plugin.config.UserData.HiddenTasks.remove(taskData.GUID);
            }
            else
            {
                plugin.config.UserData.HiddenTasks.add(taskData.GUID);
                plugin.config.UserData.PlannedTasks.remove(taskData.GUID);
            }
            plugin.QueueRefresh();
        });
        ButtonCombo.add(markHiddenButton, BorderLayout.NORTH);

        JPanel ButtonComboContainer = new JPanel();
        ButtonComboContainer.setBorder(new EmptyBorder(0, 0, 0, 0));
        ButtonComboContainer.setLayout(new BorderLayout());

        JPanel ButtonCombo2 = new JPanel();
        ButtonCombo2.setBorder(new EmptyBorder(0, 0, 0, 0));
        ButtonCombo2.setLayout(new BorderLayout());

        JButton locateTaskButton = new JButton();
        locateTaskButton.setBorder(new EmptyBorder(3, 0, 3, 0));

        switch (taskData.Difficulty)
        {
            case BEGINNER:
                locateTaskButton.setIcon(LOCATE_BEGINNER_ICON);
                break;
            case EASY:
                locateTaskButton.setIcon(LOCATE_EASY_ICON);
                break;
            case MEDIUM:
                locateTaskButton.setIcon(LOCATE_MEDIUM_ICON);
                break;
            case HARD:
                locateTaskButton.setIcon(LOCATE_HARD_ICON);
                break;
            case ELITE:
                locateTaskButton.setIcon(LOCATE_ELITE_ICON);
                break;
            case MASTER:
                locateTaskButton.setIcon(LOCATE_MASTER_ICON);
                break;
            case CUSTOM:
                locateTaskButton.setIcon(LOCATE_ICON);
                break;
        }

        locateTaskButton.addActionListener(e ->
        {
            plugin.FocusOnTaskOnWorldMap(taskData);
        });

        JButton removeTaskFromPlanButton = new JButton();
        removeTaskFromPlanButton.setBorder(new EmptyBorder(3, 0, 3, 0));
        removeTaskFromPlanButton.setIcon(CLOSE_ICON);

        removeTaskFromPlanButton.addActionListener(e ->
        {
            boolean bLocalIsPlanned = plugin.config.UserData.PlannedTasks.containsKey(taskData.GUID);
            if (bLocalIsPlanned)
            {
                plugin.config.UserData.PlannedTasks.remove(taskData.GUID);
            }
            else if (taskData.bIsCustomTask)
            {
                plugin.config.UserData.CustomTasks.remove(taskData.GUID);
                plugin.bMapDisplayPointsDirty = true;
            }
            plugin.QueueRefresh();
        });

        ButtonCombo2.add(locateTaskButton, BorderLayout.NORTH);
        if (bIsPlanned || taskData.bIsCustomTask)
        {
            ButtonCombo2.add(removeTaskFromPlanButton, BorderLayout.SOUTH);
        }

        if (bIsPlanned)
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
                    plugin.config.UserData.PlannedTasks.put(taskData.GUID, new TaskSortData(Integer.valueOf(((TextField)(e.getSource())).getText()), taskData.bIsCustomTask));
                    plugin.config.UserData.HiddenTasks.remove(taskData.GUID);
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

                plugin.config.UserData.PlannedTasks.put(taskData.GUID, new TaskSortData(CurrentOrder + 1, taskData.bIsCustomTask));
                plugin.config.UserData.HiddenTasks.remove(taskData.GUID);
                plugin.bMapDisplayPointsDirty = true;
                plugin.QueueRefresh();
            });
            ButtonCombo.add(startButton, BorderLayout.SOUTH);
        }

        ButtonComboContainer.add(ButtonCombo, BorderLayout.EAST);
        ButtonComboContainer.add(ButtonCombo2, BorderLayout.WEST);
        add(ButtonComboContainer, BorderLayout.LINE_END);
    }
}