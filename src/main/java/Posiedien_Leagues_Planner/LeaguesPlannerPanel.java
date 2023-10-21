package Posiedien_Leagues_Planner;

import Posiedien_Leagues_Planner.pathfinder.Pathfinder;
import Posiedien_Leagues_Planner.pathfinder.PathfinderConfig;
import Posiedien_Leagues_Planner.pathfinder.SplitFlagMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class LeaguesPlannerPanel extends PluginPanel
{
    private LeaguesPlannerConfig config = null;
    private PosiedienLeaguesPlannerPlugin plugin = null;
    private final FixedWidthPanel taskOverviewWrapper = new FixedWidthPanel();

    private JPanel NoSearchPanel = new JPanel();
    private final FixedWidthPanel taskListWrapper = new FixedWidthPanel();

    private static final ImageIcon START_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/start.png"));
    private static final ImageIcon UNCHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/unchecked.png"));
    private static final ImageIcon CHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/checked.png"));

    private final JPanel allDropdownSections = new JPanel();
    private final JPanel allDropdownSections2 = new JPanel();
    private final JComboBox<Enum> filterDropdown;
    private final JComboBox<Enum> filterDropdown2;
    private final JComboBox<Enum> filterDropdown3;
    private final JComboBox<Enum> filterDropdown4;
    private JPanel searchTasksPanel;


    private final FixedWidthPanel regionsUnlockedListPanel = new FixedWidthPanel();
    private final FixedWidthPanel taskListPanel = new FixedWidthPanel();
    private final JScrollPane scrollableContainer;
    private final JScrollPane regionUnlockedScrollableContainer;
    private final int DROPDOWN_HEIGHT = 20;

    public Thread longRunningTask = null;
    public SplitFlagMap NewMap = null;


    JButton autostartButton = new JButton();
    public ActionListener autoRecalculateCallback = e->
            {
                plugin.enableAutoRecalculate = !plugin.enableAutoRecalculate;
                if (plugin.enableAutoRecalculate)
                {
                    autostartButton.setIcon(CHECKED_ICON);
                }
                else
                {
                    autostartButton.setIcon(UNCHECKED_ICON);
                }

                plugin.QueueRefresh();
            };


    JButton misthalinUnlockedButton = new JButton();
    public ActionListener misthalinUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "MisthalinUnlocked",
                !config.MisthalinUnlocked());

        if (config.MisthalinUnlocked())
        {
            misthalinUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            misthalinUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    public void RecreateBoundData()
    {
        if (NewMap != null && longRunningTask != null && longRunningTask.isAlive())
        {
            NewMap.cancel();
            longRunningTask.interrupt();
        }

        NewMap = SplitFlagMap.fromResources(plugin, config);

        longRunningTask = new Thread(NewMap);
        longRunningTask.start();
    }

    JButton karamjaUnlockedButton = new JButton();

    public ActionListener karamjaUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "KaramjaUnlocked",
                !config.KaramjaUnlocked());

        if (config.KaramjaUnlocked())
        {
            karamjaUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            karamjaUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton kandarinUnlockedButton = new JButton();
    public ActionListener kandarinUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "KandarinUnlocked",
                !config.KandarinUnlocked());

        if (config.KandarinUnlocked())
        {
            kandarinUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            kandarinUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton asgarniaUnlockedButton = new JButton();
    public ActionListener asgarniaUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "AsgarniaUnlocked",
                !config.AsgarniaUnlocked());

        if (config.AsgarniaUnlocked())
        {
            asgarniaUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            asgarniaUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton fremennikUnlockedButton = new JButton();
    public ActionListener fremennikUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "FremennikUnlocked",
                !config.FremennikUnlocked());

        if (config.FremennikUnlocked())
        {
            fremennikUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            fremennikUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton kourendUnlockedButton = new JButton();
    public ActionListener kourendUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "KourendUnlocked",
                !config.KourendUnlocked());

        if (config.KourendUnlocked())
        {
            kourendUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            kourendUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton wildernessUnlockedButton = new JButton();
    public ActionListener wildernessUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "WildernessUnlocked",
                !config.WildernessUnlocked());

        if (config.WildernessUnlocked())
        {
            wildernessUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            wildernessUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton morytaniaUnlockedButton = new JButton();
    public ActionListener morytaniaUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "MorytaniaUnlocked",
                !config.MorytaniaUnlocked());

        if (config.MorytaniaUnlocked())
        {
            morytaniaUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            morytaniaUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton tirannwnUnlockedButton = new JButton();
    public ActionListener tirannwnUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "TirannwnUnlocked",
                !config.TirannwnUnlocked());

        if (config.TirannwnUnlocked())
        {
            tirannwnUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            tirannwnUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };

    JButton desertUnlockedButton = new JButton();
    public ActionListener desertUnlockedCallback = e->
    {
        plugin.getConfigManager().setConfiguration(
                PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                "DesertUnlocked",
                !config.DesertUnlocked());

        if (config.DesertUnlocked())
        {
            desertUnlockedButton.setIcon(CHECKED_ICON);
        }
        else
        {
            desertUnlockedButton.setIcon(UNCHECKED_ICON);
        }

        RecreateBoundData();
        plugin.QueueRefresh();
    };


    public JPanel CreateClickableOption(JButton inJButton, String OptionName, boolean bInitialValue, Color ForegroundColor, Color BackgroundColor, ActionListener Callback)
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        buttonPanel.setLayout(new BorderLayout());

        JLabel buttonPanelLabel = new JLabel();
        buttonPanelLabel.setText(OptionName);
        buttonPanelLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        buttonPanelLabel.setLayout(new BorderLayout(0, BORDER_OFFSET));

        int R = ForegroundColor.getRed();
        int G = ForegroundColor.getGreen();
        int B = ForegroundColor.getBlue();

        // Adjust to make sure its bright
        int MaxVal = Math.max(Math.max(R, G), B);
        int Adjuster = 255 - MaxVal;

        Color ConvertedColor = new Color(R + Adjuster,G + Adjuster,B + Adjuster , 255);

        buttonPanelLabel.setForeground(ConvertedColor);
        buttonPanelLabel.setBackground(BackgroundColor);
        buttonPanel.add(buttonPanelLabel, BorderLayout.WEST);

        if (bInitialValue)
        {
            inJButton.setIcon(CHECKED_ICON);
        }
        else
        {
            inJButton.setIcon(UNCHECKED_ICON);
        }

        inJButton.setBorder(new EmptyBorder(5, 10, 5, 10));
        inJButton.setLayout(new BorderLayout(0, BORDER_OFFSET));
        inJButton.addActionListener(Callback);
        buttonPanel.add(inJButton, BorderLayout.EAST);

        return buttonPanel;
    }


    public ItemListener filterCallbackDifficulty = e->
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            Enum source = (Enum) e.getItem();
            plugin.getConfigManager().setConfiguration(
                    PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                    "FilteredDifficulty",
                    source);

            plugin.QueueRefresh();
        }

    };

    public ItemListener filterCallbackOther = e->
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            Enum source = (Enum) e.getItem();
            plugin.getConfigManager().setConfiguration(
                    PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                    "FilteredOther",
                    source);

            plugin.QueueRefresh();
        }

    };

    public ItemListener filterCallbackRequirements = e->
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            Enum source = (Enum) e.getItem();
            plugin.getConfigManager().setConfiguration(
                    PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                    "FilteredRequirements",
                    source);

            plugin.QueueRefresh();
        }

    };

    public ItemListener filterCallbackSort = e->
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            Enum source = (Enum) e.getItem();
            plugin.getConfigManager().setConfiguration(
                    PosiedienLeaguesPlannerPlugin.CONFIG_GROUP,
                    "TaskSort",
                    source);

            plugin.QueueRefresh();
        }

    };

    public LeaguesPlannerPanel(PosiedienLeaguesPlannerPlugin plugin)
    {
        super(false);

        this.plugin = plugin;
        this.config = plugin.config;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        /* Setup overview panel */
        JPanel titlePanel = new JPanel();
        titlePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        titlePanel.setLayout(new BorderLayout());

        JLabel title = new JLabel();
        title.setText("Posiedien's Leagues Planner");
        title.setForeground(Color.WHITE);
        titlePanel.add(title, BorderLayout.WEST);

        // Options
        final JPanel viewControls = new JPanel(new GridLayout(1, 3, 5, 0));
        viewControls.setBackground(ColorScheme.DARK_GRAY_COLOR);

        titlePanel.add(viewControls, BorderLayout.EAST);

        JLabel pleaseLoginLabel = new JLabel();
        pleaseLoginLabel.setForeground(Color.GRAY);
        pleaseLoginLabel.setText("<html><body style='text-align:left'>Please log in to use the leagues planner plugin" +
                ".</body></html>");

        NoSearchPanel.setLayout(new BorderLayout());
        NoSearchPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        NoSearchPanel.add(pleaseLoginLabel);
        NoSearchPanel.setVisible(false);

/*
        JPanel reloadActionsPanel = new JPanel();
        reloadActionsPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        reloadActionsPanel.setLayout(new BorderLayout());
        JLabel reloadallActions = new JLabel();
        reloadallActions.setText("Reload all Actions -> ");
        reloadallActions.setForeground(Color.WHITE);
        reloadallActions.setBorder(new EmptyBorder(5, 10, 5, 10));
        reloadallActions.setLayout(new BorderLayout(0, BORDER_OFFSET));
        reloadActionsPanel.add(reloadallActions, BorderLayout.WEST);
        JButton reloadbutton = new JButton();
        reloadbutton.setIcon(START_ICON);
        reloadbutton.setBorder(new EmptyBorder(5, 10, 5, 10));
        reloadbutton.setLayout(new BorderLayout(0, BORDER_OFFSET));
        reloadbutton.addActionListener(e ->
        {
            plugin.QueueRefresh();
            plugin.CurrentStepSelected = null;
            if (plugin.getSelectedQuest() != null)
            {
                plugin.getSelectedQuest().setCurrentStep(questHelperPlugin.OldStepSelected);
            }

            plugin.startUpQuest(null);
            plugin.recreateActions();
            plugin.loadQuestList = true;
            plugin.MarkUIAndActionRefresh(true, true);
        });
        reloadActionsPanel.add(reloadbutton, BorderLayout.EAST);
*/

        JPanel recalculatePanel = new JPanel();
        recalculatePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        recalculatePanel.setLayout(new BorderLayout());

        JLabel RecaculateLabel = new JLabel();
        RecaculateLabel.setText("Refresh Panel -> ");
        RecaculateLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        RecaculateLabel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        RecaculateLabel.setForeground(Color.WHITE);
        recalculatePanel.add(RecaculateLabel, BorderLayout.WEST);

        JButton startButton = new JButton();
        startButton.setIcon(START_ICON);
        startButton.setBorder(new EmptyBorder(5, 10, 5, 10));
        startButton.setLayout(new BorderLayout(0, BORDER_OFFSET));
        startButton.addActionListener(e ->
        {
            plugin.QueueRefresh();
            /*plugin.loadQuestList = true;
            plugin.updateShortestPath();
            plugin.MarkUIAndActionRefresh(true, true);*/
        });
        recalculatePanel.add(startButton, BorderLayout.EAST);

        searchTasksPanel = new JPanel();
        searchTasksPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        searchTasksPanel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        searchTasksPanel.add(NoSearchPanel, BorderLayout.SOUTH);

        taskListPanel.setBorder(new EmptyBorder(8, 10, 0, 10));
        taskListPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        taskListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Filters
        filterDropdown = makeNewDropdown(TaskDifficulty.values(), "filterListBy", filterCallbackDifficulty);
        JPanel filtersPanel = makeDropdownPanel(filterDropdown, "Difficulty Filter");
        filtersPanel.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

        filterDropdown2 = makeNewDropdown(OtherFilter.values(), "filterListBy2", filterCallbackOther);
        JPanel filtersPanel2 = makeDropdownPanel(filterDropdown2, "Planning Filter");
        filtersPanel2.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

        filterDropdown4 = makeNewDropdown(FilterRequirements.values(), "filterListBy4", filterCallbackRequirements);
        JPanel filtersPanel4 = makeDropdownPanel(filterDropdown4, "Skill Req Filter");
        filtersPanel4.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

        filterDropdown3 = makeNewDropdown(TaskSortMethod.values(), "filterListBy3", filterCallbackSort);
        JPanel filtersPanel3 = makeDropdownPanel(filterDropdown3, "Task Sort Method");
        filtersPanel3.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

        allDropdownSections.setBorder(new EmptyBorder(0, 0, 10, 0));
        allDropdownSections.setLayout(new BorderLayout(0, 0));

        allDropdownSections2.setBorder(new EmptyBorder(0, 0, 10, 0));
        allDropdownSections2.setLayout(new BorderLayout(0, 0));

        allDropdownSections.add(filtersPanel, BorderLayout.NORTH);
        allDropdownSections.add(filtersPanel2, BorderLayout.SOUTH);
        allDropdownSections2.add(filtersPanel4, BorderLayout.NORTH);
        allDropdownSections2.add(filtersPanel3, BorderLayout.SOUTH);

        searchTasksPanel.add(allDropdownSections, BorderLayout.NORTH);
        searchTasksPanel.add(allDropdownSections2, BorderLayout.CENTER);

        // Wrapper
        taskListWrapper.setLayout(new BorderLayout());
        taskListWrapper.add(taskListPanel, BorderLayout.NORTH);

        scrollableContainer = new JScrollPane(taskListWrapper);
        scrollableContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


        JPanel OptionsPanel = new JPanel();
        OptionsPanel.setLayout(new BorderLayout());

        JPanel introOptionsPanel = new JPanel();
        introOptionsPanel.setLayout(new BorderLayout());
        introOptionsPanel.add(titlePanel, BorderLayout.NORTH);
        //introOptionsPanel.add(reloadActionsPanel, BorderLayout.CENTER);
        introOptionsPanel.add(recalculatePanel, BorderLayout.SOUTH);

        OptionsPanel.add(introOptionsPanel, BorderLayout.NORTH);

        JPanel autoRecalculatePanel = CreateClickableOption(autostartButton, "Enable Auto Refresh -> ", plugin.enableAutoRecalculate, Color.WHITE, Color.DARK_GRAY, autoRecalculateCallback);
        OptionsPanel.add(autoRecalculatePanel, BorderLayout.SOUTH);

        regionsUnlockedListPanel.setBorder(new EmptyBorder(8, 10, 0, 10));
        regionsUnlockedListPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        regionsUnlockedListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel RegionsUnlockedtitle = new JLabel();
        RegionsUnlockedtitle.setText("- Regions Unlocked -");
        RegionsUnlockedtitle.setForeground(Color.WHITE);

        regionsUnlockedListPanel.add(RegionsUnlockedtitle);

        JPanel misthalinPanel = CreateClickableOption(misthalinUnlockedButton, "Misthalin Unlocked ", config.MisthalinUnlocked(), RegionType.GetRegionColor(config, RegionType.MISTHALIN, false), Color.WHITE, misthalinUnlockedCallback);
        regionsUnlockedListPanel.add(misthalinPanel);

        JPanel karamjaPanel = CreateClickableOption(karamjaUnlockedButton, "Karamja Unlocked ", config.KaramjaUnlocked(), RegionType.GetRegionColor(config, RegionType.KARAMJA, false), Color.WHITE,karamjaUnlockedCallback);
        regionsUnlockedListPanel.add(karamjaPanel);

        JPanel kandarinPanel = CreateClickableOption(kandarinUnlockedButton, "Kandarin Unlocked ", config.KandarinUnlocked(), RegionType.GetRegionColor(config, RegionType.KANDARIN, false), Color.WHITE,kandarinUnlockedCallback);
        regionsUnlockedListPanel.add(kandarinPanel);

        JPanel asgarniaPanel = CreateClickableOption(asgarniaUnlockedButton, "Asgarnia Unlocked ", config.AsgarniaUnlocked(), RegionType.GetRegionColor(config, RegionType.ASGARNIA, false), Color.WHITE,asgarniaUnlockedCallback);
        regionsUnlockedListPanel.add(asgarniaPanel);

        JPanel fremennikPanel = CreateClickableOption(fremennikUnlockedButton, "Fremennik Unlocked ", config.FremennikUnlocked(), RegionType.GetRegionColor(config, RegionType.FREMENNIK, false), Color.WHITE,fremennikUnlockedCallback);
        regionsUnlockedListPanel.add(fremennikPanel);

        JPanel kourendPanel = CreateClickableOption(kourendUnlockedButton, "Kourend Unlocked ", config.KourendUnlocked(), RegionType.GetRegionColor(config, RegionType.KOUREND, false), Color.WHITE,kourendUnlockedCallback);
        regionsUnlockedListPanel.add(kourendPanel);

        JPanel wildernessPanel = CreateClickableOption(wildernessUnlockedButton, "Wilderness Unlocked ", config.WildernessUnlocked(), RegionType.GetRegionColor(config, RegionType.WILDERNESS, false), Color.WHITE,wildernessUnlockedCallback);
        regionsUnlockedListPanel.add(wildernessPanel);

        JPanel morytaniaPanel = CreateClickableOption(morytaniaUnlockedButton, "Morytania Unlocked ", config.MorytaniaUnlocked(), RegionType.GetRegionColor(config, RegionType.MORYTANIA, false), Color.WHITE,morytaniaUnlockedCallback);
        regionsUnlockedListPanel.add(morytaniaPanel);

        JPanel tirannwnPanel = CreateClickableOption(tirannwnUnlockedButton, "Tirannwn Unlocked ", config.TirannwnUnlocked(), RegionType.GetRegionColor(config, RegionType.TIRANNWN, false), Color.WHITE,tirannwnUnlockedCallback);
        regionsUnlockedListPanel.add(tirannwnPanel);

        JPanel desertPanel = CreateClickableOption(desertUnlockedButton, "Desert Unlocked ", config.DesertUnlocked(), RegionType.GetRegionColor(config, RegionType.DESERT, false), Color.WHITE,desertUnlockedCallback);
        regionsUnlockedListPanel.add(desertPanel);

        regionUnlockedScrollableContainer = new JScrollPane(regionsUnlockedListPanel);
        regionUnlockedScrollableContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel introDetailsPanel = new JPanel();
        introDetailsPanel.setLayout(new BorderLayout());
        introDetailsPanel.add(OptionsPanel, BorderLayout.NORTH);
        introDetailsPanel.add(regionUnlockedScrollableContainer, BorderLayout.CENTER);
        introDetailsPanel.add(searchTasksPanel, BorderLayout.SOUTH);

        add(introDetailsPanel, BorderLayout.NORTH);
        add(scrollableContainer, BorderLayout.CENTER);

        /* Layout */

        taskOverviewWrapper.setLayout(new BorderLayout());
    }

    private JPanel makeDropdownPanel(JComboBox dropdown, String name)
    {
        // Filters
        JLabel filterName = new JLabel(name);
        filterName.setForeground(Color.WHITE);

        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BorderLayout());
        filtersPanel.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
        filtersPanel.add(filterName, BorderLayout.CENTER);
        filtersPanel.add(dropdown, BorderLayout.EAST);

        return filtersPanel;
    }

    private JComboBox<Enum> makeNewDropdown(Enum[] values, String key, ItemListener callback)
    {
        JComboBox<Enum> dropdown = new JComboBox<>(values);
        dropdown.setFocusable(false);
        dropdown.setForeground(Color.WHITE);
        dropdown.setRenderer(new DropdownRenderer());
        dropdown.addItemListener(callback);

        return dropdown;
    }

    ArrayList<TaskSelectPanel> FilteredTaskListPanels = new ArrayList<>();

    public void CacheFilteredTaskList()
    {
        FilteredTaskListPanels.clear();

        TaskDifficulty DiffFilter = config.FilteredDifficulty();
        OtherFilter OthFilter = config.FilteredOther();
        FilterRequirements ReqFilter = config.FilteredRequirements();

        Integer MaxSortPriority = config.UserData.FindSortPriorityMaxOfIndices(5);

        // Go through all of our tasks and filter out anything that doesn't belong
        for (SortedTask SortedTaskIter : config.TaskData.SortedLeaguesTaskList)
        {
            TaskData data = plugin.GetTaskData(SortedTaskIter.TaskGUID, SortedTaskIter.bIsCustomTask);

            // Skip due to some filter
            boolean bIsPartOfPlan = config.UserData.PlannedTasks.containsKey(SortedTaskIter.TaskGUID);
            if (!bIsPartOfPlan && DiffFilter != TaskDifficulty.NONE && DiffFilter != data.Difficulty)
            {
                continue;
            }

            int ReqDifferent = data.CalculateNeededRequirementsForTask(plugin.client);
            if (ReqFilter == FilterRequirements.MEETS_REQ)
            {
                if (ReqDifferent != 0)
                {
                    continue;
                }
            }
            else if (ReqFilter == FilterRequirements.NEAR_REQ)
            {
                // 10 levels away
                if (ReqDifferent > 10)
                {
                    continue;
                }
            }


            if (OthFilter == OtherFilter.NO_PLAN)
            {
                if (bIsPartOfPlan)
                {
                    continue;
                }
            }

            if (OthFilter == OtherFilter.NEXT_5_TASKS)
            {
                if (bIsPartOfPlan)
                {
                    if (config.UserData.PlannedTasks.get(SortedTaskIter.TaskGUID).SortPriority >= MaxSortPriority)
                    {
                        continue;
                    }
                }
            }

            if (OthFilter == OtherFilter.ONLY_PLAN)
            {
                if (!bIsPartOfPlan)
                {
                    continue;
                }
            }

            boolean bSkipTask = false;
            for (RegionType ReqRegion : data.Regions)
            {
                if (!RegionType.GetRegionUnlocked(config, ReqRegion))
                {
                    bSkipTask = true;
                    break;
                }
            }

            // Go through all the task locations
            int PointsInRegions = 0;
            for (WorldPoint TaskWorldPoint : data.Locations)
            {
                if (config.RegionData.IsTileInUnlockedRegion(config, TaskWorldPoint))
                {
                    ++PointsInRegions;
                }
            }

            // Go through all the task overworld location
            for (WorldPoint TaskOverworldWorldPoint : data.OverworldLocations)
            {
                if (config.RegionData.IsTileInUnlockedRegion(config, TaskOverworldWorldPoint))
                {
                    ++PointsInRegions;
                }
            }

            if (!data.Locations.isEmpty() && PointsInRegions == 0)
            {
                bSkipTask = true;
            }

            if (bSkipTask)
            {
                continue;
            }

            FilteredTaskListPanels.add(new TaskSelectPanel(plugin, data));
        }
    }


    public void ApplyFilteredTaskListToPanel()
    {
        taskListPanel.removeAll();
        FilteredTaskListPanels.forEach(taskListPanel::add);
    }

    public ExecutorService pathfindingExecutor = Executors.newSingleThreadExecutor();
    public Future<?> pathfinderFuture;
    public Object pathfinderMutex = new Object();

    int CurrentPathfinderIndex = 0;

    @Getter
    public ArrayList<Pathfinder> pathfinderArray = new ArrayList<>();

    public int AddTaskListToSortedLeaguesTasks(WorldPoint PlayerLocation, HashMap<UUID, TaskData> TaskCache)
    {
        TaskSortMethod SortConfig = config.TaskSort();
        int NeededPathFinderSize = 0;

        for (HashMap.Entry<UUID, TaskData> entry : TaskCache.entrySet())
        {
            // Calculate our sort value
            int SortValue = 0;

            // Hidden values bumped to the end
            boolean bIsHidden = plugin.config.UserData.HiddenTasks.contains(entry.getKey());
            if (bIsHidden)
            {
                SortValue += 100000;
            }

            // completed go to the end
            boolean bIsCompleted = plugin.config.UserData.CompletedTasks.contains(entry.getKey());
            if (bIsCompleted)
            {
                SortValue += 100000;
            }

            // Always put things on the plan first
            if (config.UserData.PlannedTasks.containsKey(entry.getKey()))
            {
                SortValue += config.UserData.PlannedTasks.get(entry.getKey()).SortPriority;
                ++NeededPathFinderSize;
            }
            else
            {
                SortValue += 100000;
                if (SortConfig == TaskSortMethod.DISTANCE && PlayerLocation != null)
                {
                    float ClosestDistance = 90000;
                    for (WorldPoint NextLocation : entry.getValue().Locations)
                    {
                        float NextDistance = PlayerLocation.distanceTo(NextLocation);
                        if (ClosestDistance > NextDistance)
                        {
                            ClosestDistance = NextDistance;
                        }
                    }
                    SortValue += (int) ClosestDistance;
                }

                // Fallback with difficulty
                {
                    if (entry.getValue().Difficulty == TaskDifficulty.EASY)
                    {
                        SortValue += 100;
                    }
                    else if (entry.getValue().Difficulty == TaskDifficulty.MEDIUM)
                    {
                        SortValue += 200;
                    }
                    else if (entry.getValue().Difficulty == TaskDifficulty.HARD)
                    {
                        SortValue += 300;
                    }
                    else if (entry.getValue().Difficulty == TaskDifficulty.ELITE)
                    {
                        SortValue += 400;
                    }
                    else if (entry.getValue().Difficulty == TaskDifficulty.MASTER)
                    {
                        SortValue += 500;
                    }
                    else if (entry.getValue().Difficulty == TaskDifficulty.CUSTOM)
                    {
                        SortValue += 600;
                    }
                }

                // Region tie breakers
                {
                    if (!entry.getValue().Regions.isEmpty())
                    {
                        RegionType FirstRegion = entry.getValue().Regions.get(0);
                        SortValue += RegionType.GetRegionValue(FirstRegion);
                    }
                }
            }

            config.TaskData.SortedLeaguesTaskList.add(new SortedTask(entry.getKey(), SortValue, entry.getValue().bIsCustomTask));
        }
        return NeededPathFinderSize;
    }

    void CacheSortedLeaguesTasks(WorldPoint PlayerLocation)
    {
        config.TaskData.SortedLeaguesTaskList.clear();

        int NeededPathFinderSize = 0;
        NeededPathFinderSize += AddTaskListToSortedLeaguesTasks(PlayerLocation, config.TaskData.LeaguesTaskList);
        NeededPathFinderSize += AddTaskListToSortedLeaguesTasks(PlayerLocation, config.UserData.CustomTasks);

        config.TaskData.SortedLeaguesTaskList.sort(new Comparator<SortedTask>() {
            @Override
            public int compare(SortedTask o1, SortedTask o2)
            {
                return (o1.SortPriority.compareTo(o2.SortPriority));
            }
        });

        // Restart pathfinding settings
        ResetPathfindingSettings(NeededPathFinderSize);

        // Start a pathfinding task for each planned task
        Player player = plugin.client.getLocalPlayer();
        WorldPoint PreviousPointPosition = null;
        if (player != null)
        {
            PreviousPointPosition = player.getWorldLocation();
        }

        for (SortedTask sortTask : config.TaskData.SortedLeaguesTaskList)
        {
            // We are done with queueing our tasks
            if (!config.UserData.PlannedTasks.containsKey(sortTask.TaskGUID))
            {
                break;
            }

            float ClosestDistance = 9000000.0f;
            WorldPoint ClosestWorldPoint = null;
            TaskData data = plugin.GetTaskData(sortTask.TaskGUID, sortTask.bIsCustomTask
            );
            for (WorldPoint TaskPoint : data.Locations)
            {
                float NextDistance = 0.0f;
                if (PreviousPointPosition != null)
                {
                    NextDistance = PreviousPointPosition.distanceTo(TaskPoint);;
                }

                if ( NextDistance < ClosestDistance)
                {
                    ClosestDistance = NextDistance;
                    ClosestWorldPoint = TaskPoint;
                }
            }
            PreviousPointPosition = ClosestWorldPoint;
        }
    }

    public boolean bPathfindingReset = false;
    private void ResetPathfindingSettings(int NeededPathFinderSize)
    {
        config.UserData.CacheSortedPlannedTasks();

        for (int i = 0; i < pathfinderArray.size(); ++i)
        {
            synchronized (pathfinderMutex)
            {
                if (pathfinderArray.get(i) != null)
                {
                    pathfinderArray.get(i).cancel();
                    pathfinderFuture.cancel(true);
                }
            }
        }
        pathfinderArray.clear();

        if (pathfindingExecutor == null)
        {
            ThreadFactory shortestPathNaming = new ThreadFactoryBuilder().setNameFormat("shortest-path-%d").build();
            pathfindingExecutor = Executors.newSingleThreadExecutor(shortestPathNaming);
        }

        CurrentPathfinderIndex = 0;
        bPathfindingReset = true;
    }

    public void refresh()
    {
        Player player = plugin.client.getLocalPlayer();
        WorldPoint playerLocation = null;
        if (player != null)
        {
            playerLocation = player.getWorldLocation();
        }
        CacheSortedLeaguesTasks(playerLocation);

        filterDropdown4.setSelectedItem(config.FilteredRequirements());
        filterDropdown3.setSelectedItem(config.TaskSort());
        filterDropdown2.setSelectedItem(config.FilteredOther());
        filterDropdown.setSelectedItem(config.FilteredDifficulty());

        CacheFilteredTaskList();
        ApplyFilteredTaskListToPanel();

        autostartButton.repaint();
        autostartButton.revalidate();

        if (FilteredTaskListPanels.isEmpty())
        {
            NoSearchPanel.setVisible(true);
            NoSearchPanel.removeAll();
            JLabel noMatch = new JLabel();
            noMatch.setForeground(Color.GRAY);
            noMatch.setText("<html><body style='text-align:left'>No tasks are available that match your current filters</body></html>");
            NoSearchPanel.add(noMatch);
        }
        else
        {
            NoSearchPanel.setVisible(false);
        }

        repaint();
        revalidate();
    }
}
