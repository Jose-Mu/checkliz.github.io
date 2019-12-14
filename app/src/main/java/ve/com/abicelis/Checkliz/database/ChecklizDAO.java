package ve.com.abicelis.Checkliz.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.google.android.gms.location.Geofence;

import java.io.InvalidClassException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import ve.com.abicelis.Checkliz.enums.AttachmentType;
import ve.com.abicelis.Checkliz.enums.TaskCategory;
import ve.com.abicelis.Checkliz.enums.ReminderRepeatEndType;
import ve.com.abicelis.Checkliz.enums.ReminderRepeatType;
import ve.com.abicelis.Checkliz.enums.TaskSortType;
import ve.com.abicelis.Checkliz.enums.TaskStatus;
import ve.com.abicelis.Checkliz.enums.ReminderType;
import ve.com.abicelis.Checkliz.enums.TaskViewModelType;
import ve.com.abicelis.Checkliz.exception.CouldNotDeleteDataException;
import ve.com.abicelis.Checkliz.exception.CouldNotGetDataException;
import ve.com.abicelis.Checkliz.exception.CouldNotInsertDataException;
import ve.com.abicelis.Checkliz.exception.CouldNotUpdateDataException;
import ve.com.abicelis.Checkliz.exception.PlaceNotFoundException;
import ve.com.abicelis.Checkliz.model.UnprogrammedTasksByTitleComparator;
import ve.com.abicelis.Checkliz.model.attachment.Attachment;
import ve.com.abicelis.Checkliz.model.attachment.AudioAttachment;
import ve.com.abicelis.Checkliz.model.attachment.ImageAttachment;
import ve.com.abicelis.Checkliz.model.attachment.LinkAttachment;
import ve.com.abicelis.Checkliz.model.attachment.ListAttachment;
import ve.com.abicelis.Checkliz.model.reminder.LocationBasedReminder;
import ve.com.abicelis.Checkliz.model.reminder.OneTimeReminder;
import ve.com.abicelis.Checkliz.model.reminder.Reminder;
import ve.com.abicelis.Checkliz.model.reminder.RepeatingReminder;
import ve.com.abicelis.Checkliz.model.Place;
import ve.com.abicelis.Checkliz.model.Task;
import ve.com.abicelis.Checkliz.model.attachment.TextAttachment;
import ve.com.abicelis.Checkliz.model.Time;
import ve.com.abicelis.Checkliz.util.CalendarUtil;
import ve.com.abicelis.Checkliz.util.SharedPreferenceUtil;
import ve.com.abicelis.Checkliz.util.TaskUtil;
import ve.com.abicelis.Checkliz.util.sorting.TaskSortingUtil;
import ve.com.abicelis.Checkliz.viewmodel.TaskTriggerViewModel;
import ve.com.abicelis.Checkliz.viewmodel.TaskViewModel;

public class ChecklizDAO {

    private ChecklizDbHelper mDatabaseHelper;
    private Context mContext;

    public ChecklizDAO(Context context) {
        mDatabaseHelper = new ChecklizDbHelper(context);
        mContext = context;
    }



    public List<TaskViewModel> getUnprogrammedTasks() throws CouldNotGetDataException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        List<TaskViewModel> result = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();

        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME,
                null, ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName() + "=?",
                new String[]{TaskStatus.UNPROGRAMMED.name()}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                Task current = getTaskFromCursor(cursor);


                current.setAttachments(getAttachmentsOfTask(current.getId()));


                if(current.getReminderType() != ReminderType.NONE)
                    throw new CouldNotGetDataException("Error, found task with TaskStatus=UNPROGRAMMED with ReminderType != NONE");

                tasks.add(current);
            }
        } finally {
            cursor.close();
        }


        Collections.sort(tasks, new UnprogrammedTasksByTitleComparator());

        for (int i = 0; i < tasks.size(); i++) {
            result.add(new TaskViewModel(tasks.get(i), TaskViewModelType.UNPROGRAMMED_REMINDER));
        }

        return result;
    }

    public List<TaskViewModel> getLocationBasedTasks(Resources resources) throws CouldNotGetDataException, InvalidClassException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        List<TaskViewModel> result;
        List<Task> tasks = new ArrayList<>();

        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME,
                null, ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName() + "=?",
                new String[]{TaskStatus.PROGRAMMED.name()}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                Task current = getTaskFromCursor(cursor);

                if(current.getReminderType() != ReminderType.LOCATION_BASED)
                    continue;


                current.setAttachments(getAttachmentsOfTask(current.getId()));


                if(current.getReminderType() == ReminderType.NONE)
                    throw new CouldNotGetDataException("Error, Task with TaskStatus=PROGRAMMED has ReminderType=NONE");
                else
                    current.setReminder(getReminderOfTask(current.getId(), current.getReminderType()));

                tasks.add(current);
            }

        } finally {
            cursor.close();
        }


        result = new TaskSortingUtil().generateProgrammedTaskHeaderList(tasks, TaskSortType.PLACE, resources);

        return result;
    }

    public List<TaskViewModel> getProgrammedTasks(@NonNull TaskSortType sortType, boolean includeLocationBasedTasks, Resources resources) throws CouldNotGetDataException, InvalidClassException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        List<TaskViewModel> result;
        List<Task> tasks = new ArrayList<>();

        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME,
                null, ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName() + "=?",
                new String[]{TaskStatus.PROGRAMMED.name()}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                Task current = getTaskFromCursor(cursor);

                if(!includeLocationBasedTasks && current.getReminderType() == ReminderType.LOCATION_BASED)
                    continue;


                current.setAttachments(getAttachmentsOfTask(current.getId()));


                if(current.getReminderType() == ReminderType.NONE)
                    throw new CouldNotGetDataException("Error, Task with TaskStatus=PROGRAMMED has ReminderType=NONE");
                else
                    current.setReminder(getReminderOfTask(current.getId(), current.getReminderType()));

                tasks.add(current);
            }

        } finally {
            cursor.close();
        }


        result = new TaskSortingUtil().generateProgrammedTaskHeaderList(tasks, sortType, resources);

        return result;
    }

    public List<TaskViewModel> getDoneTasks(@NonNull TaskSortType sortType, Resources resources) throws CouldNotGetDataException, InvalidClassException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        List<TaskViewModel> result = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();

        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME,
                null, ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName() + "=?",
                new String[]{TaskStatus.DONE.name()}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                Task current = getTaskFromCursor(cursor);

                current.setAttachments(getAttachmentsOfTask(current.getId()));

                if(current.getReminderType() != ReminderType.NONE)
                    current.setReminder(getReminderOfTask(current.getId(), current.getReminderType()));

                tasks.add(current);
            }

        } finally {
            cursor.close();
        }


        result = new TaskSortingUtil().generateDoneTaskHeaderList(tasks, sortType, resources);

        return result;
    }

    public List<Task> getLocationBasedTasksAssociatedWithPlace(int placeId, int geofenceTransition) throws CouldNotGetDataException {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = null;

        switch (geofenceTransition) {
            case -1: //Any
                cursor = db.query(ChecklizContract.LocationBasedReminderTable.TABLE_NAME, null,
                        ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_PLACE_FK.getName() + "=?",
                        new String[] {String.valueOf(placeId)}, null, null, null);
                break;

            case Geofence.GEOFENCE_TRANSITION_DWELL:
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                cursor = db.query(ChecklizContract.LocationBasedReminderTable.TABLE_NAME, null,
                        ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_PLACE_FK.getName() + "=? AND " +
                        ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_ENTERING.getName() + "=?",
                        new String[] {String.valueOf(placeId), "true"}, null, null, null);
                break;


            case Geofence.GEOFENCE_TRANSITION_EXIT:
                cursor = db.query(ChecklizContract.LocationBasedReminderTable.TABLE_NAME, null,
                        ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_PLACE_FK.getName() + "=? AND " +
                                ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_EXITING.getName() + "=?",
                        new String[] {String.valueOf(placeId), "true"}, null, null, null);
                break;
        }

        if(cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    int taskId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TASK_FK.getName()));
                    Task task = getTask(taskId);
                    if(task.getStatus().equals(TaskStatus.PROGRAMMED))
                        tasks.add(task);
                }
            } finally {
                cursor.close();
            }
        }


        return tasks;
    }

    public TaskTriggerViewModel getNextTaskToTrigger(@NonNull List<Integer> alreadyTriggeredTaskList) throws CouldNotGetDataException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Task nextTaskToTrigger = null;
        Calendar triggerDate = null;
        Time triggerTime = null;

        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME,
                null, ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName() + "=?",
                new String[]{TaskStatus.PROGRAMMED.name()}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                Task current = getTaskFromCursor(cursor);

                if(alreadyTriggeredTaskList.contains(current.getId()))   //Skip
                    continue;

                try {
                    current.setReminder(getReminderOfTask(current.getId(), current.getReminderType()));
                }catch (CouldNotGetDataException | SQLiteConstraintException e ) {
                    throw new CouldNotGetDataException("Error fetching reminder for task ID" + current.getId(), e);
                }

                //TODO: this filter could be made on db query.
                if( current.getReminderType().equals(ReminderType.ONE_TIME) ||  current.getReminderType().equals(ReminderType.REPEATING) ) {

                    if(TaskUtil.checkIfOverdue(current.getReminder()))  //Skip overdue reminders
                        continue;

                    if(nextTaskToTrigger == null) {
                        nextTaskToTrigger = current;
                        triggerDate = (current.getReminderType().equals(ReminderType.ONE_TIME) ? ((OneTimeReminder)current.getReminder() ).getDate() : TaskUtil.getRepeatingReminderNextCalendar( (RepeatingReminder)current.getReminder()) );
                        triggerTime = (current.getReminderType().equals(ReminderType.ONE_TIME) ? ((OneTimeReminder)current.getReminder() ).getTime() : ((RepeatingReminder)current.getReminder()).getTime() );
                        continue;
                    }

                    if(current.getReminderType().equals(ReminderType.ONE_TIME)) {
                        OneTimeReminder otr = (OneTimeReminder)current.getReminder();
                        Calendar currentDate = CalendarUtil.getCalendarFromDateAndTime(otr.getDate(), otr.getTime());

                        if(currentDate.compareTo(triggerDate) < 0 ) {
                            nextTaskToTrigger = current;
                            triggerDate = currentDate;
                            triggerTime = otr.getTime();
                            continue;
                        }
                    }

                    if(current.getReminderType().equals(ReminderType.REPEATING)) {
                        RepeatingReminder rr = (RepeatingReminder)current.getReminder();
                        Calendar currentDate = TaskUtil.getRepeatingReminderNextCalendar(rr);

                        if(currentDate == null) continue;   //Overdue

                        if(currentDate.compareTo(triggerDate) < 0 ) {
                            nextTaskToTrigger = current;
                            triggerDate = currentDate;
                            triggerTime = rr.getTime();
                            continue;
                        }
                    }

                }
            }
        } finally {
            cursor.close();
        }

        if(nextTaskToTrigger == null) return null;
        return new TaskTriggerViewModel(nextTaskToTrigger, triggerDate, triggerTime);
    }


    public Task getTask(int taskId) throws CouldNotGetDataException, SQLiteConstraintException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(ChecklizContract.TaskTable.TABLE_NAME, null, ChecklizContract.PlaceTable._ID + "=?",
                new String[]{String.valueOf(taskId)}, null, null, null);

        if (cursor.getCount() == 0)
            throw new CouldNotGetDataException("Specified Task not found in the database. Passed id=" + taskId);
        if (cursor.getCount() > 1)
            throw new SQLiteConstraintException("Database UNIQUE constraint failure, more than one record found. Passed value=" + taskId);

        cursor.moveToNext();
        Task task = getTaskFromCursor(cursor);
        task.setAttachments(getAttachmentsOfTask(taskId));

        if(task.getReminderType() != ReminderType.NONE)
            task.setReminder(getReminderOfTask(taskId, task.getReminderType()));
        return task;
    }

    public List<Place> getPlaces() {
        List<Place> places = new ArrayList<>();

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(ChecklizContract.PlaceTable.TABLE_NAME, null, null, null, null, null, null);

        try {
            while (cursor.moveToNext()) {
                places.add(getPlaceFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }

        return places;
    }

    public List<Place> getActivePlaces() {
        List<Place> places = new ArrayList<>();
        int taskCount;
        Place place;

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(ChecklizContract.PlaceTable.TABLE_NAME, null, null, null, null, null, null);

        try {
            while (cursor.moveToNext()) {
                place = getPlaceFromCursor(cursor);

                try {
                    taskCount = getLocationBasedTasksAssociatedWithPlace(place.getId(), -1).size();
                } catch (CouldNotGetDataException e) {
                    taskCount = 0;
                }

                if(taskCount > 0)
                    places.add(place);
            }
        } finally {
            cursor.close();
        }

        return places;
    }

    public Place getPlace(int placeId) throws PlaceNotFoundException, SQLiteConstraintException {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(ChecklizContract.PlaceTable.TABLE_NAME, null, ChecklizContract.PlaceTable._ID + "=?",
                new String[]{String.valueOf(placeId)}, null, null, null);

        if (cursor.getCount() == 0)
            throw new PlaceNotFoundException("Specified Place not found in the database. Passed id=" + placeId);
        if (cursor.getCount() > 1)
            throw new SQLiteConstraintException("Database UNIQUE constraint failure, more than one record found. Passed value=" + placeId);

        cursor.moveToNext();
        return getPlaceFromCursor(cursor);
    }

    public ArrayList<Attachment> getAttachmentsOfTask(int taskId) {
        ArrayList<Attachment> attachments = new ArrayList<>();
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(ChecklizContract.AttachmentTable.TABLE_NAME, null,
                ChecklizContract.AttachmentTable.COLUMN_NAME_TASK_FK.getName() + "=?",
                        new String[]{String.valueOf(taskId)}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                attachments.add(getAttachmentFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }

        return attachments;
    }

    public Reminder getReminderOfTask(int taskId, @NonNull ReminderType reminderType) throws  CouldNotGetDataException, SQLiteConstraintException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        Reminder reminder;

        String tableName, whereClause;
        switch (reminderType) {
            case ONE_TIME:
                whereClause = ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
                tableName = ChecklizContract.OneTimeReminderTable.TABLE_NAME;
                break;
            case REPEATING:
                whereClause = ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
                tableName = ChecklizContract.RepeatingReminderTable.TABLE_NAME;
                break;
            case LOCATION_BASED:
                whereClause = ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
                tableName = ChecklizContract.LocationBasedReminderTable.TABLE_NAME;
                break;
            default:
                throw new CouldNotGetDataException("ReminderType is invalid. Type=" + reminderType);
        }

        Cursor cursor = db.query(tableName, null, whereClause, new String[]{String.valueOf(taskId)}, null, null, null);
        try {
            if (cursor.getCount() == 0)
                throw new CouldNotGetDataException("Specified Reminder not found in the database. Passed id=" + taskId);
            if (cursor.getCount() > 1)
                throw new SQLiteConstraintException("Database UNIQUE constraint failure, more than one Reminder found. Passed id=" + taskId);

            cursor.moveToNext();
            switch (reminderType) {
                case ONE_TIME:
                    reminder = getOneTimeReminderFromCursor(cursor);
                    break;
                case REPEATING:
                    reminder = getRepeatingReminderFromCursor(cursor);
                    break;
                case LOCATION_BASED:
                    reminder = getLocationBasedReminderFromCursor(cursor);
                    int placeId = ((LocationBasedReminder)reminder).getPlaceId();
                    try {
                        ((LocationBasedReminder)reminder).setPlace(getPlace(placeId));
                    } catch (PlaceNotFoundException | SQLiteConstraintException e) {
                        throw new CouldNotGetDataException("Error trying to get Place for Location-based Reminder", e);
                    }

                    break;
                default:
                    throw new CouldNotGetDataException("ReminderType is invalid. Type=" + reminderType);
            }
        } finally {
            cursor.close();
        }

        return reminder;
    }


    public boolean deletePlace(int placeId) throws CouldNotDeleteDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        List<Task> tasks;
        try {
            tasks = getLocationBasedTasksAssociatedWithPlace(placeId, -1);
        }catch (CouldNotGetDataException e) {
            throw new CouldNotDeleteDataException("Error getting Task list associated with Place. PlaceID=" + placeId, e);
        }

        if(tasks.size() > 0) {
            for (Task task : tasks) {
                deleteReminderOfTask(task.getId());
                task.setStatus(TaskStatus.UNPROGRAMMED);
                task.setReminderType(ReminderType.NONE);
                try {
                    updateTask(task);
                } catch (CouldNotUpdateDataException e) {
                    throw new CouldNotDeleteDataException("Error updating RemidnerType of Task to NONE. TaskID=" + task.getId(), e);
                }
            }
        }

        return db.delete(ChecklizContract.PlaceTable.TABLE_NAME,
                ChecklizContract.PlaceTable._ID + " =?",
                new String[]{String.valueOf(placeId)}) > 0;
    }

    public boolean deleteAttachmentsOfTask(int taskId) throws CouldNotDeleteDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        return db.delete(ChecklizContract.AttachmentTable.TABLE_NAME,
                ChecklizContract.AttachmentTable.COLUMN_NAME_TASK_FK.getName() + " =?",
                new String[]{String.valueOf(taskId)}) > 0;
    }

    public boolean deleteAttachment(int attachmentId) throws CouldNotDeleteDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        return db.delete(ChecklizContract.AttachmentTable.TABLE_NAME,
                ChecklizContract.AttachmentTable._ID + " =?",
                new String[]{String.valueOf(attachmentId)}) > 0;
    }

    public void deleteReminderOfTask(int taskId) throws CouldNotDeleteDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        String tableName, whereClause;

        whereClause = ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
        tableName = ChecklizContract.OneTimeReminderTable.TABLE_NAME;
        db.delete(tableName, whereClause, new String[]{String.valueOf(taskId)});

        whereClause = ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
        tableName = ChecklizContract.RepeatingReminderTable.TABLE_NAME;
        db.delete(tableName, whereClause, new String[]{String.valueOf(taskId)});

        whereClause = ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TASK_FK.getName() + " =?";
        tableName = ChecklizContract.LocationBasedReminderTable.TABLE_NAME;
        db.delete(tableName, whereClause, new String[]{String.valueOf(taskId)});
    }

    public boolean deleteTask(int taskId) throws CouldNotDeleteDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        Task task;
        try {
            task = getTask(taskId);
        } catch (CouldNotGetDataException e) {
            throw new CouldNotDeleteDataException("Failed to get task from database. TaskID=" + taskId, e);
        }

        deleteAttachmentsOfTask(taskId);

        if(task.getReminderType() != ReminderType.NONE) {
            deleteReminderOfTask(taskId);
        }

        return db.delete(ChecklizContract.TaskTable.TABLE_NAME,
                ChecklizContract.TaskTable._ID + " =?",
                new String[]{String.valueOf(taskId)}) > 0;

    }


    public long updatePlace(Place place) throws CouldNotUpdateDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        int count = db.update(
                ChecklizContract.PlaceTable.TABLE_NAME,
                getValuesFromPlace(place),
                ChecklizContract.PlaceTable._ID + " =? ",
                new String[] {String.valueOf(place.getId())} );

        return count;
    }

    public long[] updateAttachmentsOfTask(Task task) throws CouldNotUpdateDataException {
        try {
            deleteAttachmentsOfTask(task.getId());
        } catch (CouldNotDeleteDataException e) {
            throw new CouldNotUpdateDataException("Could not delete attachments while updating.", e.getCause());
        }

        long[] insertedRowIds;
        try {
            insertedRowIds = insertAttachmentsOfTask(task.getId(), task.getAttachments());
        } catch (CouldNotInsertDataException e) {
            throw new CouldNotUpdateDataException("Could not insert attachments while updating.", e.getCause());
        }

        return insertedRowIds;
    }

    public long[] updateAttachments(ArrayList<Attachment> attachments) throws CouldNotUpdateDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long[] updatedRowIds = new long[attachments.size()];

        for (int i = 0; i < attachments.size(); i++)
            updatedRowIds[i] = updateAttachment(attachments.get(i));

        return updatedRowIds;
    }

    public long updateAttachment(Attachment attachment) throws CouldNotUpdateDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        long updatedRowIds = db.update(ChecklizContract.AttachmentTable.TABLE_NAME,
                    getValuesFromAttachment(attachment),
                    ChecklizContract.AttachmentTable._ID + " =? ",
                    new String[]{String.valueOf(attachment.getId())});

        return updatedRowIds;
    }

    public boolean updateReminderOfTask(Reminder reminder, int taskId) throws CouldNotUpdateDataException {

        try {
            deleteReminderOfTask(taskId);
        }catch (CouldNotDeleteDataException e) {
            throw new CouldNotUpdateDataException("Error while deleting old reminder in dao.updateReminder(). Reminder=" + reminder.toString(), e);
        }

        if(reminder != null) {
            try {
                insertReminderOfTask(taskId, reminder);
            }catch (CouldNotInsertDataException e) {
                throw new CouldNotUpdateDataException("Error while inserting new reminder in dao.updateReminder(). Reminder=" + reminder.toString(), e);
            }
        }

        return true;
    }

    public long updateTask(Task task) throws CouldNotUpdateDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        updateAttachmentsOfTask(task);

        updateReminderOfTask(task.getReminder(), task.getId());

        return db.update(
                ChecklizContract.TaskTable.TABLE_NAME,
                getValuesFromTask(task),
                ChecklizContract.TaskTable._ID + " =? ",
                new String[] {String.valueOf(task.getId())} );
    }


    public long insertPlace(Place place) throws CouldNotInsertDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        ContentValues values = getValuesFromPlace(place);

        long newRowId;
        newRowId = db.insert(ChecklizContract.PlaceTable.TABLE_NAME, null, values);

        if (newRowId == -1)
            throw new CouldNotInsertDataException("There was a problem inserting the Place: " + place.toString());

        return newRowId;
    }

    public long[] insertAttachmentsOfTask(int taskId, List<Attachment> attachments) throws CouldNotInsertDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long[] newRowIds = new long[attachments.size()];

        for (int i = 0; i < attachments.size(); i++) {

            Attachment attachment = attachments.get(i);
            attachment.setTaskId(taskId);
            ContentValues values = getValuesFromAttachment(attachment);

            newRowIds[i] = db.insert(ChecklizContract.AttachmentTable.TABLE_NAME, null, values);

            if (newRowIds[i] == -1)
                throw new CouldNotInsertDataException("There was a problem inserting the Attachment: " + attachments.toString());
        }

        return newRowIds;
    }

    public long insertReminderOfTask(int taskId, Reminder reminder) throws CouldNotInsertDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        ContentValues values;
        String tableName;

        reminder.setTaskId(taskId);

        switch (reminder.getType()) {
            case ONE_TIME:
                values = getValuesFromOneTimeReminder((OneTimeReminder) reminder);
                tableName = ChecklizContract.OneTimeReminderTable.TABLE_NAME;
                break;
            case REPEATING:
                values = getValuesFromRepeatingReminder((RepeatingReminder) reminder);
                tableName = ChecklizContract.RepeatingReminderTable.TABLE_NAME;
                break;
            case LOCATION_BASED:
                values = getValuesFromLocationBasedReminder((LocationBasedReminder) reminder);
                tableName = ChecklizContract.LocationBasedReminderTable.TABLE_NAME;
                break;
            default:
                throw new CouldNotInsertDataException("ReminderType is invalid. Type=" + reminder.getType());
        }

        long newRowId;
        newRowId = db.insert(tableName, null, values);

        if (newRowId == -1)
            throw new CouldNotInsertDataException("There was a problem inserting the Reminder: " + reminder.toString());

        return newRowId;
    }


    public long insertTask(Task task) throws CouldNotInsertDataException {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        ContentValues values = getValuesFromTask(task);

        long newRowId;
        newRowId = db.insert(ChecklizContract.TaskTable.TABLE_NAME, null, values);

        if (newRowId == -1)
            throw new CouldNotInsertDataException("There was a problem inserting the Task: " + task.toString());

        if (task.getAttachments() != null && task.getAttachments().size() > 0) {
            try {
                insertAttachmentsOfTask((int)newRowId, task.getAttachments());
            } catch (CouldNotInsertDataException e) {
                throw new CouldNotInsertDataException("There was a problem inserting the Attachments while inserting the Task: " + task.toString(), e);
            }
        }

        if (task.getReminder() != null) {
            try {
                insertReminderOfTask((int)newRowId, task.getReminder());
            } catch (CouldNotInsertDataException e) {
                throw new CouldNotInsertDataException("There was a problem inserting the Reminder while inserting the Task: " + task.toString(), e);
            }
        }

        return newRowId;
    }




    private ContentValues getValuesFromTask(Task task) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName(), task.getStatus().name());
        values.put(ChecklizContract.TaskTable.COLUMN_NAME_TITLE.getName(), task.getTitle());
        values.put(ChecklizContract.TaskTable.COLUMN_NAME_DESCRIPTION.getName(), task.getDescription());
        values.put(ChecklizContract.TaskTable.COLUMN_NAME_CATEGORY.getName(), task.getCategory().name());
        values.put(ChecklizContract.TaskTable.COLUMN_NAME_REMINDER_TYPE.getName(), task.getReminderType().name());

        values.put(ChecklizContract.TaskTable.COLUMN_NAME_DONE_DATE.getName(), (task.getStatus() == TaskStatus.DONE ? task.getDoneDate().getTimeInMillis() : -1));
        return values;
    }


    private ContentValues getValuesFromOneTimeReminder(OneTimeReminder oneTimeReminder) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TASK_FK.getName(), oneTimeReminder.getTaskId());
        values.put(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_DATE.getName(), oneTimeReminder.getDate().getTimeInMillis());
        values.put(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TIME.getName(), oneTimeReminder.getTime().getTimeInMinutes());
        return values;
    }


    private ContentValues getValuesFromRepeatingReminder(RepeatingReminder repeatingReminder) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TASK_FK.getName(), repeatingReminder.getTaskId());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_DATE.getName(), repeatingReminder.getDate().getTimeInMillis());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TIME.getName(), repeatingReminder.getTime().getTimeInMinutes());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_TYPE.getName(), repeatingReminder.getRepeatType().name());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_INTERVAL.getName(), repeatingReminder.getRepeatInterval());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_TYPE.getName(), repeatingReminder.getRepeatEndType().name());
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_NUMBER_OF_EVENTS.getName(), (repeatingReminder.getRepeatEndType() == ReminderRepeatEndType.FOR_X_EVENTS ? repeatingReminder.getRepeatEndNumberOfEvents() : -1));
        values.put(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_DATE.getName(), (repeatingReminder.getRepeatEndType() == ReminderRepeatEndType.UNTIL_DATE ? repeatingReminder.getRepeatEndDate().getTimeInMillis() : -1));
        return values;
    }


    private ContentValues getValuesFromLocationBasedReminder(LocationBasedReminder locationBasedReminder) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TASK_FK.getName(), locationBasedReminder.getTaskId());
        values.put(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_PLACE_FK.getName(), locationBasedReminder.getPlaceId());
        values.put(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_ENTERING.getName(), String.valueOf(locationBasedReminder.getTriggerEntering()));
        values.put(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_EXITING.getName(), String.valueOf(locationBasedReminder.getTriggerExiting()));
        return values;
    }


    private ContentValues getValuesFromPlace(Place place) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_ALIAS.getName(), place.getAlias());
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_ADDRESS.getName(), place.getAddress());
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_LATITUDE.getName(), place.getLatitude());
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_LONGITUDE.getName(), place.getLongitude());
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_RADIUS.getName(), place.getRadius());
        values.put(ChecklizContract.PlaceTable.COLUMN_NAME_IS_ONE_OFF.getName(), place.isOneOff());
        return values;
    }


    private ContentValues getValuesFromAttachment(Attachment attachment) {
        ContentValues values = new ContentValues();
        values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_TASK_FK.getName(), attachment.getTaskId());
        values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_TYPE.getName(), attachment.getType().name());

        switch (attachment.getType()) {
            case AUDIO:
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName(), ((AudioAttachment) attachment).getAudioFilename());
                break;
            case IMAGE:
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_BLOB.getName(), ((ImageAttachment) attachment).getThumbnail());
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName(), ((ImageAttachment) attachment).getImageFilename());
                break;
            case TEXT:
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName(), ((TextAttachment) attachment).getText());
                break;
            case LIST:
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName(), ((ListAttachment) attachment).getItemsJson());
                break;
            case LINK:
                values.put(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName(), ((LinkAttachment) attachment).getLink());
                break;
            default:
                throw new InvalidParameterException("AttachmentType is invalid. Value = " + attachment.getType());
        }
        return values;
    }
















    /* Cursor to Model */

    private Task getTaskFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.TaskTable._ID));
        TaskStatus status = TaskStatus.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_STATUS.getName())));
        String title = cursor.getString(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_TITLE.getName()));
        String description = cursor.getString(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_DESCRIPTION.getName()));
        TaskCategory category = TaskCategory.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_CATEGORY.getName())));


        ReminderType reminderType;
        try {
            reminderType = ReminderType.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_REMINDER_TYPE.getName())));
        } catch (IllegalArgumentException e) { //Thrown if task has no reminder
            reminderType = null;
        }

        Calendar doneDate = null;
        if (status == TaskStatus.DONE) {
            long doneDateLong = cursor.getLong(cursor.getColumnIndex(ChecklizContract.TaskTable.COLUMN_NAME_DONE_DATE.getName()));
            if (doneDateLong != -1) {
                doneDate = Calendar.getInstance();
                doneDate.setTimeInMillis(doneDateLong);
            }
        }

        return new Task(id, status, title, description, category, reminderType, null, doneDate);
    }


    private OneTimeReminder getOneTimeReminderFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.OneTimeReminderTable._ID));
        int taskId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TASK_FK.getName()));

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_DATE.getName())));

        Time time = new Time(cursor.getInt(cursor.getColumnIndex(ChecklizContract.OneTimeReminderTable.COLUMN_NAME_TIME.getName())));
        time.setDisplayTimeFormat(SharedPreferenceUtil.getTimeFormat(mContext));

        return new OneTimeReminder(id, taskId, date, time);
    }


    private RepeatingReminder getRepeatingReminderFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable._ID));
        int taskId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TASK_FK.getName()));

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_DATE.getName())));

        Time time = new Time(cursor.getInt(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_TIME.getName())));
        time.setDisplayTimeFormat(SharedPreferenceUtil.getTimeFormat(mContext));

        ReminderRepeatType repeatType = ReminderRepeatType.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_TYPE.getName())));

        int repeatInterval = cursor.getInt(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_INTERVAL.getName()));
        ReminderRepeatEndType repeatEndType = ReminderRepeatEndType.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_TYPE.getName())));

        int repeatEndNumberOfEvents = -1;
        if(repeatEndType == ReminderRepeatEndType.FOR_X_EVENTS)
            repeatEndNumberOfEvents = cursor.getInt(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_NUMBER_OF_EVENTS.getName()));

        Calendar repeatEndDate = null;
        if(repeatEndType == ReminderRepeatEndType.UNTIL_DATE) {
            repeatEndDate = Calendar.getInstance();
            repeatEndDate.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ChecklizContract.RepeatingReminderTable.COLUMN_NAME_REPEAT_END_DATE.getName())));
        }

        return new RepeatingReminder(id, taskId, date, time, repeatType, repeatInterval, repeatEndType, repeatEndNumberOfEvents, repeatEndDate);
    }


    private LocationBasedReminder getLocationBasedReminderFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable._ID));
        int taskId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TASK_FK.getName()));
        int placeId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_PLACE_FK.getName()));
        boolean triggerEntering = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_ENTERING.getName())));
        boolean triggerExiting = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.LocationBasedReminderTable.COLUMN_NAME_TRIGGER_EXITING.getName())));

        return new LocationBasedReminder(id, taskId, placeId, null, triggerEntering, triggerExiting);
    }


    private Place getPlaceFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.PlaceTable._ID));
        String alias = cursor.getString(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_ALIAS.getName()));
        String address = cursor.getString(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_ADDRESS.getName()));
        double latitude = cursor.getDouble(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_LATITUDE.getName()));
        double longitude = cursor.getDouble(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_LONGITUDE.getName()));
        int radius = cursor.getInt(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_RADIUS.getName()));
        boolean isOneOff = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.PlaceTable.COLUMN_NAME_IS_ONE_OFF.getName())));

        return new Place(id, alias, address, latitude, longitude, radius, isOneOff);
    }

    private Attachment getAttachmentFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ChecklizContract.AttachmentTable._ID));
        int reminderId = cursor.getInt(cursor.getColumnIndex(ChecklizContract.AttachmentTable.COLUMN_NAME_TASK_FK.getName()));
        AttachmentType attachmentType = AttachmentType.valueOf(cursor.getString(cursor.getColumnIndex(ChecklizContract.AttachmentTable.COLUMN_NAME_TYPE.getName())));
        String textContent = cursor.getString(cursor.getColumnIndex(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_TEXT.getName()));
        byte[] blobContent = cursor.getBlob(cursor.getColumnIndex(ChecklizContract.AttachmentTable.COLUMN_NAME_CONTENT_BLOB.getName()));

        switch (attachmentType) {
            case AUDIO:
                return new AudioAttachment(id, reminderId, textContent);
            case IMAGE:
                return new ImageAttachment(id, reminderId, blobContent, textContent);
            case TEXT:
                return new TextAttachment(id, reminderId, textContent);
            case LINK:
                return new LinkAttachment(id, reminderId, textContent);
            case LIST:
                return new ListAttachment(id, reminderId, textContent);
            default:
                throw new InvalidParameterException("AttachmentType is invalid. Value = " + attachmentType);
        }
    }

}