package net.lzzy.practicesonline.activities.models;

import net.lzzy.practicesonline.activities.constants.DbConstants;
import net.lzzy.practicesonline.activities.utils.AppUtils;
import net.lzzy.sqllib.SqlRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author lzzy_gxy
 * @date 2019/4/16
 * Description:
 */
public class PracticeFactory {
    private static final PracticeFactory OUR_INSTANCE = new PracticeFactory();
    private SqlRepository<Practice> repository;

    public static PracticeFactory getInstance() {
        return OUR_INSTANCE;
    }

    private PracticeFactory() {
        repository = new SqlRepository<>(AppUtils.getContext(),Practice.class, DbConstants.packager);
    }

    public List<Practice> get(){
        return repository.get();
    }

    public Practice getById(String id){
        return repository.getById(id);
    }

    public List<Practice> search(String kw){
        try {
            return repository.getByKeyword(kw,
                    new String[]{Practice.COL_NAME,Practice.COL_OUTLINES},false);
        } catch (IllegalAccessException|InstantiationException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** 根据apiId判断是否已经下载过该数据 **/

    private boolean isPracticeDb(Practice practice) {
        try {
            return repository.getByKeyword(String.valueOf(practice.getApiId()),
                    new String[]{Practice.COL_API_ID},true).size() > 0;
        } catch (IllegalAccessException |InstantiationException e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean add(Practice practice){
        if (isPracticeDb(practice)){
            return false;
        }
        repository.insert(practice);
        return true;
    }

    /**  **/
    public UUID getPractice(int apiId){
        try {
            List<Practice> practices=repository.getByKeyword(String.valueOf(apiId),
                    new String[]{Practice.COL_API_ID},true);
            if (practices.size()>0){
                return practices.get(0).getId();
            }
        } catch (IllegalAccessException |InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** 下载 **/
    public void setPracticeDown(String id){
        Practice practice=getById(id);
        if (practice!=null){
            practice.setDownloaded(true);
            repository.update(practice);
        }
    }

    /** 保存题目 **/
    public void saveQuestion(List<Question> questions,UUID practiceId){
        for (Question q:questions){
            QuestionFactory.getInstance().insert(q);
        }
        setPracticeDown(practiceId.toString());
    }

    /** 删除跟章节有关的题目 **/
    public boolean deletePracticeAndRelated(Practice practice) {
        try {
            List<String> sqlActions = new ArrayList<>();
            sqlActions.add(repository.getDeleteString(practice));
            QuestionFactory factory = QuestionFactory.getInstance();
            List<Question> questions = factory.getByPractice(practice.getId().toString());
            if (questions.size() > 0) {
                for (Question q : questions) {
                    sqlActions.addAll(factory.getDeleteString(q));
                }
            }
            repository.exeSqls(sqlActions);
            if (!isPracticeDb(practice)){
                //todo: 清除Cookies
            }
            return true;
        }catch (Exception e){
            return false;
        }
    }

}
