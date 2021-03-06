package org.jdownloader.extensions.omnibox.omni.gui;

import javax.swing.JPanel;

import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;

import org.jdownloader.extensions.omnibox.OmniboxExtension;

public class AwesomeCustomToolbarAction extends CustomToolbarAction {

    private final OmniboxExtension awesomebar;
    private final JPanel              awesomePanel;

    public AwesomeCustomToolbarAction(OmniboxExtension awesomebar) {
        super("addons.awesomebar");
        this.awesomebar = awesomebar;

        awesomePanel = this.awesomebar.getToolbarPanel();
    }

    private static final long serialVersionUID = 5484555948469924227L;

    /**
     * the addon has to get disabled, not the CustomToolbarAction
     */
    public boolean force() {
        return true;
    }

    @Override
    public void addTo(Object toolBar) {
        // Toolbaractions might be used by other components, too
        // it's up to the custom action to implement them
        if (toolBar instanceof ToolBar) {
            ToolBar tb = (ToolBar) toolBar;
            tb.add(this.awesomebar.getToolbarPanel(), "");
        }
    }

    @Override
    public void initDefaults() {
    }

    public JPanel getAwesomePanel() {
        return awesomePanel;
    }

    @Override
    protected String createMnemonic() {
        return null;
    }

    @Override
    protected String createAccelerator() {
        return null;
    }

    @Override
    protected String createTooltip() {
        return null;
    }

}