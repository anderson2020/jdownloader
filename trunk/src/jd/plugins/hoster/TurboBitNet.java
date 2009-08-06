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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "turbobit.net" }, urls = { "http://[\\w\\.]*?(bluetooths.pp.ru|dz-files.ru|file.alexforum.ws|file.grad.by|file.krut-warez.ru|filebit.org|files.best-trainings.org.ua|files.wzor.ws|gdefile.ru|letitshare.ru|mnogofiles.com|share.uz|sibit.net|turbo-bit.ru|turbobit.net|turbobit.ru|upload.mskvn.by|vipbit.ru|files.prime-speed.ru|filestore.net.ru|turbobit.ru|upload.dwmedia.ru|upload.uz|xrfiles.ru)/[a-z0-9]+\\.html" }, flags = { 0 })
public class TurboBitNet extends PluginForHost {

    public TurboBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://turbobit.net/rules";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("<div class=\"code-404\">404</div>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infos = br.getRegex(Pattern.compile("<h1 class=\"download-file\">.*<span class='file-icon rar'>&nbsp;</span><b>(.*?)</b></h1>.*<div class=\"download-file\">.*<div><b>.*:</b>(.*?)</div></div>", Pattern.DOTALL));
        String fileName = infos.getMatch(0);
        //TODO: scheiss kyrillisch wiedermal ... geht nicht
        String fileSize = infos.getMatch(1).replace("??", "Mb").replace("??", "Kb");
        if (fileName == null || fileSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(fileName.trim());
        downloadLink.setDownloadSize(Regex.getSize(fileSize.trim()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String id = new Regex(downloadLink.getDownloadURL(), Pattern.compile(".*/(.*?)\\.html")).getMatch(0);

        br.getPage("http://turbobit.net/download/free/" + id);
        String waittime = br.getRegex("<span id='timeout'>(.*?)</span></h1>").getMatch(0);
        if(waittime != null) {
            sleep(Long.parseLong(waittime) * 1000, downloadLink);
        }
        String captchaUrl = br.getRegex("<img alt=\"Captcha\" src=\"(.*?)\" width=\"150\" height=\"50\" />").getMatch(0);

        Form form = br.getForm(0);

        for (int i = 1; i <= 3; i++) {
            String captchaCode = getCaptchaCode(captchaUrl, downloadLink);

            form.put("captcha_response", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("updateTime: function()")) break;
        }
        if (!br.containsHTML("updateTime: function()")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        sleep(61 * 1000, downloadLink);
        br.getPage("http://turbobit.net/download/timeout/" + id);

        String downloadUrl = br.getRegex("<a href='(.*?)'>").getMatch(0);

        dl = br.openDownload(downloadLink, downloadUrl, true, 0);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}