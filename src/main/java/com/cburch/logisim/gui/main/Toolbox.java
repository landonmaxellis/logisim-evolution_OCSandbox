/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class Toolbox extends JPanel {
  private static final long serialVersionUID = 1L;

  private static final String NORMAL_VIEW = "normal";
  private static final String SEARCH_VIEW = "search";

  private final Project proj;
  private final ProjectExplorer toolbox;

  private final JTextField searchField;
  private final CardLayout cardLayout;
  private final JPanel contentPanel;

  private final DefaultListModel<Tool> searchResultsModel;
  private final JList<Tool> searchResultsList;
  private final JLabel noResultsLabel;

  Toolbox(Project proj, Frame frame, MenuListener menu) {
    super(new BorderLayout());
    this.proj = proj;

    final var toolbarModel = new ToolboxToolbarModel(frame, menu);
    final var toolbar = new Toolbar(toolbarModel);

// NEW: top container
    final var topPanel = new JPanel(new BorderLayout());

// toolbar at top
    topPanel.add(toolbar, BorderLayout.NORTH);

// search bar directly under it
    final var searchPanel = new JPanel(new BorderLayout());
    searchField = new JTextField();
    searchField.setToolTipText("Search components");
    searchPanel.add(searchField, BorderLayout.CENTER);

    topPanel.add(searchPanel, BorderLayout.SOUTH);

// add BOTH to top
    add(topPanel, BorderLayout.NORTH);

    toolbox = new ProjectExplorer(proj, false);
    toolbox.setListener(new ToolboxManip(proj, toolbox));
    final var normalScrollPane = new JScrollPane(toolbox);

    searchResultsModel = new DefaultListModel<>();
    searchResultsList = new JList<>(searchResultsModel);
    searchResultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    searchResultsList.setCellRenderer(new ToolListRenderer());

    noResultsLabel = new JLabel("No matching components", JLabel.CENTER);

    final var searchResultsPanel = new JPanel(new BorderLayout());
    searchResultsPanel.add(new JScrollPane(searchResultsList), BorderLayout.CENTER);
    searchResultsPanel.add(noResultsLabel, BorderLayout.SOUTH);

    cardLayout = new CardLayout();
    contentPanel = new JPanel(cardLayout);
    contentPanel.add(normalScrollPane, NORMAL_VIEW);
    contentPanel.add(searchResultsPanel, SEARCH_VIEW);
    add(contentPanel, BorderLayout.CENTER);

    installSearchBehavior();

    toolbarModel.menuEnableChanged(menu);
    cardLayout.show(contentPanel, NORMAL_VIEW);
    noResultsLabel.setVisible(false);
  }

  private void installSearchBehavior() {
    searchField.getDocument().addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(DocumentEvent e) {
                updateSearchResults();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                updateSearchResults();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                updateSearchResults();
              }
            });

    searchResultsList.addListSelectionListener(
            e -> {
              if (!e.getValueIsAdjusting()) {
                final var selectedTool = searchResultsList.getSelectedValue();
                if (selectedTool != null) {
                  proj.setTool(selectedTool);
                }
              }
            });

    searchResultsList.addMouseListener(
            new MouseAdapter() {
              @Override
              public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                  final var selectedTool = searchResultsList.getSelectedValue();
                  if (selectedTool != null) {
                    proj.setTool(selectedTool);
                  }
                }
              }
            });
  }

  private void updateSearchResults() {
    final var query = searchField.getText();

    if (query == null || query.trim().isEmpty()) {
      searchResultsModel.clear();
      noResultsLabel.setVisible(false);
      cardLayout.show(contentPanel, NORMAL_VIEW);
      return;
    }

    final var allTools = getAllTools();
    final var matches = new ArrayList<Tool>();

    for (final var tool : allTools) {
      if (tool != null && StringUtil.matchesSearchQuery(tool.getDisplayName(), query)) {
        matches.add(tool);
      }
    }

    searchResultsModel.clear();
    for (final var tool : matches) {
      searchResultsModel.addElement(tool);
    }

    noResultsLabel.setVisible(matches.isEmpty());
    cardLayout.show(contentPanel, SEARCH_VIEW);
  }

  private List<Tool> getAllTools() {
    final Set<Tool> allTools = new LinkedHashSet<>();
    collectTools(proj.getLogisimFile(), allTools);
    return new ArrayList<>(allTools);
  }

  private void collectTools(Library library, Set<Tool> tools) {
    if (library == null) {
      return;
    }

    for (final var tool : library.getTools()) {
      if (tool != null) {
        tools.add(tool);
      }
    }

    for (final var childLibrary : library.getLibraries()) {
      collectTools(childLibrary, tools);
    }
  }

  void setHaloedTool(Tool value) {
    toolbox.setHaloedTool(value);
  }

  public void updateStructure() {
    toolbox.updateStructure();
    updateSearchResults();
  }

  private static class ToolListRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (value instanceof Tool tool) {
        setText(tool.getDisplayName());
      }

      return this;
    }
  }
}