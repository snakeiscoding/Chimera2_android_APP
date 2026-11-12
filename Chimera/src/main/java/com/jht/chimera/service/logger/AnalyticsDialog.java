package com.jht.chimera.service.logger;

import static com.jht.chimera.lib.common.analytic.AnalyticsUtil.STATS_FILE;
import static com.jht.chimera.lib.common.analytic.AnalyticsUtil.WORKOUT_FOLDER;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SimpleArraySelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.analytic.AnalyticStats;
import com.jht.chimera.lib.common.analytic.AnalyticWorkoutSession;
import com.jht.chimera.lib.common.config.ChimeraEnums;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("HardCodedStringLiteral")
public class AnalyticsDialog extends Dialog {
    private static final String TAG = "AnalyticsDialog";
    Context mContext;
    TextView mTextView;
    SimpleArraySelectorItem mItems;
    WebView mWebview;

    public AnalyticsDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        //setCanceledOnTouchOutside(true);

        setContentView(R.layout.analytics_dlg);

        findViewById(R.id.messageBoxCloseImageButton).setOnClickListener(v -> dismiss());

        WindowManager window = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int widthPixels = size.x;
        int heightPixels = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = (int) (widthPixels * .95);
        lp.height = (int) (heightPixels * .95);
        getWindow().setAttributes(lp);

        mTextView = findViewById(R.id.messageBoxInfoTextView);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        mWebview = findViewById(R.id.webview);
        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);

        init();
    }

    void init() {

        List<String> absoluteFileList = new LinkedList<>();
        List<String> tmpFileList = new LinkedList<>();
        File statsFile = new File(STATS_FILE);
        if (statsFile.exists()) {
            tmpFileList.add(statsFile.getName());
            absoluteFileList.add(statsFile.getAbsolutePath());
        }

        File workoutFolder = new File(WORKOUT_FOLDER);
        File[] files = workoutFolder.listFiles();
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        if (files != null) {
            for (File file : files) {
                tmpFileList.add(file.getName());
                absoluteFileList.add(file.getAbsolutePath());
            }
        }
        String[] fileList = tmpFileList.toArray(new String[0]);
        mItems = new SimpleArraySelectorItem(fileList, 0, Gravity.START | Gravity.CENTER_VERTICAL, 20, 0, 0, 0);

        RecyclerView fileSelector = findViewById(R.id.file_selector);
        SelectorDefaultAnimations.setDefaultAnimations(mContext, fileSelector);
        final SelectorAdapterBase adapter = new SelectorAdapterBase(getContext(), mItems, item -> {
            loadFile(absoluteFileList.get(item.getIndex()));
        });
        fileSelector.setAdapter(adapter);

        if (absoluteFileList.size() > 0) loadFile(absoluteFileList.get(0));
    }

    void loadFile(String path) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = "";
        File statsFile = new File(STATS_FILE);
        try {
            if (statsFile.getAbsolutePath() == path) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
                AnalyticStats stats = gson.fromJson(bufferedReader, AnalyticStats.class);
                if (stats == null) stats = new AnalyticStats();
                bufferedReader.close();
                json = gson.toJson(stats);
                loadWebview(stats);
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
                AnalyticWorkoutSession session = gson.fromJson(bufferedReader, AnalyticWorkoutSession.class);
                if (session == null) session = new AnalyticWorkoutSession();
                bufferedReader.close();
                json = gson.toJson(session);
                loadWebview(session);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        mTextView.setText(json);
    }

    String mapToPieChart(Map<String, Long> map, String title) {
        StringBuilder chart = new StringBuilder();
        chart.append("var data = new google.visualization.DataTable();");
        chart.append("data.addColumn('string', 'Key');");
        chart.append("data.addColumn('number', 'Value');");
        chart.append("data.addRows([");

        for (Map.Entry<String, Long> entry : map.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            chart.append("['" + key + "', " + value + "],");
        }

        chart.append("]);");

        chart.append("        // Set chart options\n");
        chart.append("        var options = {'title':'" + title + "', legend: { textStyle: { fontSize: 10 } }, 'width':'100%', 'height':300};");
        chart.append("        // Instantiate and draw our chart, passing in some options.\n");
        chart.append("        var node = document.createElement(\"div\");\n");
        chart.append("        document.getElementById(\"chart_div\").appendChild(node);\n");
        chart.append("        var chart = new google.visualization.PieChart(node);");
        chart.append("        chart.draw(data, options);\n");
        return chart.toString();
    }

    final String mBodyTop = "<html>\n" +
            "  <head>\n" +
            "    <!--Load the AJAX API-->\n" +
            "    <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
            "    <script type=\"text/javascript\">\n" +
            "\n" +
            "      // Load the Visualization API and the corechart package.\n" +
            "      google.charts.load('current', {'packages':['corechart']});\n" +
            "\n" +
            "      // Set a callback to run when the Google Visualization API is loaded.\n" +
            "      google.charts.setOnLoadCallback(drawChart);\n" +
            "\n" +
            "      // Callback that creates and populates a data table,\n" +
            "      // instantiates the pie chart, passes in the data and\n" +
            "      // draws it.\n" +
            "      function drawChart() {\n";
    final String mBodyBottom = "      }\n" +
            "    </script>\n" +
            "  </head>\n" +
            "\n" +
            "  <body>\n" +
            "    <!--Div that will hold the chart-->\n" +
            "    <div id=\"chart_div\"></div>\n" +
            "  </body>\n" +
            "</html>";

    void loadWebview(AnalyticWorkoutSession session) {
        StringBuilder html = new StringBuilder();
        html.append(mBodyTop);
        html.append(mapToPieChart(session.workoutActivityInfoTimeSecMap, "Usage Distribution (seconds)"));
        html.append(mBodyBottom);

        mWebview.loadData(html.toString(), "text/html", "UTF-8");
    }

    void loadWebview(AnalyticStats stats) {

        StringBuilder html = new StringBuilder();
        html.append(mBodyTop);
        html.append(mapToPieChart(stats.workoutActivityInfoCountMap, "Usage Distribution (count)"));
        html.append(mapToPieChart(stats.workoutActivityInfoTimeSecMap, "Usage Distribution (seconds)"));
        html.append(mapToPieChart(stats.loginTypeCountMap, "Login Type (count)"));
        html.append(mapToPieChart(stats.workoutIdMap, "Workout Id (count)"));
        html.append(mBodyBottom);

        mWebview.loadData(html.toString(), "text/html", "UTF-8");

    }
}