package com.alatech.cs012;

import no.nordicsemi.android.dfu.DfuBaseService;
import android.app.Activity;

public class DfuService extends DfuBaseService {

	@Override
	protected Class<? extends Activity> getNotificationTarget() {
		// TODO Auto-generated method stub
		return NotificationActivity.class;
	}
}
