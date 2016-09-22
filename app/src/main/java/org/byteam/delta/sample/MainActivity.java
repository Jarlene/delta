package org.byteam.delta.sample;

import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.byteam.delta.Delta;
import org.byteam.delta.PatchListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mFixBtn, mCleanBtn;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFixBtn = (Button) findViewById(R.id.btn_fix);
        mFixBtn.setOnClickListener(this);
        mCleanBtn = (Button) findViewById(R.id.btn_clean);
        mCleanBtn.setOnClickListener(this);
        mTextView = (TextView) findViewById(R.id.text);
        setTextView();
    }

    private void setTextView() {
        mTextView.setText("Not Fixed!");
    }

    @Override
    public void onClick(View v) {
        if (v == mFixBtn) {
            Delta.applyPatchForTest(getApplicationContext(), new PatchListener() {
                @Override
                public void patchResult(boolean result, String message) {
                    if (result) {
                        Toast.makeText(getApplicationContext(), "补丁应用成功,请重启查看效果",
                                Toast.LENGTH_SHORT).show();
                        mFixBtn.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Process.killProcess(Process.myPid());
                                System.exit(0);
                            }
                        }, 2000);
                    } else {
                        Toast.makeText(getApplicationContext(), "补丁应用失败:" + message,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (v == mCleanBtn) {
            Delta.clear(getApplicationContext());
        }
    }
}
