package org.byteam.delta.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.byteam.delta.Delta;

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
        mTextView.setText("Not fixed!");
    }

    @Override
    public void onClick(View v) {
        if (v == mFixBtn) {
            Delta.applyPatchFromDevice(this);
        } else if (v == mCleanBtn) {
            Delta.clean(this);
        }
    }
}
