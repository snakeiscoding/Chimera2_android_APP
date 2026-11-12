package com.jht.chimera.lib.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Set;

public class UIHelper
{

	public enum DIRECTION_TYPE {
		LEFT, TOP, RIGHT, BOTTOM, ALL
	}

	public enum VIEW_SHOW_TYPE {
		SHOW, HIDE, TOGGLE
	}

	public static void dumpIntent(String LOG_TAG, Intent i) {
		Bundle bundle = i.getExtras();
		if (bundle != null) {
			Set<String> keys = bundle.keySet();
			Iterator<String> it = keys.iterator();
			Log.e(LOG_TAG, "Dumping Intent start");
			while (it.hasNext()) {
				String key = it.next();
				Log.e(LOG_TAG, "[" + key + "=" + bundle.get(key) + "]");
			}
			Log.e(LOG_TAG, "Dumping Intent end");
		}
	}

	/*
	 * A thread safe way to set text in textview/button
	 */
	public static void setText(final View v, final String text) {
		v.post(() -> {
			if (v instanceof Button) {
				((Button) v).setText(text);
			} else if (v instanceof TextView) {
				((TextView) v).setText(text);
			}
		});
	}

	public static ViewGroup getContentView(Activity activity) {
		return (ViewGroup) activity.findViewById(android.R.id.content);
	}

	public static void showDialog(Activity activity, View v) {
		//getContentView(activity).addView(v);
		// **** For EWM-596 MachineSetup.apk wifi_pw_next_apkStop *************
		ViewGroup vg = getContentView(activity);
		if (null != vg) {
			for (int i=0;i<=vg.getChildCount();++i) {
				if (v == vg.getChildAt(i)){
					vg.removeView(v);
				}
			}
		}
		vg.addView(v);
	}

	/*
	 * A thread safe way to show/hide a view
	 */
	public static void showView(final View v, final boolean show) {
		v.post(() -> v.setVisibility(show ? View.VISIBLE : View.GONE));
	}

	/*
	 * A thread safe method to enable/disable a view
	 */
	public static void enableView(final View v, final boolean enable) {
		v.post(() -> v.setEnabled(enable));
	}

	/*
	 * A thread safe method to show or hide a view
	 */
	public static void showHideView(final View v, final VIEW_SHOW_TYPE type) {
		v.post(() -> {
			boolean visible = false;
			switch (type) {
			case TOGGLE:
				visible = (v.getVisibility() == View.GONE);
				break;
			case SHOW:
				visible = true;
				break;
			case HIDE:
				visible = false;
				break;
			default:
				break;
			}

			v.setVisibility(visible ? View.VISIBLE : View.GONE);
		});
	}

	/*
	 * Helper method to set padding to a view
	 */
	public static void setPadding(View v, DIRECTION_TYPE direction, int padding) {
		switch (direction) {
		case LEFT:
			v.setPadding(padding, v.getPaddingTop(), v.getPaddingRight(),
					v.getPaddingBottom());
			break;
		case RIGHT:
			v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), padding,
					v.getPaddingBottom());
			break;
		case TOP:
			v.setPadding(v.getPaddingLeft(), padding, v.getPaddingRight(),
					v.getPaddingBottom());
			break;
		case BOTTOM:
			v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
					v.getPaddingRight(), padding);
			break;
		case ALL:
			v.setPadding(padding, padding, padding, padding);
			break;
		default:
			break;
		}
	}

	/*
	 * A helper method to set margin to a view
	 */
	// public static void setMargin(View v, DIRECTION_TYPE direction, int
	// margin) {
	// if (v.getLayoutParams() instanceof MarginLayoutParams) {
	// setMargin((MarginLayoutParams) v.getLayoutParams(), direction,
	// margin);
	// return;
	// }
	// }

	public static void setMargin(View v, DIRECTION_TYPE direction, int margin) {

		if (!(v.getLayoutParams() instanceof MarginLayoutParams))
			return;

		MarginLayoutParams marginLayoutParams = (MarginLayoutParams) v
				.getLayoutParams();

		switch (direction) {
		case LEFT:
			marginLayoutParams.setMargins(margin, marginLayoutParams.topMargin,
					marginLayoutParams.rightMargin,
					marginLayoutParams.bottomMargin);
			break;
		case RIGHT:
			marginLayoutParams.setMargins(marginLayoutParams.leftMargin,
					marginLayoutParams.topMargin, margin,
					marginLayoutParams.bottomMargin);
			break;
		case TOP:
			marginLayoutParams.setMargins(marginLayoutParams.leftMargin,
					margin, marginLayoutParams.rightMargin,
					marginLayoutParams.bottomMargin);
			break;
		case BOTTOM:
			marginLayoutParams.setMargins(marginLayoutParams.leftMargin,
					marginLayoutParams.topMargin,
					marginLayoutParams.rightMargin, margin);
			break;
		case ALL:
			marginLayoutParams.setMargins(margin, margin, margin, margin);
			break;
		default:
			break;
		}
	}

	/*
	 * A helper method to set the specified size to the provided view
	 */
	public static void setSize(View v, int width, int height) {
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.width = width;
		lp.height = height;
	}
}
