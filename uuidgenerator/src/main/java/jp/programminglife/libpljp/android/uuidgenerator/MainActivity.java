package jp.programminglife.libpljp.android.uuidgenerator;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import jp.programminglife.libpljp.android.Logger;
import jp.programminglife.libpljp.android.UUIDUtilsJava;
import jp.programminglife.libpljp.android.uuidgenerator.databinding.MainActivityABinding;

public class MainActivity extends AppCompatActivity {

    private Logger log = Logger.Companion.get(getClass());

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final MainActivityABinding binding = DataBindingUtil.setContentView(this, R.layout.main_activity_a);

        binding.aGenerateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = 1;
                try {
                    n = Integer.parseInt(binding.aNumText.getText().toString());
                }
                catch (Exception e) {
                    log.d(e, "数のパースエラー");
                }
                final Editable nodeIdText = binding.aNodeIdText.getText();
                long nodeId = 0;
                if ( nodeIdText == null || nodeIdText.length() == 0 ) {
                    nodeId = UUIDUtilsJava.getDeviceNodeId(MainActivity.this);
                }
                else {
                    try {
                        nodeId = Long.parseLong(nodeIdText.toString(), 16);
                    }
                    catch (Exception e) {
                        log.d(e, "ノードIDのパースエラー");
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (int i=0; i<n; i++) {
                    sb.append(UUIDUtilsJava.generate(nodeId, System.currentTimeMillis())).append("\n");
                }
                binding.aUuidText.setText(sb.toString());
            }
        });

    }

}
