package net.lzzy.practicesonline.activities.models;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.util.Pair;
import net.lzzy.practicesonline.activities.models.view.QuestionResult;
import net.lzzy.practicesonline.activities.models.view.WrongType;
import net.lzzy.practicesonline.activities.utils.AppUtils;
import net.lzzy.practicesonline.activities.utils.DateTimeUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * Created by lzzy_gxy on 2019/4/24.
 * Description:
 */
public class UserCookies {
    private static final String KEY_TIME = "keyTime";
    private static final int FLAG_COMMIT_N = 0;
    private static final int FLAG_COMMIT_Y = 1;
    private static final String ID_SPLITTER = "splitter";
    private SharedPreferences spPosition;
    private SharedPreferences spReadCount;
    private SharedPreferences spCommit;
    private SharedPreferences spTime;
    private static final UserCookies INSTANCE = new UserCookies();

    private SharedPreferences spChecked;

    private UserCookies() {
        spTime = AppUtils.getContext().getSharedPreferences("refresh_time", Context.MODE_PRIVATE);
        spCommit = AppUtils.getContext().getSharedPreferences("practice_commit", Context.MODE_PRIVATE);
        spPosition = AppUtils.getContext().getSharedPreferences("practice_question", Context.MODE_PRIVATE);
        spChecked = AppUtils.getContext().getSharedPreferences("practice_checked", Context.MODE_PRIVATE);
        spReadCount = AppUtils.getContext().getSharedPreferences("read_count", Context.MODE_PRIVATE);
    }

    public static UserCookies getInstance() {
        return INSTANCE;
    }

    public void updateLastRefreshTime() {
        String time = DateTimeUtils.DATE_TIME_FORMAT.format(new Date());
        spTime.edit().putString("keyTime", time).apply();
    }

    public String getLastRefreshTime() {
        return spTime.getString(KEY_TIME, "");
    }

    public void updateCurrentQuestion(String practiceId, int pos) {
        spPosition.edit().putInt(practiceId, pos).apply();
    }

    public int getCurrentQuestion(String questionId) {
        return spPosition.getInt(questionId, 0);
    }

    public void commitPractice(String practiceId) {
        spCommit.edit().putInt(practiceId, FLAG_COMMIT_Y).apply();
    }

    public int getReadCount(String questionId) {
        return spReadCount.getInt(questionId, 0);
    }


    public void updateReadCount(String questionId) {
        int count = getReadCount(questionId) + 1;
        spReadCount.edit().putInt(questionId, count).apply();
    }

    public boolean isPracticeCommitted(String practiceId) {
        int result = spCommit.getInt(practiceId, FLAG_COMMIT_N);
        return result == FLAG_COMMIT_Y;

    }

    public void changeOptionState(Option option, boolean isChecked, boolean isMulti) {
        String ids = spChecked.getString(option.getQuestionId().toString(), "");
        String id = option.getId().toString();
        if (isMulti) {
            if (isChecked && !ids.contains(id)) {
                ids = ids.concat(ID_SPLITTER).concat(id);
            } else if (!isChecked && ids.contains(id)) {
                ids = ids.replace(id, "");
            }
        } else {
            if (isChecked) {
                ids = id;
            }
        }
        spChecked.edit().putString(option.getQuestionId().toString(), trunkSplitter(ids)).apply();
    }

    private String trunkSplitter(String ids) {
        boolean isSplitterRepeat = true;
        String repeatSplitter = ID_SPLITTER.concat(ID_SPLITTER);
        while (isSplitterRepeat) {
            isSplitterRepeat = false;
            if (ids.contains(repeatSplitter)) {
                isSplitterRepeat = true;
                ids = ids.replace(repeatSplitter, ID_SPLITTER);
            }
        }
        if (ids.endsWith(ID_SPLITTER)) {
            ids = ids.substring(0, ids.length() - 1);
        }
        if (ids.startsWith(ID_SPLITTER)) {
            ids = ids.substring(1);
        }
        return ids;

    }

    public boolean isOptionSelected(Option option) {
        String ids = spChecked.getString(option.getQuestionId().toString(), "");
        return ids.contains(option.getId().toString());
    }


    /**
     * 对比文本文件保存的数据以此来对比答案是否正确
     **/
    public List<QuestionResult> getResultFromCookies(List<Question> questions) {
        List<QuestionResult> results=new ArrayList<>();
        for (Question question:questions){
            QuestionResult result=new QuestionResult();
            result.setQuestionId(question.getId());
            String checkedIds=spChecked.getString(question.getId().toString(),"");
            result.setRight(isUserRight(checkedIds,question).first);
            result.setType(isUserRight(checkedIds,question).second);
            results.add(result);
        }

        return results;
    }
    /** 对比答案是否正确 **/
    private Pair<Boolean,WrongType> isUserRight(String checkedIds,Question question){
        boolean miss=false, extra=false;
        for (Option option:question.getOptions()){
            if (option.isAnswer()){
                if (!checkedIds.contains(option.getId().toString())){
                    miss=true;
                }
            }else {
                if (checkedIds.contains(option.getId().toString())){
                    extra=true;
                }
            }
        }

        if (miss && extra){
            return new Pair<>(false,WrongType.WRONG_OPTIONS);
        }else if (miss){
            return new Pair<>(false,WrongType.MISS_OPTIONS);
        }else if (extra){
            return new Pair<>(false,WrongType.EXTRA_OPTIONS);
        }else {
            return new Pair<>(true,WrongType.RIGHT_OPTIONS);
        }

    }
    /** 判断错题类型 **/
    private WrongType getWrongType(String checkedIds, Question question){

        return null;

    }

}