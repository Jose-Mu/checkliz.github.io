package ve.com.abicelis.Checkliz.model.reminder;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;

import ve.com.abicelis.Checkliz.enums.ReminderType;
import ve.com.abicelis.Checkliz.model.Time;

public class OneTimeReminder extends Reminder implements Serializable {

    private Calendar date;
    private Time time;

    public OneTimeReminder() {} //Parameter-less constructor used when creating reminder

    public OneTimeReminder(@NonNull Calendar date, @NonNull Time time) {
        init(date, time);
    }

    public OneTimeReminder(int id, int taskId, @NonNull Calendar date, @NonNull Time time) {
        super(id, taskId);
        init(date, time);
    }

    private void init(@NonNull Calendar date, @NonNull Time time) {
        this.date = date;
        this.time = time;
    }


    @Override
    public ReminderType getType() {
        return ReminderType.ONE_TIME;
    }

    public Calendar getDate() {
        return date;
    }
    public void setDate(Calendar date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }
    public void setTime(Time time) {
        this.time = time;
    }


    @Override
    public String toString() {
        String                      res = "Reminder ID=" + getId() + "\r\n";
                                    res += " Type=" + getType().name() + "\r\n";
                                    res += " TaskID=" + getTaskId() + "\r\n";
                                    res +=  " Date=" + date.toString() + "\r\n";
                                    res +=  " Time=" + time.toString();
        return res;
    }
}
