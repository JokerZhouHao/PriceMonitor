package zhou.monitor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showListView();
    }

    /**
     * 设置ListView显示内容
     */
    private void showListView(){
        ListView listView = (ListView) findViewById(R.id.lv);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.view_limit_price, getData());
        listView.setAdapter(adapter);
        this.registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //设置Menu显示内容
        menu.setHeaderTitle("文件操作");
        menu.add(1, 100, 1, "复制");
        menu.add(1, 101, 1, "剪切");
        menu.add(1, 102, 1, "粘贴");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 100:
                Toast.makeText(this, "点击了复制", Toast.LENGTH_SHORT).show();
                break;
            case 101:
                Toast.makeText(this, "点击了剪切", Toast.LENGTH_SHORT).show();
                break;
            case 102:
                Toast.makeText(this, "点击了粘贴", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }


    /**
     * 构造ListView显示的数据
     * @return
     */
    private ArrayList<String> getData(){
        ArrayList<String> list = new ArrayList<String>();
        for(int i=0; i<5; ++i) {
            list.add("文件" + i);
        }
        return list;
    }
}
