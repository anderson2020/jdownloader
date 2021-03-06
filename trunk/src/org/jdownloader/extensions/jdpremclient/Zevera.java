package org.jdownloader.extensions.jdpremclient;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.AccountController;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.TransferStatus;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.formatter.TimeFormatter;

public class Zevera extends PluginForHost implements JDPremInterface {

    private boolean                  proxyused    = false;
    private String                   infostring   = null;
    private PluginForHost            plugin       = null;
    private static boolean           enabled      = false;
    private static ArrayList<String> premiumHosts = new ArrayList<String>();
    private static final Object      LOCK         = new Object();

    public Zevera(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://zevera.com");
        infostring = "zevera.com @ " + wrapper.getLazy().getDisplayName();
    }

    @Override
    public String getAGBLink() {
        if (plugin == null) return "http://zevera.com/TermsOfUse.aspx";
        return plugin.getAGBLink();
    }

    @Override
    public long getVersion() {
        if (plugin == null) return Formatter.getRevision("$Revision: 13504 $");
        return plugin.getVersion();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (plugin != null) {
            plugin.actionPerformed(e);
        } else {
            super.actionPerformed(e);
        }
    }

    @Override
    public String getHost() {
        if (plugin == null) return "zevera.com";
        return plugin.getHost();
    }

    @Override
    public ConfigContainer getConfig() {
        if (plugin == null) return super.getConfig();
        return plugin.getConfig();
    }

    @Override
    public String getBuyPremiumUrl() {
        if (plugin == null) return "http://www.zevera.com/Prices.aspx";
        return plugin.getBuyPremiumUrl();
    }

    @Override
    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (plugin == null) {
            super.handle(downloadLink, account);
            return;
        }
        proxyused = false;
        /* copied from PluginForHost */
        final TransferStatus transferStatus = downloadLink.getTransferStatus();
        transferStatus.usePremium(false);
        transferStatus.setResumeSupport(false);
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        /* try zevera first */
        if (account == null) {
            if (handleZevera(downloadLink)) return;
        } else if (!PremiumCompoundExtension.preferLocalAccounts()) {
            if (handleZevera(downloadLink)) return;
        }
        if (proxyused = true) {
            /* failed, now try normal */
            proxyused = false;

        }
        plugin.handle(downloadLink, account);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        br.reset();
        plugin.handleFree(link);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (plugin == null) return;
        proxyused = false;
        br.reset();
        plugin.handlePremium(downloadLink, account);
    }

    @Override
    public void setBrowser(Browser br) {
        this.br = br;
        if (plugin != null) plugin.setBrowser(br);
    }

    @Override
    public Browser getBrowser() {
        if (plugin != null) return plugin.getBrowser();
        return this.br;
    }

    @Override
    public void clean() {
        super.clean();
        if (plugin != null) plugin.clean();
    }

    private boolean handleZevera(DownloadLink link) throws Exception {
        Account acc = null;
        synchronized (LOCK) {
            /* jdpremium enabled */
            if (!PremiumCompoundExtension.isStaticEnabled() || !enabled) return false;
            /* premium available for this host */
            if (!premiumHosts.contains(link.getHost())) return false;
            acc = AccountController.getInstance().getValidAccount("zevera.com");
            /* enabled account found? */
            if (acc == null || !acc.isEnabled()) return false;
        }
        proxyused = true;
        requestFileInformation(link);
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String user = Encoding.urlEncode(acc.getUser());
        String pw = Encoding.urlEncode(acc.getPass());
        br.setConnectTimeout(90 * 1000);
        br.setReadTimeout(90 * 1000);
        br.setDebug(true);
        dl = null;
        // String url = Encoding.urlEncode(link.getDownloadURL());
        String url = link.getDownloadURL();
        String FileID = null;
        String dlUrl = null;
        showMessage(link, "Phase 1/3: Ask to Download");
        br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=generatedownload&login=" + user + "&pass=" + pw + "&olink=" + url);

        // br.followConnection();
        String infos[] = br.getRegex("(.*?)(,|$)").getColumn(0);
        if (br.containsHTML("File Added Successfully!")) {
            logger.severe("1: File added successfully " + br.toString());
            // File Added Successfully!,FileID:1198409,FileName:,
            // FileNameOnServer:C14316_2011618113759434_6674.zip,
            // StorageServerURL:Http://rapidus.000.gr,FileSizeInBytes:0,Status:Waiting,
            // TotalBytesToReceive:0P
            FileID = new Regex(infos[1], "FileID:(.+)").getMatch(0);
            logger.severe("2: FileID= " + FileID);
            while (true) {
                br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=fileinfo&login=" + user + "&pass=" + pw + "&FileID=" + FileID);
                if (br.containsHTML("Status:Downloaded")) {
                    dlUrl = br.getRegex("DownloadURL:(.*?)(,|$)").getMatch(0);
                    showMessage(link, "Phase 2/3: Check Download");
                    break;
                }
                this.sleep(15 * 1000l, link, "Waiting for download to finish on Zevera");
            }
        } else {
            if (br.containsHTML("Status:Downloaded")) {
                logger.severe("3: Status:Downloaded");
                logger.severe("3: Infos[5]=" + infos[5]);
                dlUrl = br.getRegex("DownloadURL:(.*?)(,|$)").getMatch(0);
                showMessage(link, "Phase 2/3: Check Download");
            } else {
                logger.severe("5:ERROR_FILE_NOT_FOUND");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Error adding file");
            }
        }
        if (dlUrl != null) {
            showMessage(link, "Phase 3/3: Download:" + dlUrl);
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlUrl, true, 1);
            if (dl.getConnection().isContentDisposition()) {
                long filesize = dl.getConnection().getLongContentLength();
                if (filesize == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
                /* contentdisposition, lets download it */
                dl.startDownload();
                return true;
            } else {
                /*
                 * download is not contentdisposition, so remove this host from
                 * premiumHosts list
                 */
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (plugin == null) return AvailableStatus.UNCHECKABLE;
        return plugin.requestFileInformation(parameter);
    }

    @Override
    public void reset() {
        if (plugin != null) {
            plugin.reset();
        }
    }

    @Override
    public void init() {
        if (plugin != null) {
            plugin.init();
        } else {
            super.init();
        }
    }

    private void resetAvailablePremium() {
        synchronized (LOCK) {
            premiumHosts.clear();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        if (plugin == null) {
            /* this plugin do not have fetchAccountInfo */
            return null;
        } else if (plugin.getHost().equalsIgnoreCase("zevera.com")) {
            String restartReq = enabled == false ? "(Restart required) " : "";
            AccountInfo ac = new AccountInfo();
            final boolean follow = br.isFollowingRedirects();
            br.setFollowRedirects(true);
            br.setConnectTimeout(60 * 1000);
            br.setReadTimeout(60 * 1000);
            br.setDebug(true);
            String username = Encoding.urlEncode(account.getUser());
            String pass = Encoding.urlEncode(account.getPass());
            String loginPage = null;
            long trafficLeft = 0;
            String ses = null;
            String hosts = null;
            String EndSubscriptionDate = null;
            try {
                loginPage = br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=accountinfo&login=" + username + "&pass=" + pass);
                ses = new Regex(loginPage, "AvailableExtraTraffic:(\\d+)").getMatch(0);
                if (ses != null) {
                    trafficLeft = Long.parseLong(ses);
                } else {
                    ses = new Regex(loginPage, "AvailableTodayTraffic:(\\d+)").getMatch(0);
                    if (ses != null) {
                        trafficLeft = Long.parseLong(ses);
                    }
                }
                EndSubscriptionDate = new Regex(loginPage, "EndSubscriptionDate:(.*?),").getMatch(0);
                hosts = br.getPage("http://www.zevera.com/jDownloader.ashx?cmd=gethosters&login=" + username + "&pass=" + pass);
            } catch (Exception e) {
                account.setTempDisabled(true);
                account.setValid(true);
                resetAvailablePremium();
                ac.setStatus("Zevera Server Error, temp disabled" + restartReq);
                return ac;
            } finally {
                br.setFollowRedirects(follow);
            }
            if (loginPage == null || trafficLeft <= 0) {
                account.setValid(false);
                account.setTempDisabled(false);
                ac.setStatus("Account invalid");
                resetAvailablePremium();
            } else {
                if (EndSubscriptionDate != null) ac.setValidUntil(TimeFormatter.getMilliSeconds(EndSubscriptionDate, "yyyy/MM/dd HH:mm:ss", null));
                ac.setTrafficLeft(trafficLeft * 1024 * 1024);
                synchronized (LOCK) {
                    premiumHosts.clear();
                    if (!"0".equals(hosts.trim())) {
                        String hoster[] = new Regex(hosts, "(.*?)(,|$)").getColumn(0);
                        if (hoster != null) {
                            for (String host : hoster) {
                                if (host == null || host.length() == 0) continue;
                                premiumHosts.add(host.trim());
                            }
                        }
                    }
                }
                account.setValid(true);
                if (premiumHosts.size() == 0) {
                    ac.setStatus(restartReq + "Account valid: 0 Hosts via Zevera.com available");
                } else {
                    ac.setStatus(restartReq + "Account valid: " + premiumHosts.size() + " Hosts via Zevera.com available");
                }
            }
            return ac;
        }
        return plugin.fetchAccountInfo(account);
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (plugin != null) plugin.resetDownloadlink(link);
    }

    @Override
    public String getSessionInfo() {
        if (proxyused || plugin == null) return infostring;
        return plugin.getSessionInfo();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (plugin != null) plugin.correctDownloadLink(link);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        if (plugin != null) return plugin.getMaxSimultanFreeDownloadNum();
        return super.getMaxSimultanFreeDownloadNum();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        if (plugin != null) return plugin.getMaxSimultanPremiumDownloadNum();
        return super.getMaxSimultanPremiumDownloadNum();
    }

    @Override
    public int getMaxSimultanDownload(final Account account) {
        if (plugin != null) {
            if (PremiumCompoundExtension.preferLocalAccounts() && account != null) {
                /* user prefers usage of local account */
                return plugin.getMaxSimultanDownload(account);
            } else if (PremiumCompoundExtension.isStaticEnabled() && enabled) {
                /* Zevera */
                synchronized (LOCK) {
                    if (premiumHosts.contains(plugin.getHost()) && AccountController.getInstance().getValidAccount("zevera.com") != null) return Integer.MAX_VALUE;
                }
            }
            return plugin.getMaxSimultanDownload(account);
        }
        return 0;
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (plugin == null) return false;
        return plugin.checkLinks(urls);
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        if (proxyused || plugin == null) return "";
        return plugin.getFileInformationString(downloadLink);
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        if (plugin == null) return super.createMenuitems();
        return plugin.createMenuitems();
    }

    @Override
    public ArrayList<Account> getPremiumAccounts() {
        if (plugin != null) return plugin.getPremiumAccounts();
        return super.getPremiumAccounts();
    }

    public void setReplacedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public void enablePlugin() {
        enabled = true;
    }

    @Override
    public int getTimegapBetweenConnections() {
        if (plugin != null) return plugin.getTimegapBetweenConnections();
        return super.getTimegapBetweenConnections();
    }

    @Override
    public boolean rewriteHost(DownloadLink link) {
        if (plugin != null) return plugin.rewriteHost(link);
        return false;
    }

    @Override
    public void setDownloadInterface(DownloadInterface dl) {
        this.dl = dl;
        if (plugin != null) plugin.setDownloadInterface(dl);
    }

    @Override
    public String getCustomFavIconURL() {
        if (proxyused) return "zevera.com";
        if (plugin != null) return plugin.getCustomFavIconURL();
        return null;
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
        link.requestGuiUpdate();
    }

}
