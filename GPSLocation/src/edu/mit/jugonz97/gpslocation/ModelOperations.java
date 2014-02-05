package edu.mit.jugonz97.gpslocation;

import java.util.ArrayList;

import android.widget.ArrayAdapter;

public class ModelOperations {
	private static final int arrayAdapterMaxSize = 5;
	
	public static void updateList(ArrayAdapter<String> adapter,
			String strToInsert) {

		if (adapter == null || strToInsert == null) return; // Take care of nulls
		
		// Get old items
		ArrayList<String> savedItems = new ArrayList<String>();
		int itemIterator = 0;
		// Only get items that exist, and only get up to 4 of them
		while (itemIterator < adapter.getCount() && itemIterator < arrayAdapterMaxSize -1) {
			savedItems.add(adapter.getItem(itemIterator));
			++itemIterator;
		}

		// Clear the array
		adapter.clear();
		
		// Now put them into our new array
		adapter.insert(strToInsert, 0);
		// Put saved items into the array
		for (int i=0; i < savedItems.size(); ++i) {
			adapter.insert(savedItems.get(i), i+1);
		}
		
		// Our data has been changed! Notify listeners.
		adapter.notifyDataSetChanged();
	}
}
