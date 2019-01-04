package zhou.monitor;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class Activity1 extends AppCompatActivity {
    private EditText et;
    private TextView tv;
    private RadioButton rb1;
    private RadioButton rb2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(), "onCreate: ");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_1);

        tv = (TextView)findViewById(R.id.text2);
        Button b1 = (Button)findViewById(R.id.button1);
        b1.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                /* 取得输入的身高 */
                et = (EditText)findViewById(R.id.height);
                double height = Double.parseDouble(et.getText().toString());

                /* 取得性别 */
                String sex = "";
                rb1 = (RadioButton)findViewById(R.id.sex1);
                rb2 = (RadioButton)findViewById(R.id.sex2);

                if(rb1.isChecked()) sex = "M";
                else sex = "F";

                /* new 一个Intent对象，并指定class */
                Intent intent = new Intent();
                intent.setClass(Activity1.this, Activity2.class);
                /* new 一个bundle对象，并将要传递的数据传入 */
                Bundle bundle = new Bundle();
                bundle.putDouble("height", height);
                bundle.putString("sex", sex);
                /* 将bundle对象assign个Intent */
                intent.putExtras(bundle);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        tv.setText("121212");
        switch (resultCode){
            case RESULT_OK:
                /* 取得数据，并显示在画面上 */
                Bundle bundle = data.getExtras();
                String sex = bundle.getString("sex");
                double height = bundle.getDouble("height");
                et.setText("" + height);
                if(sex.equals("M")) rb1.setChecked(true);
                else rb2.setChecked(true);
                tv.setText(String.valueOf(height));
                break;
            default:
                break;
        }
    }


}
