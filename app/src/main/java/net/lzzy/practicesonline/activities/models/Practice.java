package net.lzzy.practicesonline.activities.models;

import net.lzzy.sqllib.Sqlitable;

import java.util.Date;

/**
 * Created by lzzy_gxy on 2019/4/16.
 * Description:
 */
public class Practice extends BaseEntity implements Sqlitable {
    public static final String COL_NAME = "name";
    public static final String COL_OUTLINES = "outlines";
    public static final String COL_API_ID = "apiId";
    private String name;

    private int questionCount;
    private Date downloadDate;
    private String outlines;
    private boolean isDownloaded;
    private  int apild;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public Date getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(Date downloadDate) {
        this.downloadDate = downloadDate;
    }

    public String getOutlines() {
        return outlines;
    }

    public void setOutlines(String outlines) {
        this.outlines = outlines;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public int getApild() {
        return apild;
    }

    public void setApild(int apild) {
        this.apild = apild;
    }



    @Override
    public boolean needUpdate() {
        return false;
    }

    public int getApiId() {
        return apild;
    }
}
