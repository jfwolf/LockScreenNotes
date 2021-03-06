package de.nilsfo.lockscreennotes.data;

import android.content.Context;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import de.nilsfo.lockscreennotes.util.TimeUtils;
import de.nilsfo.lsn.R;
import timber.log.Timber;

/**
 * Created by Nils on 21.02.2017.
 */

public class RelativeTimeTextfieldContainer {

	private static RelativeTimeTextfieldContainer container;
	private ArrayList<TextView> viewList;
	private LinkedList<TextView> deleteList;
	private HashMap<TextView, Long> timestampMap;

	private RelativeTimeTextfieldContainer() {
		viewList = new ArrayList<TextView>();
		timestampMap = new HashMap<>();
		deleteList = new LinkedList<TextView>();
	}

	public static RelativeTimeTextfieldContainer getContainer() {
		if (container == null) container = new RelativeTimeTextfieldContainer();
		return container;
	}

	public void clear() {
		viewList.clear();
	}

	public void add(TextView view, long timestamp) {
		viewList.add(view);
		timestampMap.put(view, timestamp);
	}

	public void remove(TextView view) {
		viewList.remove(view);
		timestampMap.remove(view);
	}

	public void updateText(Context context) {
		while (!deleteList.isEmpty()) {
			remove(deleteList.pop());
		}

		for (TextView v : viewList) {
			long timestamp = timestampMap.get(v);
			if (v.isShown()) {
				v.setText(context.getString(R.string.last_edited, new TimeUtils(context).formatRelative(timestamp)));
			} else deleteList.add(v);
		}
		Timber.v("Updating displayed note times. Updated count: " + viewList.size() + " To be deleted: " + deleteList.size());
	}
}
