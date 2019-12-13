package ve.com.abicelis.Checkliz.model.attachment;

import java.io.Serializable;

import ve.com.abicelis.Checkliz.enums.AttachmentType;

public abstract class Attachment implements Serializable {
    private int id;
    private int taskId;

    public Attachment() {}   //id-less Constructor used when creating Attachments
    public Attachment(int id, int taskId) {
        this.id = id;
        this.taskId = taskId;
    }

    public abstract AttachmentType getType();

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

}
