package com.jht.chimera.ui.machinesettings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datetime.DateTimeUtil;
import com.jht.chimera.ui.machinesettings.errorostringsdatabase.ErrorCodeStrings;
import com.jht.chimera.ui.machinesettings.errorostringsdatabase.ErrorStringsDB;
import com.jht.liberror.ErrorLog;
import com.jht.liberror.SQLiteHelper;

import java.util.Locale;

public class ErrorCodeDetails extends RestringActivity {
    private TextView errorCodeText = null;
    private TextView errorCodeCount = null;
    private TextView errorCodeTime = null;
    private TextView errorCodeDescription = null;
    private ErrorStringsDB errorStringsDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error_code_details);
        errorCodeText = findViewById(R.id.error_code_heading);
        errorCodeCount = findViewById(R.id.ec_detail_count);
        errorCodeTime = findViewById(R.id.ec_detail_time);
        errorCodeDescription = findViewById(R.id.ec_detail_details);

        findViewById(R.id.eng_home).setOnClickListener(v -> LaunchAppUtil.launchSplashScreen());
        findViewById(R.id.error_code_back).setOnClickListener(v -> finish());
        errorStringsDB = new ErrorStringsDB(getApplicationContext());
        processIntent();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent();
    }

    @SuppressLint("SetTextI18n")
    private void processIntent() {
        Intent intent = getIntent();

        ErrorLog log = intent.getParcelableExtra("error_log");
        if(log != null && errorCodeText != null) {
            errorCodeText.setText(String.format(Locale.ENGLISH, "Error Code: %04X", log.errorCode));
            errorCodeCount.setText(log.count + "");
            StringBuilder timeStampBuilder = new StringBuilder();
            for(final String occurrence : log.occurrences) {
                timeStampBuilder.append(DateTimeUtil.getDate(SQLiteHelper.format(occurrence))).append("\n");
            }
            errorCodeTime.setText(timeStampBuilder.toString());
            ErrorCodeStrings errorDescriptionContents = errorStringsDB.getServiceDescriptions(log.errorCode);
            if (errorDescriptionContents != null) {

                String errorDetails = "<big><b>DESCRIPTION</b></big><br />" + // NON-NLS
                        makeHeaderLinesBold(errorDescriptionContents.description) + // NON-NLS
                        "<br/><br/>" + // NON-NLS
                        "<big><b>CAUSE</b></big><br />" + // NON-NLS
                        makeHeaderLinesBold(errorDescriptionContents.cause) + // NON-NLS
                        "<br/><br/>" + // NON-NLS
                        "<big><b>BASIC TROUBLESHOOTING</b></big><br />" + // NON-NLS
                        makeHeaderLinesBold(errorDescriptionContents.troubleshooting); // NON-NLS
                Spanned sp = Html.fromHtml(errorDetails.replace("\n", "<br/>")); // NON-NLS
                errorCodeDescription.setText(sp);
            }
        }
    }

    private String makeHeaderLinesBold(String text) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        if(lines == null) {
            return "";
        }
        for(String line : lines) {
            if(line.endsWith(":")) {
                result.append("<b>").append(line).append("</b><br/>"); //NON-NLS
            }
            else {
                result.append(line).append("<br/>"); //NON-NLS
            }
        }
        return result.toString();
    }
}
