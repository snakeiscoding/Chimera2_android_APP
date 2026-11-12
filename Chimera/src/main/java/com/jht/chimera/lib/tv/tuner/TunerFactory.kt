package com.jht.chimera.lib.tv.tuner

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500Tuner
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500Variant
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETuner
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EVariant
import com.jht.chimera.mock.MockSerialPort
import com.jht.datamatrixlib.DataMatrix
import com.jht.serialport.peripheral.tuner.ITuner
import com.jht.serialport.peripheral.tuner.cab.CabCommand
import com.jht.serialport.peripheral.tuner.cab.CabRemoteButtonMap
import com.jht.serialport.peripheral.tuner.channel.IChannel

object TunerFactory {
    fun getTuner(context: Context, source: SettingKeys.TVSource): IChimeraTuner<out IChannel>? {
        if (DataMatrix.getBoolean(TVKeys.DATA_USE_TUNER_EMULATOR_BOOL)) {
            return when (source) {
                SettingKeys.TVSource.US4 -> {
                    US4Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_US4_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_US4_TUNER_HOST_OUTPUT_STREAM)
                    )
                }
                SettingKeys.TVSource.US5 -> {
                    US5Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_US6_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_US6_TUNER_HOST_OUTPUT_STREAM)
                    )
                }
                SettingKeys.TVSource.US6 -> {
                    US6Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_US6_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_US6_TUNER_HOST_OUTPUT_STREAM)
                    )
                }
                SettingKeys.TVSource.US7 -> {
                    US7Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_US7_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_US7_TUNER_HOST_OUTPUT_STREAM)
                    )
                }
                SettingKeys.TVSource.US8 -> {
                    LG6500Tuner(
                        context,
                        MockSerialPort(
                            TVKeys.EVENT_US8_TUNER_HOST_INPUT_STREAM,
                            TVKeys.EVENT_US8_TUNER_HOST_OUTPUT_STREAM
                        ),
                        LG6500Variant.US8
                    )
                }
                SettingKeys.TVSource.US9 -> Lg750ETuner(context, Lg750EVariant.NorthAmerica)
                SettingKeys.TVSource.KM5 -> {
                    KM5Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_KM5_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_KM5_TUNER_HOST_OUTPUT_STREAM)
                    )
                }
                SettingKeys.TVSource.KM6 -> {
                    LG6500Tuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_KM6_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_KM6_TUNER_HOST_OUTPUT_STREAM),
                        LG6500Variant.KM6
                    )
                }
                SettingKeys.TVSource.KM7 -> Lg750ETuner(context, Lg750EVariant.Europe)
                SettingKeys.TVSource.BR3 -> Lg750ETuner(context, Lg750EVariant.SouthAmerica)
                SettingKeys.TVSource.JP1 -> Lg750ETuner(context, Lg750EVariant.Japan)
                SettingKeys.TVSource.CAB -> {
                    CABTuner(
                        context,
                        MockSerialPort(TVKeys.EVENT_CAB_TUNER_HOST_INPUT_STREAM, TVKeys.EVENT_CAB_TUNER_HOST_OUTPUT_STREAM),
                        CabRemoteButtonMap(overrides = mapOf(
                            ITuner.REMOTE_BUTTON.ENTER to {
                                CabCommand.EnterCommand.fromDisplayName(
                                    displayName = DataMatrix.getString(SettingKeys.TV.DATA_CAB_ENTER_COMMAND_STRING)
                                ) ?: CabCommand.EnterCommand.EnterTV
                            }
                        ))
                    )
                }
                else -> null
            }
        } else {
            return when (source) {
                SettingKeys.TVSource.US4 -> US4Tuner(context)
                SettingKeys.TVSource.US5 -> US5Tuner(context)
                SettingKeys.TVSource.US6 -> US6Tuner(context)
                SettingKeys.TVSource.US7 -> US7Tuner(context)
                SettingKeys.TVSource.US8 -> LG6500Tuner(context, LG6500Variant.US8)
                SettingKeys.TVSource.US9 -> Lg750ETuner(context, Lg750EVariant.NorthAmerica)
                SettingKeys.TVSource.KM5 -> KM5Tuner(context)
                SettingKeys.TVSource.KM6 -> LG6500Tuner(context, LG6500Variant.KM6)
                SettingKeys.TVSource.KM7 -> Lg750ETuner(context, Lg750EVariant.Europe)
                SettingKeys.TVSource.BR3 -> Lg750ETuner(context, Lg750EVariant.SouthAmerica)
                SettingKeys.TVSource.JP1 -> Lg750ETuner(context, Lg750EVariant.Japan)
                SettingKeys.TVSource.CAB -> { CABTuner.create(
                    context,
                    CabRemoteButtonMap(overrides = mapOf(
                        ITuner.REMOTE_BUTTON.ENTER to {
                            CabCommand.EnterCommand.fromDisplayName(
                                displayName = DataMatrix.getString(SettingKeys.TV.DATA_CAB_ENTER_COMMAND_STRING)
                            ) ?: CabCommand.EnterCommand.EnterTV
                        }
                    ))
                )}
                else -> null
            }
        }
    }
}