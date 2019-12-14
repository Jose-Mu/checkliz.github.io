package ve.com.abicelis.Checkliz.enums;

import android.content.Context;
import android.support.annotation.StringRes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ve.com.abicelis.Checkliz.R;


public enum TaskStatus implements Serializable {
    UNPROGRAMMED(R.string.task_status_unprogrammed),
    PROGRAMMED(R.string.task_status_programmed),
    DONE(R.string.task_status_done);

    private @StringRes
    int friendlyNameRes;

    TaskStatus(@StringRes int friendlyNameRes) {
        this.friendlyNameRes = friendlyNameRes;

    }

    public static List<String> getFriendlyValues(Context context) {
        List<String> friendlyValues = new ArrayList<>();
        for (TaskStatus ts : values()) {
            friendlyValues.add(context.getResources().getString(ts.friendlyNameRes));
        }
        return friendlyValues;
    }

}
