package Posiedien_Leagues_Planner;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LeaguesPlannerPanel extends PluginPanel
{
    private final PosiedienLeaguesPlannerPlugin plugin;

    private JPanel TaskList = new JPanel();
    JButton autostartButton = new JButton();

    private static final ImageIcon START_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/start.png"));
    private static final ImageIcon UNCHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/unchecked.png"));
    private static final ImageIcon CHECKED_ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(PosiedienLeaguesPlannerPlugin.class, "/checked.png"));

    public LeaguesPlannerPanel(PosiedienLeaguesPlannerPlugin plugin)
    {
        super(false);

        this.plugin = plugin;

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
        pleaseLoginLabel.setText("<html><body style='text-align:left'>Please log in to use the leagues optimizer" +
                ".</body></html>");

        TaskList.setLayout(new BorderLayout());
        TaskList.setBorder(new EmptyBorder(5, 10, 5, 10));
        TaskList.add(pleaseLoginLabel);
        TaskList.setVisible(false);

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
            /*plugin.CurrentStepSelected = null;
            if (plugin.getSelectedQuest() != null)
            {
                plugin.getSelectedQuest().setCurrentStep(questHelperPlugin.OldStepSelected);
            }

            plugin.startUpQuest(null);
            plugin.recreateActions();
            plugin.loadQuestList = true;
            plugin.MarkUIAndActionRefresh(true, true);*/
        });
        reloadActionsPanel.add(reloadbutton, BorderLayout.EAST);

        JPanel recalculatePanel = new JPanel();
        recalculatePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        recalculatePanel.setLayout(new BorderLayout());

        JLabel RecaculateLabel = new JLabel();
        RecaculateLabel.setText("Recalculate Order -> ");
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
            /*plugin.loadQuestList = true;
            plugin.updateShortestPath();
            plugin.MarkUIAndActionRefresh(true, true);*/
        });
        recalculatePanel.add(startButton, BorderLayout.EAST);

        JPanel autoRecalculatePanel = new JPanel();
        autoRecalculatePanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        autoRecalculatePanel.setLayout(new BorderLayout());

        JLabel autoRecaculateLabel = new JLabel();
        autoRecaculateLabel.setText("Enable Auto Recalculate -> ");
        autoRecaculateLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        autoRecaculateLabel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        autoRecaculateLabel.setForeground(Color.WHITE);
        autoRecalculatePanel.add(autoRecaculateLabel, BorderLayout.WEST);

        if (plugin.enableAutoRecalculate)
        {
            autostartButton.setIcon(CHECKED_ICON);
        }
        else
        {
            autostartButton.setIcon(UNCHECKED_ICON);
        }

        autostartButton.setBorder(new EmptyBorder(5, 10, 5, 10));
        autostartButton.setLayout(new BorderLayout(0, BORDER_OFFSET));
        autostartButton.addActionListener(e ->
        {
            /*
            plugin.enableAutoRecalculate = !questHelperPlugin.enableAutoRecalculate;
            plugin.MarkUIAndActionRefresh(false, true);
            plugin.refreshPanels = true;
             */

            if (plugin.enableAutoRecalculate)
            {
                autostartButton.setIcon(CHECKED_ICON);
            }
            else
            {
                autostartButton.setIcon(UNCHECKED_ICON);
            }

        });
        autoRecalculatePanel.add(autostartButton, BorderLayout.EAST);

    }
}
