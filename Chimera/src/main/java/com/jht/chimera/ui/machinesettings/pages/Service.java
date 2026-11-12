package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SimpleArraySelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.log.ServiceLog;
import com.jht.chimera.lib.common.log.ServiceLogList;
import com.jht.chimera.lib.language.StringHelper;
import com.jht.chimera.lib.language.StringHelper.ServiceTypes;
import com.jht.chimera.ui.machinesettings.adapters.service.ServiceLogItem;
import com.jht.chimera.ui.machinesettings.dialogs.EngDialogFactory;
import com.jht.chimera.ui.machinesettings.dialogs.ServiceLogEntry;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Implements the service log machine page gluing the page to the model layer.
 */
public class Service extends SettingPageBase {

    private DataMatrix dataMatrix = new DataMatrix();
    private FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

    private SelectorAdapterBase adapter;
    private RecyclerView table;
    private SelectorItemBase base;


    public Service() {
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.service, null);
        container.addView(view);

        table = view.findViewById(R.id.eng_service_history_list);
        SelectorDefaultAnimations.setDefaultAnimations(context, table);
        base = new SelectorItemBase(new ArrayList<ISelectorItem>());
        adapter = new SelectorAdapterBase(getContext(), base, null);
        table.setAdapter(adapter);

        final RecyclerView serviceSelector = view.findViewById(R.id.eng_service_history_selector);
        SelectorDefaultAnimations.setDefaultAnimations(context, serviceSelector);

        final ServiceTypes[] logOptions = getServiceOptions();
        serviceSelector.setAdapter(new SelectorAdapterBase(getContext(), new SimpleArraySelectorItem(getServiceOptionText(logOptions), -1),
                item -> EngDialogFactory.serviceLogEntry(serviceSelector.getLayoutManager().findViewByPosition(item.getIndex()), logOptions[item.getIndex()], new ServiceLogEntry.IOnEntryComplete() {
                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onEntry(ServiceTypes serviceType, String description) {
                        final ServiceLog log = new ServiceLog();
                        log.serviceType = serviceType.ordinal();
                        log.accumulatedFloor = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT);
                        log.accumulatedDistanceKm = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
                        log.accumulatedTimeSec = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
                        log.logTime = Calendar.getInstance();
                        log.message = description;
                        DataMatrix.putParcelable(LoggerKeys.ACTION_ADD_SERVICE_PARCELABLE, log);
                    }
                })));

        return view;

    }

    private void deleteLog(ServiceLog log) {
        DataMatrix.putParcelable(LoggerKeys.ACTION_DELETE_SERVICE_LOG_PARCELABLE, log);
    }

    private String[] getServiceOptionText(ServiceTypes[] option) {
        String[] text = new String[option.length];
        for (int i = 0; i < option.length; i++) {
            text[i] = StringHelper.getServiceTypeString(getContext(), option[i]);
        }
        return text;
    }

    private ServiceTypes[] getServiceOptions() {
        switch (config.machineClassType) {
            case Treadmill:
            case Climbmill:
            case Stepper:
            case Ascent:
            case Bike:
            case SpinBike:
            case Elliptical:
            case RecumbentElliptical:
                if (ProductConfig.isLightCommercial()) {
                    return new ServiceTypes[]{
                            ServiceTypes.Battery,
                            ServiceTypes.BeltDrive,
                            ServiceTypes.BeltGeneratorFlyWheel,
                            ServiceTypes.BeltRunningFlipDeck,
                            ServiceTypes.BeltRunningNewDeck,
                            ServiceTypes.ConsoleCable,
                            ServiceTypes.LubeBelt
                    };
                }
                return new ServiceTypes[]{
                        ServiceTypes.Battery,
                        ServiceTypes.BeltDrive,
                        ServiceTypes.BeltGeneratorFlyWheel,
                        ServiceTypes.BeltRunningFlipDeck,
                        ServiceTypes.BeltRunningNewDeck,
                        ServiceTypes.ChainDrive,
                        ServiceTypes.StairDrive,
                        ServiceTypes.ConsoleCable,
                        ServiceTypes.LubeBelt
                };
            default:
                return new ServiceTypes[0];
        }
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        updateAdapter();
        dataMatrix.createParcelableAdapter(LoggerKeys.DATA_SERVICE_LOG_LIST_PARCELABLE, ServiceLogList.class, list -> updateAdapter());
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }

    private void updateAdapter() {

        ServiceLogList errorLogList = DataMatrix.getParcelable(LoggerKeys.DATA_SERVICE_LOG_LIST_PARCELABLE, ServiceLogList.class);
        if (errorLogList == null) return;
        List<ServiceLog> serviceLogs = errorLogList.serviceLogList;

        base.removeAllChildren();
        for (ServiceLog serviceLog : serviceLogs) {
            base.addItem(new ServiceLogItem(serviceLog, v -> deleteLog(serviceLog)));
        }
        adapter.notifyDataSetChanged();
    }

}
