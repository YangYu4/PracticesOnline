package net.lzzy.practicesonline.activities.activities;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.lzzy.practicesonline.R;
import net.lzzy.practicesonline.activities.constants.ApiConstants;
import net.lzzy.practicesonline.activities.fragments.QuestionFragment;
import net.lzzy.practicesonline.activities.models.FavoriteFactory;
import net.lzzy.practicesonline.activities.models.Question;
import net.lzzy.practicesonline.activities.models.QuestionFactory;
import net.lzzy.practicesonline.activities.models.UserCookies;
import net.lzzy.practicesonline.activities.models.view.PracticeResult;
import net.lzzy.practicesonline.activities.models.view.QuestionResult;
import net.lzzy.practicesonline.activities.network.PracticeService;
import net.lzzy.practicesonline.activities.utils.AbstractStaticHandler;
import net.lzzy.practicesonline.activities.utils.AppUtils;
import net.lzzy.practicesonline.activities.utils.ViewUtils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class QuestionActivity extends AppCompatActivity {
    public static final int WHAT_OK = 0;
    public static final int WHAT_NO = 1;
    public static final int WHAT_EXCEPTION = 2;

    public static final String EXTRA_PRACTICE_ID = "extraPracticeId";
    public static final String EXTRA_RESULT = "extraResult";
    public static final int REQUEST_CODE_RESULT = 2;

    private int apiId;
    private List<Question> questions;
    private TextView tvView;
    private TextView tvCommit;
    private boolean isCommitted = false;
    private ViewPager pager;
    private TextView tvHint;
    private FragmentStatePagerAdapter adapter;
    private String practiceId;
    private int pos;
    private View[] dots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_question);
        AppUtils.addActivity(this);
        retrieveData();
        initViews();
        initDots();
        setListeners();
        int pos = UserCookies.getInstance().getCurrentQuestion(practiceId);
        pager.setCurrentItem(pos);
        UserCookies.getInstance().updateReadCount(questions.get(pos).getId().toString());

    }

    /**
     * 监听
     **/
    private void setListeners() {
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                refreshDots(position);
                UserCookies.getInstance().updateCurrentQuestion(practiceId, position);
                UserCookies.getInstance().updateReadCount(questions.get(position).getId().toString());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

        });
        tvCommit.setOnClickListener(v -> commitPractice());
        tvView.setOnClickListener(v -> redirect());
    }

    /**
     * 查看
     **/
    private void redirect() {
        List<QuestionResult> results = UserCookies.getInstance().getResultFromCookies(questions);
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(EXTRA_PRACTICE_ID, practiceId);
        intent.putParcelableArrayListExtra(EXTRA_RESULT, (ArrayList<? extends Parcelable>) results);
        startActivityForResult(intent, REQUEST_CODE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==ResultActivity.RESULT_CODE&&requestCode==REQUEST_CODE_RESULT) {
            pager.setCurrentItem(data.getIntExtra(ResultActivity.POSITION,-1));
        }

        if (requestCode==ResultActivity.RESULT_CODE_PRACTICE && resultCode==REQUEST_CODE_RESULT&&data!=null){
            String practiceId=data.getStringExtra(ResultActivity.PRACTICES_ID);
            if (!practiceId.isEmpty()){
                List<Question> questionList=new ArrayList<>();
                FavoriteFactory factory= FavoriteFactory.getInstance();
                for (Question question:QuestionFactory.getInstance().getByPractice(practiceId)){
                    if (factory.isQuestionStarred(question.getId().toString())){
                        questionList.add(question);
                    }
                }
                questions.clear();
                questions.addAll(questionList);
                initDots();
                adapter.notifyDataSetChanged();
                if (questions.size()>0){
                    pager.setCurrentItem(0);
                    refreshDots(0);
                }

            }

        }

    }

    /**
     * 提交
     **/
    String info;

    private void commitPractice() {
        List<QuestionResult> results = UserCookies.getInstance().getResultFromCookies(questions);
        List<String> macs = AppUtils.getMacAddress();
        String[] items = new String[macs.size()];
        macs.toArray(items);
        info = items[0];
        new AlertDialog.Builder(this)
                .setTitle("选择Mac地址")
                .setSingleChoiceItems(items, 0, (dialog, which) -> info = items[which])
                .setNegativeButton("取消", null)
                .setPositiveButton("提交", (dialog, which) -> {
                    PracticeResult result = new PracticeResult(results, apiId, "杨誉" + info);
                    postResult(result);
                }).show();
    }

    private void postResult(PracticeResult result) {
        AppUtils.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int code = PracticeService.postResult(result);
                    if (code >= 200 && code <= 220) {
                        handler.sendEmptyMessage(WHAT_OK);
                    } else {
                        handler.sendEmptyMessage(WHAT_NO);
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(WHAT_EXCEPTION);
                }
            }
        });

    }
    //endregion

    private QuestionActivity.CountHandler handler = new QuestionActivity.CountHandler(this);

    public static class CountHandler extends AbstractStaticHandler<QuestionActivity> {

        public CountHandler(QuestionActivity context) {
            super(context);
        }

        @Override
        public void handleMessage(Message msg, QuestionActivity questionActivity) {
            switch (msg.what) {
                case WHAT_OK:
                    questionActivity.isCommitted = true;
                    Toast.makeText(questionActivity, "提交成功", Toast.LENGTH_SHORT).show();
                    UserCookies.getInstance().commitPractice(questionActivity.practiceId);
                    questionActivity.redirect();
                    break;
                case WHAT_NO:
                    Toast.makeText(questionActivity, "提交失败", Toast.LENGTH_SHORT).show();
                    break;
                case WHAT_EXCEPTION:
                    Toast.makeText(questionActivity, "异常", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;

            }
        }

    }

    /**
     * pager导航点的操作及显示
     **/
    private void initDots() {
        int count = questions.size();
        dots = new View[count];
        LinearLayout container = findViewById(R.id.activity_question_dots);
        container.removeAllViews();
        int px = ViewUtils.dp2px(16, this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px, px);
        px = ViewUtils.dp2px(5, this);
        params.setMargins(px, px, px, px);
        for (int i = 0; i < count; i++) {
            TextView tvDot = new TextView(this);
            tvDot.setLayoutParams(params);
            tvDot.setBackgroundResource(R.drawable.dot_style);
            tvDot.setTag(i);
            tvDot.setOnClickListener(v -> pager.setCurrentItem((Integer) v.getTag()));
            container.addView(tvDot);
            dots[i] = tvDot;
        }
    }

    private void refreshDots(int pos) {
        for (int i = 0; i < dots.length; i++) {
            int drawable = i == pos ? R.drawable.dot_fill_style : R.drawable.dot_style;
            dots[i].setBackgroundResource(drawable);
        }
    }

    private void initViews() {
        tvView = findViewById(R.id.activity_question_tv_view);
        tvCommit = findViewById(R.id.activity_question_tv_commit);
        tvHint = findViewById(R.id.activity_question_hint);
        pager = findViewById(R.id.activity_question_pager);
        if (isCommitted) {
            tvCommit.setVisibility(View.GONE);
            tvView.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.VISIBLE);
        } else {
            tvView.setVisibility(View.GONE);
            tvCommit.setVisibility(View.VISIBLE);
            tvHint.setVisibility(View.GONE);
        }
        adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                Question question = questions.get(position);
                return QuestionFragment.newInstance(question.getId().toString(), position, isCommitted);
            }

            @Override
            public int getCount() {
                return questions.size();
            }
        };
        pager.setAdapter(adapter);
    }

    /**
     * 获取Question数据
     **/
    private void retrieveData() {
        practiceId = getIntent().getStringExtra(EXTRA_PRACTICE_ID);
        apiId = getIntent().getIntExtra(PracticesActivity.EXTRA_API_ID, -1);
        questions = QuestionFactory.getInstance().getByPractice(practiceId);
        isCommitted = UserCookies.getInstance().isPracticeCommitted(practiceId);
        if (apiId < 0 || questions == null || questions.size() == 0) {
            Toast.makeText(this, "no question", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppUtils.removeActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppUtils.setRunning(getLocalClassName());

    }

    @Override
    protected void onStop() {
        super.onStop();
        AppUtils.setStopped(getLocalClassName());
    }


}
