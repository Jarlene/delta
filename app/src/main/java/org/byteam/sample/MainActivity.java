package org.byteam.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.byteam.tp.Tp;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mFixBtn;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFixBtn = (Button) findViewById(R.id.btn_fix);
        mFixBtn.setOnClickListener(this);
        mTextView = (TextView) findViewById(R.id.text);
        setTextView();
    }

    private void setTextView() {
        mTextView.setText("Fixed!");
    }

    @Override
    public void onClick(View v) {
        if (v == mFixBtn) {
            Tp.applyPatchFromDevice(this);
        }
    }
}
