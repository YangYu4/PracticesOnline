package net.lzzy.practicesonline.activities.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.lzzy.practicesonline.R;
import net.lzzy.practicesonline.activities.activities.SplashActivity;

/**
 * Created by lzzy_gxy on 2019/4/15.
 * Description:
 */
public class ViewUtils {
    public static void gotoSetting(Context context){
        View view= LayoutInflater.from(context).inflate(R.layout.dialog_setting,null);
        Pair<String,String>url=AppUtils.loadServerSetting(context);
        EditText edtIp=view.findViewById(R.id.dialog_setting_edt_ip);
        edtIp.setText(url.first);
        EditText edPort=view.findViewById(R.id.dialog_setting_edt_port);
        edPort.setText(url.second);
        new AlertDialog.Builder(context)
                .setView(view)
                .setNeutralButton("取消",(dialog, which) -> {gotoMian(context);})
                .setPositiveButton("确定",(dialog, which) -> {
                    String ip=edtIp.getText().toString();
                    String port=edPort.getText().toString() ;
                    if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)) {
                        Toast.makeText(context,"信息不完成整,",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AppUtils.saveServerSetting(ip,port,context);
                    gotoMian(context);
                })
                .show();

    }
    private static void gotoMian(Context context){
        if (context instanceof SplashActivity){
            ((SplashActivity)context).gotoMain();
        }
    }
}
