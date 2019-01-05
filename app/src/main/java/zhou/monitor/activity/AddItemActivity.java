package zhou.monitor.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import zhou.monitor.R;
import zhou.monitor.entity.SettingInfo;
import zhou.monitor.service.GlobalService;
import zhou.monitor.thread.ConnectTest;

public class AddItemActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        init();
    }

    public void init(){
        try {
            // 设置添加价格出价按钮响应
            ((Button)findViewById(R.id.addPrice)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String type = ((EditText)findViewById(R.id.priceType)).getText().toString().trim().toUpperCase();
                    if(type.equals("")){
                        Toast.makeText(AddItemActivity.this, "价格-类型不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(!type.contains("/")){
                        Toast.makeText(AddItemActivity.this, "价格-类型不对，必须以/隔开", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean isBigger = true;
                    String outPrice = ((EditText)findViewById(R.id.priceOut)).getText().toString();
                    if(outPrice.equals("")){
                        Toast.makeText(AddItemActivity.this, "价格-出价不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    } else if(outPrice.startsWith("+")){
                        isBigger = true;
                        outPrice = outPrice.substring(1);
                    } else if(outPrice.startsWith("-")){
                        isBigger = false;
                        outPrice = outPrice.substring(1);
                    } else {
                        Toast.makeText(AddItemActivity.this, "价格-出价必须以 +或- 开头", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 测试连接
                    String result = ConnectTest.test(SettingInfo.priceAddr() + type.replace("/", ""));
                    if(!result.contains("price")){
                        Toast.makeText(AddItemActivity.this, result, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 添加
                    result = GlobalService.addPriceItem(type, isBigger, Double.parseDouble(outPrice));
                    if(result.equals("ok"))     Toast.makeText(AddItemActivity.this, "添加出价成功", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(AddItemActivity.this, "添加失败:" + result, Toast.LENGTH_SHORT).show();
                }
            });

            // 设置添加振幅按钮
            ((Button)findViewById(R.id.addWave)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String type = ((EditText)findViewById(R.id.waveType)).getText().toString().trim().toUpperCase();
                    if(type.equals("")){
                        Toast.makeText(AddItemActivity.this, "振幅-类型不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(!type.contains("/")){
                        Toast.makeText(AddItemActivity.this, "振幅-类型不对，必须以/隔开", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String minuSpan = ((EditText)findViewById(R.id.waveMinuteSpan)).getText().toString();
                    if(minuSpan.equals("")){
                        Toast.makeText(AddItemActivity.this, "振幅-时间间隔不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(Integer.parseInt(minuSpan) < 1){
                        Toast.makeText(AddItemActivity.this, "振幅-时间间隔必须为不小于1的整数", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String wave = ((EditText)findViewById(R.id.waveWave)).getText().toString();
                    if(wave.equals("")){
                        Toast.makeText(AddItemActivity.this, "振幅-振幅不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 测试连接
                    String result = ConnectTest.test(SettingInfo.priceAddr() + type.replace("/", ""));
                    if(!result.contains("price")){
                        Toast.makeText(AddItemActivity.this, result, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    result = GlobalService.addWaveItem(type, Integer.parseInt(minuSpan),Double.parseDouble(wave));
                    if(result.equals("ok"))     Toast.makeText(AddItemActivity.this, "添加振幅成功", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(AddItemActivity.this, "添加失败:" + result, Toast.LENGTH_SHORT).show();
                }
            });

        }catch (Exception e){}
    }
}
