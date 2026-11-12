package com.jht.chimera.service.tiers.helpers;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.download.DownloadManager;
import com.jht.chimera.lib.download.DownloadRequest;
import com.jht.chimera.lib.download.IDownloadStatus;
import com.jht.chimera.service.tiers.UpgradeSkin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadHelper implements IDownloadStatus
{

    private List<DownloadRecord> activeDownloads = new ArrayList<DownloadRecord>();
    private DownloadManager dm = new DownloadManager();
    private boolean allDownloadsAdded = false;
    private IAllDownloadsComplete allDownloadsComplete;


    public enum DOWNLOAD_TYPE
    {
        UPGRADE_BACKGROUND,
        UPGRADE_OK,
        UPGRADE_CANCEL,
        UPGRADE_UPGRADE,
        UPGRADE_OK_PRESSED,
        UPGRADE_CANCEL_PRESSED,
        UPGRADE_UPGRADE_PRESSED;
    }

    public interface IAllDownloadsComplete
    {
        public void onDownloadsComplete();
    }

    private class DownloadRecord
    {
        public DownloadRecord(DOWNLOAD_TYPE download, UpgradeSkin skin, DownloadRequest request, String md5)
        {
            this.download = download;
            this.skin = skin;
            this.request = request;
            this.md5 = md5;
        }
        private UpgradeSkin skin;
        private DOWNLOAD_TYPE download;
        private DownloadRequest request;
        private String md5;

        public UpgradeSkin skin() { return this.skin; }
        public DOWNLOAD_TYPE download() { return this.download; }
        public DownloadRequest request() { return this.request; }
        public String md5() { return this.md5; }
    }

    public DownloadHelper(IAllDownloadsComplete allDownloadsComplete)
    {
        this.allDownloadsComplete = allDownloadsComplete;
    }

    public void clearDownloads()
    {
        activeDownloads.clear();
        dm.clearDownloads();
        allDownloadsAdded = false;
    }

    public void download(DOWNLOAD_TYPE download, UpgradeSkin skin, String url, String md5)
    {
        File dir = new File("/sdcard/Download/upgrade_skins");
        if(!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "/sdcard/Download/upgrade_skins/" + download; //NON-NLS

        DownloadRequest request = new DownloadRequest(fileName, url, download.toString());
        dm.download(request, this);
        activeDownloads.add(new DownloadRecord(download, skin, request, md5));
    }

    @Override
    public void onComplete(DownloadRequest request, STATUS_CODE status,	int errorCode) {
        DownloadRecord record = null;
        for(DownloadRecord r : activeDownloads)
        {
            if(r.request().file.equals(request.file))
            {
                record = r;
                if(status == STATUS_CODE.SUCCESS)
                {
                    if(r.md5() == null || r.md5().equals("") || FileUtils.computeMd5(request.file).equals(r.md5()))
                    {
                        switch(r.download())
                        {
                            case UPGRADE_BACKGROUND:
                                //r.skin().setBackground(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setBackgroundPath(request.file);
                                break;
                            case UPGRADE_CANCEL:
                                //r.skin().setCancelButton(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setCancelButtonPath(request.file);
                                break;
                            case UPGRADE_OK:
                                //r.skin().setOkButton(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setOkButtonPath(request.file);
                                break;
                            case UPGRADE_UPGRADE:
                                //r.skin().setUpgradeButton(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setUpgradeButtonPath(request.file);
                                break;
                            case UPGRADE_CANCEL_PRESSED:
                                //r.skin().setCancelButtonPressed(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setCancelButtonPressedPath(request.file);
                                break;
                            case UPGRADE_OK_PRESSED:
                                //r.skin().setOkButtonPressed(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setOkButtonPressedPath(request.file);
                                break;
                            case UPGRADE_UPGRADE_PRESSED:
                                //r.skin().setUpgradeButtonPressed(MiscUtilHelper.decodeBackgroundImage(new File(request.file)));
                                r.skin().setUpgradeButtonPressedPath(request.file);
                                break;
                            default:
                                break;
                        }
                    }
                }
                break;
            }
        }
        if(record != null)
        {
            activeDownloads.remove(record);
        }
        checkDownloadsComplete();

    }

    public void doneAddingDownloads()
    {
        allDownloadsAdded = true;
        checkDownloadsComplete();
    }

    private void checkDownloadsComplete()
    {
        if(allDownloadsAdded && activeDownloads.size() == 0 && allDownloadsComplete != null)
        {
            allDownloadsComplete.onDownloadsComplete();
        }
    }


}
