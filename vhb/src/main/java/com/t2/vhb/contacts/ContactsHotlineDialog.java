
package com.t2.vhb.contacts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.t2.vhb.R;

import java.util.HashMap;
import java.util.Map;

public class ContactsHotlineDialog extends DialogFragment {

    private static final String[] HOTLINE_NAMES = new String[] {
            "911", "Veteran's Crisis Line (En)", "Veteran's Crisis Line (Sp)", "DCoE Outreach Center"
    };

    private static final String[] HOTLINE_DESCRIPTION = new String[] {
            "9. 1. 1.", "Veteran's Crisis Line: English", "Veteran's Crisis Line: Spanish", "Deecoe Outreach Center"
    };

    private static final String[] HOTLINE_NUMBERS = new String[] {
            "tel:911", "tel:1-800-273-8255", "tel:1-888-628-9454", "tel:1-866-966-1020"
    };

    public static ContactsHotlineDialog newInstance() {
        return new ContactsHotlineDialog();
    }

    private HotlineAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new HotlineAdapter();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity()).setTitle("Emergency hotlines")
                .setIcon(R.drawable.ic_dialog_alert).setAdapter(mAdapter, (dialog, which) -> {
                    final Map<String, String> data = new HashMap<>();
                    data.put("Hotline", HOTLINE_NAMES[which]);
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(HOTLINE_NUMBERS[which]));
                    startActivity(intent);
                }).create();
    }

    private final class HotlineAdapter extends ArrayAdapter<String> {
        public HotlineAdapter() {
            super(getActivity(), android.R.layout.simple_list_item_1, HOTLINE_NAMES);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v = super.getView(position, convertView, parent);
            v.setContentDescription(HOTLINE_DESCRIPTION[position]);
            return v;
        }
    }

}
