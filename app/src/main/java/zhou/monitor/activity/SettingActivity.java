package zhou.monitor.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import zhou.monitor.R;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.service.GlobalService;
import zhou.monitor.thread.ConnectTest;
import zhou.monitor.utility.MLog;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        try{
            init();
        } catch (Exception e){

        }
    }

    public void init() throws Exception{
        // 声音提醒、振动提醒设置
        if(SettingInfo.onSound) ((RadioButton)findViewById(R.id.soundOn)).setChecked(true);
        else ((RadioButton)findViewById(R.id.soundClose)).setChecked(true);
        if(SettingInfo.onVibrate) ((RadioButton)findViewById(R.id.vibrateOn)).setChecked(true);
        else ((RadioButton)findViewById(R.id.vibrateClose)).setChecked(true);
        ((Button)findViewById(R.id.setSoundAndVibrate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if(((RadioButton)findViewById(R.id.soundOn)).isChecked())   SettingInfo.onSound = true;
                else SettingInfo.onSound = false;
                if(((RadioButton)findViewById(R.id.vibrateOn)).isChecked())  SettingInfo.onVibrate = true;
                else SettingInfo.onVibrate = false;
                SettingInfo.writeToFile(GlobalService.pathConfig);
                Toast.makeText(SettingActivity.this, "设置声音、振动成功", Toast.LENGTH_SHORT).show();
            }
        });

        // 初始化服务器地址设置
        ((EditText)findViewById(R.id.serviceAdd)).setText(SettingInfo.serveAddr);
        ((Button)findViewById(R.id.updateServeAddr)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingInfo.serveAddr = ((EditText)findViewById(R.id.serviceAdd)).getText().toString().trim();
                SettingInfo.writeToFile(GlobalService.pathConfig);
                Toast.makeText(SettingActivity.this, "设置服务器地址成功", Toast.LENGTH_SHORT).show();
            }
        });

        // 初始化测试栏
        final EditText etAddr = (EditText)findViewById(R.id.etAddTest);
        etAddr.setText(SettingInfo.priceAddr);
        ((Button)findViewById(R.id.priceAddrTest)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                try{
                    String url = etAddr.getText().toString();
                    String response = ConnectTest.test(url);
                    etAddr.setText(url + "\n" + response);
                } catch (Exception e){}
            }
        });

        ((Button)findViewById(R.id.priceAddUpdate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    String priceUrl = etAddr.getText().toString();
                    String url = priceUrl  + "ETHUSDT";
                    String response = ConnectTest.test(url);
                    etAddr.setText(url + "\n" + response);
                    SettingInfo.priceAddr = priceUrl;
                    SettingInfo.writeToFile(GlobalService.pathConfig);
                    Toast.makeText(SettingActivity.this, "更新网址成功", Toast.LENGTH_SHORT);
                } catch (Exception e){}
            }
        });

        // 显示日志内容
        ((EditText)findViewById(R.id.etLog)).setText(MLog.loadLog());
    }
}
