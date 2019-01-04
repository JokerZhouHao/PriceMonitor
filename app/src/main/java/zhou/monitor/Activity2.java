package zhou.monitor;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Activity2 extends AppCompatActivity {
    Bundle bunde;
    Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);
        intent = this.getIntent();
        bunde = intent.getExtras();

        String sex = bunde.getString("sex");
        double height = bunde.getDouble("height");

        String sexText = "";
        if(sex.equals("M")) sexText="男";
        else sexText="女性";

        String weight = this.getWeight(sex, height);

        TextView tv1 = (TextView)findViewById(R.id.text1);
        tv1.setText("你是一位" + sexText + "\n你的身高是" + height + "厘米\n你的标准体重是" + weight + "公斤");

        Button b1 = (Button)findViewById(R.id.button1);
        b1.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                /* 回传result给上一个activity */
                Activity2.this.setResult(RESULT_OK, intent);
                /* 关闭activity */
                Activity2.this.finish();
            }
        });
    }

    /* 进行四舍五入 */
    private  String format(double num){
        NumberFormat formatter = new DecimalFormat("0.00");
        String s = formatter.format(num);
        return s;
    }

    private String getWeight(String sex, double height){
        String weight = "";
        if(sex.equals("M")) return format((height - 80) * 0.7);
        else return format((height - 70) * 0.6);
    }
}
