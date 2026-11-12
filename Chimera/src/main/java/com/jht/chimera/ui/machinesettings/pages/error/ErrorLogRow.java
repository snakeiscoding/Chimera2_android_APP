package com.jht.chimera.ui.machinesettings.pages.error;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datetime.DateTimeUtil;
import com.jht.chimera.ui.machinesettings.ErrorCodeDetails;
import com.jht.chimera.ui.machinesettings.adapters.error.ErrorLogItem;
import com.jht.liberror.ErrorLog;
import com.jht.liberror.SQLiteHelper;

import java.util.Locale;

public class ErrorLogRow extends RelativeLayout {
    private ErrorLog errorLog;

    public ErrorLogRow(Context context) {
        this(context, null);
    }

    public ErrorLogRow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ErrorLogRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(LayoutInflater.from(context).inflate(R.layout.error_log_row, (ViewGroup)null, false), lp);

        setOnClickListener(v -> {
            if(errorLog != null) {
                try {
                    Intent intent = new Intent(context, ErrorCodeDetails.class);
                    intent.putExtra("error_log", errorLog);
                    getContext().startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        });

    }
    
    @SuppressLint("SetTextI18n")
    private void setError(ErrorLog errorLog, String description) {

        findViewById(R.id.ec_row_time);
        findViewById(R.id.ec_row_description);
        findViewById(R.id.ec_row_count);

        ((TextView)findViewById(R.id.ec_row_error_code)).setText(String.format(Locale.ENGLISH, "%04X", errorLog.errorCode));
        ((TextView)findViewById(R.id.ec_row_count)).setText(errorLog.count + "");
        ((TextView)findViewById(R.id.ec_row_time)).setText(DateTimeUtil.getDate(SQLiteHelper.format(errorLog.lastOccurrence)));
        ((TextView)findViewById(R.id.ec_row_description)).setText(description);

        this.errorLog = errorLog;
    }

    public void link(ErrorLogItem item) {
        CheckBox selectRow = findViewById(R.id.select_error);
        selectRow.setOnCheckedChangeListener(null);
        selectRow.setChecked(item.isChecked());
        selectRow.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> item.setIsChecked(isChecked));
        setError(item.log(), item.getErrorDescription());
    }


    
}
