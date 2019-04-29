package net.lzzy.practicesonline.activities.activities;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import net.lzzy.practicesonline.R;
import net.lzzy.practicesonline.activities.fragments.PracticesFragment;
import net.lzzy.practicesonline.activities.models.PracticeFactory;
import net.lzzy.practicesonline.activities.network.DetectWebService;
import net.lzzy.practicesonline.activities.utils.AppUtils;
import net.lzzy.practicesonline.activities.utils.ViewUtils;

/**
 * Created by lzzy_gxy on 2019/4/16.
 * Description:
 */
public class PracticesActivity extends BaseActivity implements PracticesFragment.OnPracticesSelectedListener{
    public static final String EXTRA_LOCAL_COUNT ="localhost" ;
    private static final String EXTRA_PRACTICE_ID ="practiceId" ;
    private static final String EXTRA_API_ID = "apiId";
    private ServiceConnection connection;
    private boolean refresh=false;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent()!=null){
            refresh=getIntent().getBooleanExtra(DetectWebService.EXTRA_REFRESH,false);
        }

        connection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DetectWebService.DetectWebBinder binder=(DetectWebService.DetectWebBinder) service;
                binder.detect();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        SearchView search= findViewById(R.id.bar_title_sv_search);
        search.setQueryHint("输入搜索的关键词");
        search.setOnQueryTextListener(new ViewUtils.AbstractQueryListener() {
            @Override
            public void handleQuery(String kw) {
                ((PracticesFragment)getFragment()).search(kw);
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

        });

        /**④Activity中创建ServiceConnection**/
        connection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DetectWebService.DetectWebBinder binder= (DetectWebService.DetectWebBinder) service;
                binder.detect();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        //读取本地数据，传到DetectWebService,进行对比
        int localCount = PracticeFactory.getInstance().get().size();
        Intent intent=new Intent(this,DetectWebService.class);
        intent.putExtra(EXTRA_LOCAL_COUNT,localCount);

        /**⑤Activity中启动Service(bindService/startService)**/
        bindService(intent,connection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refresh){
            ((PracticesFragment)getFragment()).staetRefresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_practices;
    }

    @Override
    protected int getContainerId() {
        return R.id.activity_practoces_container;
    }

    @Override
    protected Fragment createFragment() {
        return new PracticesFragment();
    }

    @Override
    public void onPracticesSelected(String practiceId, int apiId) {
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra(EXTRA_PRACTICE_ID, practiceId);
        intent.putExtra(EXTRA_API_ID, apiId);
        startActivity(intent);


    }


    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("要退出吗？")
                .setPositiveButton("确定", (dialog, which) -> AppUtils.exit())
                .show();
    }


}
