package net.lzzy.practicesonline.activities.fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import net.lzzy.practicesonline.R;
import net.lzzy.practicesonline.activities.activities.PracticesActivity;
import net.lzzy.practicesonline.activities.models.Practice;
import net.lzzy.practicesonline.activities.models.PracticeFactory;
import net.lzzy.practicesonline.activities.models.Question;
import net.lzzy.practicesonline.activities.models.QuestionFactory;
import net.lzzy.practicesonline.activities.models.UserCookies;
import net.lzzy.practicesonline.activities.network.DetectWebService;
import net.lzzy.practicesonline.activities.network.PracticeService;
import net.lzzy.practicesonline.activities.network.QuestionService;
import net.lzzy.practicesonline.activities.utils.AbstractStaticHandler;
import net.lzzy.practicesonline.activities.utils.AppUtils;
import net.lzzy.practicesonline.activities.utils.DateTimeUtils;
import net.lzzy.practicesonline.activities.utils.ViewUtils;
import net.lzzy.sqllib.GenericAdapter;
import net.lzzy.sqllib.ViewHolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by lzzy_gxy on 2019/4/16.
 * Description:
 */
public class PracticesFragment extends BaseFragment {
    public static final int WHAT_PRACTICE_DONE = 0;
    public static final int WHAT_EXCEPTION = 1;
    public static final int  WHAT_QUESTION_EXCEPTION=2;
    public static final int  WHAT_QUESTION_DONE=3;
    private ListView lv;
    private SwipeRefreshLayout swipe;
    private TextView tvHint;
    private TextView tvTime;
    private List<Practice> practices;
    private GenericAdapter<Practice> adapter;
    private PracticeFactory factory=PracticeFactory.getInstance();
    private ThreadPoolExecutor executor= AppUtils.getExecutor();

    private boolean isDelete=false;
    private float touchX1;
    public static final float MIN_DISTANCE = 100;
    OnPracticesSelectedListener listener;

    private DownloadHandler handler=new DownloadHandler(this);
    private static class DownloadHandler extends AbstractStaticHandler<PracticesFragment> {


        public DownloadHandler(PracticesFragment context) {
            super(context);
        }

        @Override
        public void handleMessage(Message msg, PracticesFragment practicesFragment) {
            switch (msg.what){
                case WHAT_PRACTICE_DONE:
                    practicesFragment.tvTime.setText(DateTimeUtils.DATE_TIME_FORMAT.format(new Date()));
                    UserCookies.getInstance().updateLastRefreshTime();
                    try {
                        List<Practice> practices=PracticeService.getPractice(msg.obj.toString());
                        for (Practice practice:practices){
                            practicesFragment.adapter.add(practice);
                        }
                        Toast.makeText(practicesFragment.getContext(), "同步完成", Toast.LENGTH_SHORT).show();
                        practicesFragment.finishRefresh();
                    } catch (Exception e) {
                        e.printStackTrace();
                        practicesFragment.handlerPracticeException(e.getMessage());
                    }
                    break;
                case WHAT_EXCEPTION:
                    practicesFragment.handlerPracticeException(msg.obj.toString());
                    break;

                case WHAT_QUESTION_DONE:
                    UUID practiceId=practicesFragment.factory.getPracticeId(msg.arg1);
                    try {
                        List<Question> questions=QuestionService.getQuestions(msg.obj.toString(),practiceId);
                        practicesFragment.factory.saveQuestion(questions,practiceId);
                        for (Practice practice:practicesFragment.practices){
                            if (practice.getId().equals(practiceId)){
                                practice.setDownloaded(true);
                            }
                            practicesFragment.adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(practicesFragment.getContext(), "下载失败请重试"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    ViewUtils.dismissProgress();
                    break;
                case WHAT_QUESTION_EXCEPTION:
                    ViewUtils.dismissProgress();
                    Toast.makeText(practicesFragment.getContext(), "下载失败请重试\n"+msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;


                default:
                    break;
            }
        }
    }



     class PracticeDownloader extends AsyncTask<Void,Void,String>{
        WeakReference<PracticesFragment>  fragment;
         PracticeDownloader(PracticesFragment fragment){
            this.fragment=new WeakReference<>(fragment);
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PracticesFragment fragment=this.fragment.get();
            fragment.tvTime.setVisibility(View.VISIBLE);
            fragment.tvHint.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return PracticeService.getPracticesFromServer();
            }catch (IOException e){
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            PracticesFragment fragment=this.fragment.get();
            tvTime.setText(DateTimeUtils.DATE_TIME_FORMAT.format(new Date()));
            UserCookies.getInstance().updateLastRefreshTime();
            try {
                List<Practice>practices=PracticeService.getPractice(s);
                for (Practice practice:practices){
                    adapter.add(practice);
                }
                Toast.makeText(getContext(),"同步完成",Toast.LENGTH_SHORT).show();
                finishRefresh();

            }catch (Exception e){
                e.printStackTrace();
                handlerPracticeException(e.getMessage());
            }
        }
    }
    private void handlerPracticeException(String message) {
        finishRefresh();
        Snackbar.make(lv,"同步失败\n"+message,Snackbar.LENGTH_LONG)
                .setAction("重试",v -> {
                    swipe.setRefreshing(true);
                    refreshListener.onRefresh();
                }).show();

    }
    private void finishRefresh(){
        swipe.setRefreshing(false);
        tvTime.setVisibility(View.GONE);
        tvHint.setVisibility(View.GONE);
        NotificationManager manager=(NotificationManager) Objects.requireNonNull(getContext())
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager!=null){
            manager.cancel(DetectWebService.NOTIFICATION_DETECT_ID);
        }


    }

    public void staetRefresh(){
        swipe.setRefreshing(true);
        refreshListener.onRefresh();
    }


    @Override
    protected void populate() {
            initViews();
            loadPractices();
            initSwipe();

    }
    private SwipeRefreshLayout.OnRefreshListener refreshListener = this::downloadPracticesAsync;

    private void downloadPractices(int apiId) {
        tvTime.setVisibility(View.VISIBLE);
        tvHint.setVisibility(View.VISIBLE);
        executor.execute(()-> {
                    try {
                        String json = PracticeService.getPracticesFromServer();
                        handler.sendMessage(handler.obtainMessage(WHAT_PRACTICE_DONE, json));
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.sendMessage(handler.obtainMessage(WHAT_EXCEPTION, e.getMessage()));
                    }
                }
        );
    }
    private void  downloadPracticesAsync(){
        new PracticeDownloader(this).execute();

    }





    private void initSwipe() {
        swipe.setOnRefreshListener(refreshListener);

        //region 解决列表视图往下滑动与下拉刷新动作的冲突
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //判断列表是否需要下拉刷新
                boolean isTop=view.getChildCount()==0||view.getChildAt(0).getTop()>=0;
                swipe.setEnabled(isTop);
            }
        });

        //endregion

    }

    private void loadPractices() {
        practices=factory.get();
        //列表排序
        Collections.sort(practices, new Comparator<Practice>() {
            @Override
            public int compare(Practice o1, Practice o2) {
                return o2.getDownloadDate().compareTo(o1.getDownloadDate());
            }
        });
        //列表显示
        adapter=new GenericAdapter<Practice>(getActivity(),R.layout.practices_item,practices) {
            @Override
            public void populate(ViewHolder viewHolder, Practice practice) {
                viewHolder.setTextView(R.id.practices_items_name,practice.getName());
                TextView tvOutlines=viewHolder.getView(R.id.practices_items_tv_outlines);
                if (practice.isDownloaded()){
                    tvOutlines.setVisibility(View.VISIBLE);
                    tvOutlines.setOnClickListener(v -> new AlertDialog.Builder(getContext())
                            .setMessage(practice.getOutlines())
                            .show());
                }else {
                    tvOutlines.setVisibility(View.GONE);
                }

                Button btnDel=viewHolder.getView(R.id.practices_items_btn_del);
                btnDel.setVisibility(View.GONE);
                btnDel.setOnClickListener(v -> new android.app.AlertDialog.Builder(getActivity())
                        .setTitle("删除确认")
                        .setMessage("要删除该章节吗？")
                        .setNegativeButton("取消",null)
                        .setPositiveButton("确认",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isDelete = false;
                                adapter.remove(practice);
                            }
                        }).show());

                int visible = isDelete?View.VISIBLE:View.GONE;
                btnDel.setVisibility(visible);
                viewHolder.getConvertView().setOnTouchListener(new ViewUtils.AbstractTouchHandler(){
                    @Override
                    public boolean handleTouch(MotionEvent event) {
                        slideToDelete(event,btnDel,practice);
                        return true;
                    }
                });

            }

            @Override
            public boolean persistInsert(Practice practice) {
                return factory.add(practice);
            }

            @Override
            public boolean persistDelete(Practice practice) {
                return factory.deletePracticeAndRelated(practice);
            }
        };
        lv.setAdapter(adapter);
    }

    /** 触摸判断 **/
    private void slideToDelete(MotionEvent event, Button btn,Practice practice) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touchX1=event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float touchX2=event.getX();
                if (touchX1-touchX2 > MIN_DISTANCE){
                    if (!isDelete) {
                        btn.setVisibility(View.VISIBLE);
                        isDelete=true;
                    }
                }else {
                    if(btn.isShown()){
                        btn.setVisibility(View.GONE);
                        isDelete=false;
                    }
                    else {
                        performItemClick(practice);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void performItemClick(Practice practice) {
        if (practice.isDownloaded()){
           //todo:跳转到Question视图
            listener.onPracticesSelected(practice.getId().toString(),practice.getApiId());
        }else{
         new AlertDialog.Builder(getContext())
                 .setMessage("下载该章栏目？")
                 .setPositiveButton("下载",((dialog, which) -> downloadQuestion(practice.getApiId())))
                 .setNegativeButton("取消",null)
                 .show();
        }

    }
    private void downloadQuestion(int apiId){
        ViewUtils.showProgress(getContext(),"开始下载题目...");
        executor.execute(new Runnable() {


            @Override
            public void run() {
                try {
                    String json=QuestionService.getQuestionsOfPracticeFromServer(apiId);
                    Message mag=handler.obtainMessage(WHAT_QUESTION_DONE,json);
                    mag.arg1=apiId;
                    handler.sendMessage(mag);
                } catch (IOException e) {
                    handler.sendMessage(handler.obtainMessage(WHAT_QUESTION_EXCEPTION,e.getMessage()));
                }
            }
        });
    }


    private void  initViews(){
        lv = find(R.id.fragment_practices_lv);
        TextView tvNone=find(R.id.fragment_practices_tv_none);
        lv.setEmptyView(tvNone);
        swipe = find(R.id.fragment_practices_swipe);
        tvHint = find(R.id.fragment_practices_tv_hint);
        tvTime = find(R.id.fragment_practices_time);
        tvTime.setText(UserCookies.getInstance().getLastRefreshTime());
        tvHint.setVisibility(View.GONE);
        tvTime.setVisibility(View.GONE);
        find(R.id.fragment_practices_lv).setOnTouchListener(new ViewUtils.AbstractTouchHandler() {
            @Override
            public boolean handleTouch(MotionEvent event) {
                isDelete=false;
                adapter.notifyDataSetChanged();
                return false;
            }
        });
    }
    @Override
    public int getLayoutRes() {
        return R.layout.fragment_practices;
    }

    @Override
    public void search(String kw) {
      practices.clear();
      if (kw.isEmpty()){
          practices.addAll(factory.get());
      }else {
          practices.addAll(factory.search(kw));
      }
      adapter.notifyDataSetChanged();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            listener= (OnPracticesSelectedListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+"必须实现OnPracticesSelectedListener接口");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener=null;
        handler.removeCallbacksAndMessages(null);
    }


    public interface OnPracticesSelectedListener{
        /**
         * 点击章节跳转题目
         * @param practiceId
         */
        void onPracticesSelected(String practiceId,int apiId);
    }


}
