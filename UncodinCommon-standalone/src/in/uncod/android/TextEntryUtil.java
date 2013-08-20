package in.uncod.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.content.Context;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.widget.EditText;

/**
 * Created by IntelliJ IDEA. User: ddrboxman Date: 10/11/12 Time: 2:02 PM
 */
public class TextEntryUtil {

    public static List<String> getAccounts(Context context) {
        Set<String> foundAccounts = new HashSet<String>();
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                String possibleEmail = account.name;
                foundAccounts.add(possibleEmail);
            }
        }
        return new ArrayList<String>(foundAccounts);
    }

    public static boolean isValid(EditText editText) {
        boolean isValid = Patterns.EMAIL_ADDRESS.matcher(editText.getText()).matches();
        if (!isValid) {
            editText.setError("Invalid email address");
        }
        return isValid;
    }

    public static void setErrorPopupRemoverTextWatcher(EditText editText) {
            editText.addTextChangedListener(new ErrorPopupRemoverTextWatcher(editText));
    }

    public static class ErrorPopupRemoverTextWatcher implements TextWatcher {

        EditText editText;

        public ErrorPopupRemoverTextWatcher(EditText editText) {
            this.editText = editText;
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s != null && s.length() > 0 && editText.getError() != null) {
                editText.setError(null);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void afterTextChanged(Editable s) {
        }
    };
}
