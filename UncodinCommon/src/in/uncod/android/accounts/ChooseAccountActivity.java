package in.uncod.android.accounts;

import in.uncod.android.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListActivity;

public class ChooseAccountActivity extends SherlockListActivity {
    public static final String EXTRA_ACCOUNT_TYPE = "accountType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.choose_account_listview);

        // Load the requested account type (if specified)
        String type = getIntent().getStringExtra(EXTRA_ACCOUNT_TYPE);

        Account[] accounts = AccountManager.get(this).getAccountsByType(type);

        if (accounts.length > 0) {
            // Populate the list
            final ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this,
                    android.R.layout.simple_list_item_1, accounts) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView textView = (TextView) super.getView(position, convertView, parent);

                    textView.setText(getItem(position).name);

                    return textView;
                }
            };

            setListAdapter(adapter);

            getListView().setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    // Return the selected account
                    Account account = adapter.getItem(arg2);

                    Intent data = new Intent();
                    data.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    data.putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type);

                    setResult(RESULT_OK, data);
                    finish();
                }
            });
        }
        else {
            // No accounts to choose from
            setResult(RESULT_OK, new Intent());
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Cancel with an empty data intent
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }
}
