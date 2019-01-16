package zhou.monitor;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import zhou.monitor.activity.AddItemActivity;
import zhou.monitor.activity.SettingActivity;
import zhou.monitor.service.GlobalService;
import zhou.monitor.utility.MLog;
import zhou.monitor.view.MainView;

public class MainActivity extends AppCompatActivity {

    private View itemPressed = null;
    private TextView titleView = null;
    private MainActHandler handler = null;

    private static final String keyTitle = "title";

    private boolean isFront = Boolean.FALSE;    // 是否在前台

    // 处理请求
    class MainActHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            if(bundle.containsKey(keyTitle)){
                if(titleView == null) titleView = (TextView)MainActivity.this.getSupportActionBar().getCustomView().findViewById(R.id.tvTitle);
                titleView.setText(msg.getData().getString(keyTitle));
            }
        }
    }

    /**
     * 设置标题
     * @param title
     */
    public void setTitle(String title){
        Message msg = Message.obtain();
        Bundle bundle = null;
        if(null == (bundle = msg.getData())){
            bundle = new Bundle();
            msg.setData(bundle);
        }
        bundle.putString(keyTitle, title);
        if(handler == null) handler = new MainActHandler();
        handler.sendMessage(msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        handler = new MainActHandler();
        // 在前台
        isFront = Boolean.TRUE;
        // 设置在锁屏状态下也能打开
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|               //这个在锁屏状态下
//                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // 设置标题栏，在中间添加个滚动显示价格的区域
        getSupportActionBar().setCustomView(R.layout.title_bar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        titleView = (TextView)getSupportActionBar().getCustomView().findViewById(R.id.tvTitle);
        // 初始化全局参数
        try{
            GlobalService.init(this);
            // 初始化MainView
            GlobalService.mainView = new MainView(this);
        } catch (Exception e){}

        // 设置内容view
        setContentView(GlobalService.mainView);

        // 启动服务
        startService(new Intent(getBaseContext(), GlobalService.class));

//        Toast.makeText(this, "启动成功", Toast.LENGTH_SHORT).show();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    try{
//                        Thread.sleep(3000);
//                    } catch (Exception e){}
//                    GlobalService.globalSer.feedback();
//                }
//            }
//        }).start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.press_item_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()){
            case R.id.deleteItem:
                GlobalService.mainView.removeItem(itemPressed);
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        switch (item.getItemId()){
            case R.id.addItem:
                intent.setClass(this, AddItemActivity.class);
                startActivityForResult(intent, 0);
                return true;
            case R.id.setting:
                intent.setClass(this, SettingActivity.class);
                startActivityForResult(intent, 0);
                return true;
            default:
                return false;
        }
    }

    /**
     * 截获窗口点击事件
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        Toast.makeText(this, "窗口点击", Toast.LENGTH_SHORT).show();
        GlobalService.closeFeedback();
        return super.dispatchTouchEvent(ev);
    }

    public void setPressedItem(View view){
        this.itemPressed = view;
    }

    @Override
    public void finish() {
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isFront = Boolean.TRUE;
//        Log.d("MainAct", "onResume");
//        Log.d("MainAct", String.valueOf(this == GlobalService.mainAct));
        if(this != GlobalService.mainAct){  // GlobalService.mainAct改变了
            if(GlobalService.mainAct == null) MLog.writeLine("MainActivity.onResume mainAct还原前: GlobalService.mainAct = null");
            else MLog.writeLine("MainActivity.onResume mainAct还原前: " + "GlobalService.mainAct = " + GlobalService.mainAct.toString());
            GlobalService.mainAct = this;
        }
        View view = ((ViewGroup)this.findViewById(android.R.id.content)).getChildAt(0);
//        Log.d("MainAct", String.valueOf(view == GlobalService.mainView));
        if(view != GlobalService.mainView){ // GlobalService.mainView改变了
            if(GlobalService.mainView == null)  MLog.writeLine("MainActivity.onResume mainView还原前: GlobalService.mainView = null");
            else MLog.writeLine("MainActivity.onResume mainView还原前: GlobalService.mainView = " + GlobalService.mainView.toString());
            GlobalService.mainView = (MainView)view;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFront = Boolean.FALSE;
    }

    public Boolean isFront(){
        return isFront;
    }

}

