//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.controlling.ClipboardHandler;
import jd.event.ControlEvent;
import jd.gui.skins.simple.JDAction;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

public class JDTrayIcon extends PluginOptional implements WindowListener, MouseListener {
    public JDTrayIcon(PluginWrapper wrapper) {
        super(wrapper);
    }

    private class TrayInfo extends Thread {
        private Point p;

        public TrayInfo(Point p) {
            this.p = p;
        }

        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                interrupt();
            }

            if (popupMenu.isVisible() || !getPluginConfig().getBooleanProperty("Tooltipp", true)) return;

            toolLabel.setText(createHTMLInfoString());
            toolParent.pack();
            calcLocation(toolParent, p);
            toolParent.setVisible(true);
            toolParent.toFront();

            while (counter > 0) {

                toolLabel.setText(createHTMLInfoString());
                toolParent.pack();

                counter--;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hideTooltip();
        }
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    private JCheckBoxMenuItem clipboard;
    private int counter = 0;

    private JMenuItem exit;
    private TrayInfo trayInfo;
    private JPopupMenu popupMenu;
    private JCheckBoxMenuItem reconnect;
    private JMenuItem speed1;
    private JMenuItem speed2;
    private JMenuItem speed3;
    private JMenuItem speed4;
    private JMenuItem speed5;
    private JMenu speeds;
    private JMenuItem startstop;

    private JLabel toolLabel;
    private JWindow toolParent;
    private TrayIcon trayIcon;
    private JWindow trayParent;

    private JMenuItem update;

    private JFrame guiFrame;
    private boolean iconfied = false;
    private long lastDeIconifiedEvent = 0;

    public void actionPerformed(ActionEvent e) {
        SimpleGUI simplegui = (SimpleGUI) JDUtilities.getGUI();
        if (e.getSource() == exit) {
            JDUtilities.getController().exit();
        } else if (e.getSource() == startstop) {
            JDUtilities.getController().toggleStartStop();
        } else if (e.getSource() == clipboard) {
            simplegui.actionPerformed(new ActionEvent(this, JDAction.APP_CLIPBOARD, null));

        } else if (e.getSource() == update) {
            new WebUpdate().doWebupdate(true);
        } else if (e.getSource() == reconnect) {
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));
            JDUtilities.getConfiguration().save();
        } else if (e.getSource() == speed1) {
            int speed = getPluginConfig().getIntegerProperty("SPEED1", 100);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            // simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed2) {
            int speed = getPluginConfig().getIntegerProperty("SPEED2", 200);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            // simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed3) {
            int speed = getPluginConfig().getIntegerProperty("SPEED3", 300);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            // simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed4) {
            int speed = getPluginConfig().getIntegerProperty("SPEED4", 400);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            // simplegui.setSpeedStatusBar(speed);
        } else if (e.getSource() == speed5) {
            int speed = getPluginConfig().getIntegerProperty("SPEED5", 500);
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, speed);
            JDUtilities.getSubConfig("DOWNLOAD").save();
            // simplegui.setSpeedStatusBar(speed);
        }
    }

    // Compute the proper position for a popup
    private Point computeDisplayPoint(int x, int y, Dimension dim) {
        if (x - dim.width > 0) {
            x -= dim.width;
        }
        if (y - dim.height > 0) {
            y -= dim.height;
        }
        return new Point(x, y);
    }

    private JMenuItem createMenuItem(String name) {
        JMenuItem menuItem = new JMenuItem(name);
        menuItem.setIcon(null);
        menuItem.addActionListener(this);
        popupMenu.add(menuItem);
        return menuItem;
    }

    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    public String getHost() {
        return JDLocale.L("plugins.optional.trayIcon.name", "TrayIcon");
    }

    public String getRequirements() {
        return "JRE 1.6+";
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void hideTooltip() {
        toolParent.setVisible(false);
        counter = 0;
    }

    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() < 1.6) {
            logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
            return false;
        }
        if (!SystemTray.isSupported()) {
            logger.severe("Error initializing SystemTray: Tray isn't supported jet");
            return false;
        }
        try {
            JDUtilities.getController().addControlListener(this);
            if (SimpleGUI.CURRENTGUI != null && SimpleGUI.CURRENTGUI.getFrame() != null) {
                guiFrame = SimpleGUI.CURRENTGUI.getFrame();
                guiFrame.addWindowListener(this);
            }
            logger.info("Systemtray OK");
            initGUI();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JDTrayIcon Init complete");
            guiFrame = SimpleGUI.CURRENTGUI.getFrame();
            guiFrame.addWindowListener(this);
            return;
        }
        super.controlEvent(event);
    }

    private void initGUI() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED1", JDLocale.L("plugins.optional.trayIcon.speed1", "Speed 1:"), 1, 100000).setDefaultValue(100));
        cfg.setDefaultValue("100");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED2", JDLocale.L("plugins.optional.trayIcon.speed2", "Speed 2:"), 1, 100000).setDefaultValue(200));
        cfg.setDefaultValue("200");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED3", JDLocale.L("plugins.optional.trayIcon.speed3", "Speed 3:"), 1, 100000).setDefaultValue(300));
        cfg.setDefaultValue("300");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED4", JDLocale.L("plugins.optional.trayIcon.speed4", "Speed 4:"), 1, 100000).setDefaultValue(400));
        cfg.setDefaultValue("400");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "SPEED5", JDLocale.L("plugins.optional.trayIcon.speed5", "Speed 5:"), 1, 100000).setDefaultValue(500));
        cfg.setDefaultValue("500");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "Tooltipp", JDLocale.L("plugins.optional.trayIcon.tooltipp", "Tooltipp aktiv:")).setDefaultValue(true));
        cfg.setDefaultValue(true);

        popupMenu = new JPopupMenu();
        update = createMenuItem(JDLocale.L("plugins.optional.trayIcon.update", "Update"));
        createMenuItem(JDLocale.L("plugins.optional.trayIcon.configuration", "Configuration"));
        popupMenu.addSeparator();
        startstop = createMenuItem(JDLocale.L("plugins.optional.trayIcon.startorstop", "Start/Stop"));

        speeds = new JMenu(JDLocale.L("plugins.optional.trayIcon.setspeeds", "Speeds"));
        popupMenu.add(speeds);

        speed1 = new JMenuItem(getPluginConfig().getStringProperty("SPEED1", "100") + " kb/s");
        speed1.addActionListener(this);
        speed1.setIcon(null);
        speeds.add(speed1);

        speed2 = new JMenuItem(getPluginConfig().getStringProperty("SPEED2", "200") + " kb/s");
        speed2.addActionListener(this);
        speed2.setIcon(null);
        speeds.add(speed2);

        speed3 = new JMenuItem(getPluginConfig().getStringProperty("SPEED3", "300") + " kb/s");
        speed3.addActionListener(this);
        speed3.setIcon(null);
        speeds.add(speed3);

        speed4 = new JMenuItem(getPluginConfig().getStringProperty("SPEED4", "400") + " kb/s");
        speed4.addActionListener(this);
        speed4.setIcon(null);
        speeds.add(speed4);

        speed5 = new JMenuItem(getPluginConfig().getStringProperty("SPEED5", "500") + " kb/s");
        speed5.addActionListener(this);
        speed5.setIcon(null);
        speeds.add(speed5);

        popupMenu.addSeparator();

   

        clipboard = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.trayIcon.clipboard", "Clipboard"), false);
        popupMenu.add(clipboard);
        clipboard.addActionListener(this);

        reconnect = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.trayIcon.reconnect", "Reconnect"), false);
        popupMenu.add(reconnect);
        reconnect.addActionListener(this);

        popupMenu.add(reconnect);
        popupMenu.addSeparator();
        exit = createMenuItem(JDLocale.L("gui.btn_exit", "Exit"));

        trayIcon = new TrayIcon(JDImage.getImage(JDTheme.V("gui.images.jd_logo")));
        trayIcon.setImageAutoSize(true);

        trayParent = new JWindow();
        trayParent.setSize(0, 0);
        trayParent.setAlwaysOnTop(true);
        trayParent.setVisible(false);

        toolLabel = new JLabel("jDownloader");
        toolLabel.setVisible(true);
        toolLabel.setOpaque(true);
        toolLabel.setBackground(new Color(0xb9cee9));

        toolParent = new JWindow();
        toolParent.setAlwaysOnTop(true);
        toolParent.add(toolLabel);
        toolParent.pack();
        toolParent.setVisible(false);

        setTrayPopUp(popupMenu);

        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void onExit() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        JDUtilities.getController().removeControlListener(this);
        if (guiFrame != null) guiFrame.removeWindowListener(this);
    }

    private void setTrayPopUp(JPopupMenu trayMenu) {
        popupMenu = trayMenu;

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                trayParent.setVisible(false);
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        popupMenu.setVisible(true);
        popupMenu.setVisible(false);

        trayIcon.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {

                    if (toolParent.isVisible()) {
                        hideTooltip();
                    }

                    if (e.getClickCount() > 1) {
                        guiFrame.setVisible(!guiFrame.isVisible());
                        if (guiFrame.isVisible()) guiFrame.setExtendedState(Frame.NORMAL);
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    showPopup(e.getPoint());
                }
            }
        });

        trayIcon.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                if (popupMenu.isVisible()) return;
                if (counter > 0) {
                    counter = 2;
                    return;
                }
                counter = 2;

                trayInfo = new TrayInfo(e.getPoint());
                trayInfo.start();
            }
        });
    }

    private void showPopup(final Point p) {
        trayParent.setVisible(true);
        trayParent.toFront();
        hideTooltip();

        clipboard.setSelected(ClipboardHandler.getClipboard().isEnabled());
        reconnect.setSelected(!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false));

        speed1.setText(getPluginConfig().getStringProperty("SPEED1", "100") + " kb/s");
        speed2.setText(getPluginConfig().getStringProperty("SPEED2", "200") + " kb/s");
        speed3.setText(getPluginConfig().getStringProperty("SPEED3", "300") + " kb/s");
        speed4.setText(getPluginConfig().getStringProperty("SPEED4", "400") + " kb/s");
        speed5.setText(getPluginConfig().getStringProperty("SPEED5", "500") + " kb/s");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Point p2 = computeDisplayPoint(p.x, p.y, popupMenu.getPreferredSize());
                popupMenu.show(trayParent, p2.x - trayParent.getLocation().x, p2.y - trayParent.getLocation().y);
            };
        });
    }

    private String createHTMLInfoString() {
        StringBuilder creater = new StringBuilder();
        creater.append("<html><center><b>jDownloader</b></center><hr>");
        int downloads = JDUtilities.getController().getRunningDownloadNum();
        if (downloads == 0) {
            creater.append(JDLocale.L("plugins.optional.trayIcon.nodownload", "No Download in progress") + "<br>");
        } else {
            creater.append("<table>");
            creater.append("<tr><td><i>" + JDLocale.L("plugins.optional.trayIcon.downloads", "Downloads:") + "</i></td><td>" + downloads + "</td></tr>");
            creater.append("<tr><td><i>" + JDLocale.L("plugins.optional.trayIcon.speed", "Speed:") + "</i></td><td>" + JDUtilities.formatKbReadable(JDUtilities.getController().getSpeedMeter() / 1024) + "/s </td></tr>");
            creater.append("</table>");
        }
        creater.append("</html>");
        return creater.toString();
    }

    private void calcLocation(final JWindow window, final Point p) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;

                if (p.x <= limitX) {
                    if (p.y <= limitY) {
                        // top left
                        window.setLocation(p.x, p.y);
                    } else {
                        // bottom left
                        window.setLocation(p.x, p.y - window.getHeight());
                    }
                } else {
                    if (p.y <= limitY) {
                        // top right
                        window.setLocation(p.x - window.getWidth(), p.y);
                    } else {
                        // bottom right
                        window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                    }
                }

            }
        });
    }

    private void miniIt() {
        if (System.currentTimeMillis() > this.lastDeIconifiedEvent + 750) {
            this.lastDeIconifiedEvent = System.currentTimeMillis();
            if (guiFrame.isVisible()) {
                guiFrame.setVisible(false);
            } else {
                if (OSDetector.isGnome() && iconfied) {
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                    guiFrame.setState(JFrame.ICONIFIED);
                    guiFrame.setVisible(false);
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                } else {
                    guiFrame.setState(JFrame.NORMAL);
                    guiFrame.setVisible(true);
                }
                iconfied = false;
            }

        }

    }

    public void windowActivated(WindowEvent arg0) {
    }

    public void windowClosed(WindowEvent arg0) {
    }

    public void windowClosing(WindowEvent arg0) {
    }

    public void windowDeactivated(WindowEvent arg0) {
    }

    public void windowDeiconified(WindowEvent arg0) {
        windowIconified(arg0);
    }

    public void windowIconified(WindowEvent arg0) {
        miniIt();
    }

    public void windowOpened(WindowEvent arg0) {
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void mousePressed(MouseEvent arg0) {
        if (arg0.getSource() instanceof JDTrayIcon) {
            if (arg0.getClickCount() >= 2 && !SwingUtilities.isRightMouseButton(arg0)) {
                iconfied = !iconfied;
                miniIt();
            }
        }
    }

    public void mouseReleased(MouseEvent arg0) {
    }
}