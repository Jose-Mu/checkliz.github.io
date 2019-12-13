package ve.com.abicelis.Checkliz.enums;

import java.io.Serializable;

public enum TaskViewModelType implements Serializable {
    HEADER,
    UNPROGRAMMED_REMINDER,
    ONE_TIME_REMINDER,
    REPEATING_REMINDER,
    LOCATION_BASED_REMINDER
}
